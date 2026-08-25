package com.idb.auth.common.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 ({@code tools.jackson}) is the default mapper family in Spring Boot
 * 4. Its mappers are immutable, so configuration happens through the builder
 * rather than post-construction setters, and the date-format toggles moved from
 * {@code SerializationFeature} to {@code DateTimeFeature}.
 */
@Slf4j
public final class JsonUtil {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(Include.NON_NULL))
            .build();

    private JsonUtil() {
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Error converting object to JSON", e);
            return "";
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error parsing JSON to object of type {}", clazz.getName(), e);
            return null;
        }
    }
}
