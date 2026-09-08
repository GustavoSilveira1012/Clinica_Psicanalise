package com.psicogest.psicogest.security.audit;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditObjectMapperConfig {

    @Bean
    @Qualifier("auditObjectMapper")
    public ObjectMapper auditObjectMapper() {

        ObjectMapper mapper =
                new ObjectMapper();

        mapper.registerModule(
                new JavaTimeModule()
        );

        mapper.configure(
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true
        );

        mapper.configure(
                MapperFeature.SORT_PROPERTIES_ALPHABETICALLY,
                true
        );

        return mapper;
    }
}
