package com.wesleytaumaturgo.cqrs.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import java.io.IOException;
import java.util.UUID;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configura o Axon Framework para usar Jackson como serializador de eventos.
 * Necessário porque os eventos de domínio são Records (Java 21),
 * que XStream (padrão do Axon) não consegue desserializar corretamente.
 *
 * Inclui módulo Jackson para AccountId (value object sem construtor padrão).
 */
@Configuration
public class AxonConfig {

    /**
     * Jackson MixIn para DomainEvent: aplica @JsonTypeInfo/@JsonSubTypes
     * sem importar Jackson no pacote domain (mantém domínio livre de frameworks).
     * Adicionar novo evento = nova linha aqui + nova @Type, zero mudança no EventStore.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes({
      @JsonSubTypes.Type(value = AccountOpenedEvent.class, name = "AccountOpenedEvent"),
      @JsonSubTypes.Type(value = MoneyDepositedEvent.class, name = "MoneyDepositedEvent"),
      @JsonSubTypes.Type(value = MoneyWithdrawnEvent.class, name = "MoneyWithdrawnEvent")
    })
    abstract static class DomainEventMixIn {}

    @Bean
    @Primary
    public Serializer axonSerializer(ObjectMapper objectMapper) {
        objectMapper.addMixIn(DomainEvent.class, DomainEventMixIn.class);
        objectMapper.registerModule(accountIdModule());
        objectMapper.registerModule(moneyModule());
        return JacksonSerializer.builder()
            .objectMapper(objectMapper)
            .build();
    }

    private SimpleModule accountIdModule() {
        SimpleModule module = new SimpleModule("AccountIdModule");

        module.addSerializer(AccountId.class, new JsonSerializer<>() {
            @Override
            public void serialize(AccountId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeStartObject();
                gen.writeStringField("id", value.getValue().toString());
                gen.writeEndObject();
            }
        });

        module.addDeserializer(AccountId.class, new JsonDeserializer<>() {
            @Override
            public AccountId deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                JsonNode node = p.getCodec().readTree(p);
                JsonNode idNode = node.get("id");
                if (idNode == null || idNode.isNull()) {
                    throw com.fasterxml.jackson.databind.exc.MismatchedInputException.from(
                        p, AccountId.class, "Missing 'id' field in AccountId JSON");
                }
                return AccountId.of(UUID.fromString(idNode.asText()));
            }
        });

        return module;
    }

    /**
     * Serializa Money como BigDecimal (número JSON) e desserializa BigDecimal→Money.
     * Mantém o domain VO livre de dependências de infraestrutura (Jackson).
     */
    private SimpleModule moneyModule() {
        SimpleModule module = new SimpleModule("MoneyModule");

        module.addSerializer(Money.class, new JsonSerializer<>() {
            @Override
            public void serialize(Money value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeNumber(value.getValue());
            }
        });

        module.addDeserializer(Money.class, new JsonDeserializer<>() {
            @Override
            public Money deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return Money.of(p.getDecimalValue());
            }
        });

        return module;
    }
}
