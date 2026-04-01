package com.wesleytaumaturgo.cqrs.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.UUID;

/**
 * Configura o Axon Framework para usar Jackson como serializador de eventos.
 * Necessário porque os eventos de domínio são Records (Java 21),
 * que XStream (padrão do Axon) não consegue desserializar corretamente.
 *
 * Inclui módulo Jackson para AccountId (value object sem construtor padrão).
 */
@Configuration
public class AxonConfig {

    @Bean
    @Primary
    public Serializer axonSerializer(ObjectMapper objectMapper) {
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
                return AccountId.of(UUID.fromString(node.get("id").asText()));
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
