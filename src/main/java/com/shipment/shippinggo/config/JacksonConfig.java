package com.shipment.shippinggo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Global Jackson configuration to prevent serialization issues with JPA entities:
 * 1. Handles Hibernate lazy-loaded proxies gracefully (avoids LazyInitializationException)
 * 2. Prevents infinite recursion from bidirectional/circular entity relationships
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();

        // Handle Hibernate lazy-loaded proxies: serialize them as null instead of
        // forcing initialization (which can cause LazyInitializationException outside a session)
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        mapper.registerModule(hibernate6Module);

        // Don't fail on empty beans (Hibernate proxies can appear as empty)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
