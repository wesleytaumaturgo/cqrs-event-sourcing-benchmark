# ADR-003: Jackson como Serializador Axon vs XStream (padrão)

**Status:** ACCEPTED
**Data:** 2026-03-31
**Decisores:** Wesley Taumaturgo
**Contexto do projeto:** `cqrs-event-sourcing-benchmark` — implementação Axon Framework 4.9
**Descoberto em:** TASK-007 (Axon Projection + Service + Controller)

---

## Contexto

O Axon Framework 4.x usa **XStream** como serializador padrão para eventos de domínio e snapshots. O XStream serializa objetos Java para XML e armazena o resultado no campo `payload` da tabela `domain_event_entry`.

O projeto usa **Java records** (Java 16+) para os eventos de domínio:
```java
public record AccountOpenedEvent(AccountId accountId, String ownerId, BigDecimal initialBalance, Instant occurredAt) {}
public record MoneyDepositedEvent(AccountId accountId, BigDecimal amount, Instant occurredAt) {}
public record MoneyWithdrawnEvent(AccountId accountId, BigDecimal amount, Instant occurredAt) {}
```

---

## Problema identificado

Durante a implementação de TASK-007, os testes de integração falharam com o erro:

```
com.thoughtworks.xstream.converters.reflection.ReflectionConverter$CannotAccessFieldException:
Can't get field offset on a record class
```

**Causa raiz:** XStream usa `sun.misc.Unsafe.objectFieldOffset()` para acessar campos de objetos via reflection. A partir do Java 16, **Java Records proíbem esse acesso** por serem imutáveis por design — os campos de records não podem ser modificados por reflection, e o JVM lança `IllegalAccessException` ao tentar obtê-los via Unsafe.

---

## Decisão

**Substituir XStream por Jackson como serializador do Axon Framework.**

`AxonConfig.java`:
```java
@Configuration
public class AxonConfig {

    @Bean @Primary
    public Serializer axonSerializer(ObjectMapper objectMapper) {
        objectMapper.registerModule(accountIdModule());
        return JacksonSerializer.builder().objectMapper(objectMapper).build();
    }

    private SimpleModule accountIdModule() {
        var module = new SimpleModule();
        module.addSerializer(AccountId.class, new JsonSerializer<>() {
            @Override
            public void serialize(AccountId value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeStartObject();
                gen.writeStringField("id", value.getValue().toString());
                gen.writeEndObject();
            }
        });
        module.addDeserializer(AccountId.class, new JsonDeserializer<>() {
            @Override
            public AccountId deserialize(JsonParser p, DeserializationContext ctx)
                    throws IOException {
                ObjectNode node = p.getCodec().readTree(p);
                return AccountId.of(UUID.fromString(node.get("id").asText()));
            }
        });
        return module;
    }
}
```

---

## Motivos

### Compatibilidade com Java Records
Jackson 2.12+ suporta Java Records nativamente via módulo de records. Diferentemente do XStream, Jackson não usa `Unsafe.objectFieldOffset()` — serializa records via seus métodos acessores (`accountId()`, `ownerId()`, etc.) que são públicos por design.

### Melhor legibilidade do payload persistido
Com XStream, o payload em `domain_event_entry` é armazenado como XML verbose:
```xml
<com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent>
  <accountId class="com.wesleytaumaturgo.cqrs.domain.account.AccountId">
    <id>550e8400-e29b-41d4-a716-446655440000</id>
  </accountId>
  ...
</com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent>
```

Com Jackson, o payload é JSON compacto:
```json
{"accountId":{"id":"550e8400-e29b-41d4-a716-446655440000"},"ownerId":"owner-1","initialBalance":100.00}
```

### Consistência com o restante da aplicação
A aplicação já usa Spring Boot + Jackson para serialização REST. Reutilizar o mesmo `ObjectMapper` do Spring garante consistência na serialização de tipos customizados como `AccountId`, `BigDecimal`, e `Instant`.

### Suporte a longo prazo
XStream tem histórico de vulnerabilidades de desserialização (CVE-2021-29505, CVE-2022-40151) e não é mais mantido ativamente para novos runtimes Java. Jackson é ativamente mantido e é o padrão de fato para JSON no ecossistema Spring.

---

## Consequências

### Positivas
- Compatibilidade com Java 21 Records
- Payload legível em JSON (facilita debugging e inspeção manual)
- Sem warnings de `sun.misc.Unsafe` no runtime
- Reutilização do `ObjectMapper` Spring

### Negativas
- Necessidade de registrar `JsonSerializer`/`JsonDeserializer` para tipos value objects customizados (`AccountId`)
- Migração de dados não é possível se houver eventos XStream já persistidos (não aplicável — projeto new, sem dados legados)
- Payload JSON ocupa ligeiramente mais espaço que XML para payloads muito simples (irrelevante para este benchmark)

### Configuração adicional necessária
`AccountId` tem construtor privado sem anotações Jackson. Foi necessário registrar um módulo Jackson customizado (`accountIdModule()`) para serializar/deserializar o value object corretamente.

---

## Alternativas descartadas

| Alternativa | Motivo do descarte |
|-------------|-------------------|
| XStream (padrão Axon) | Incompatível com Java Records no Java 21 |
| Java serialization nativa | Performance ruim; formato binário não legível; descontinuado no Axon 4.x |
| Protobuf | Overhead de definição de schemas `.proto`; fora do escopo do benchmark |

---

## Referências

- [Axon Framework Docs — Serializer Configuration](https://docs.axoniq.io/reference-guide/axon-framework/serialization)
- [XStream CVE-2021-29505](https://nvd.nist.gov/vuln/detail/CVE-2021-29505)
- [JEP 395: Records](https://openjdk.org/jeps/395)
