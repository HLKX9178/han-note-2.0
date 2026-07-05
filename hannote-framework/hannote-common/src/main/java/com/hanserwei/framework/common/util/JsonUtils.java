package com.hanserwei.framework.common.util;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonUtils {

    private static JsonMapper jsonMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .build();

    private JsonUtils() {
    }

    /** 允许用 Spring 管理的 JsonMapper 覆盖默认实例（见 auth 的 JacksonConfig）。 */
    public static void init(JsonMapper mapper) {
        jsonMapper = mapper;
    }

    public static String toJsonString(Object obj) {
        return jsonMapper.writeValueAsString(obj);
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return jsonMapper.readValue(json, clazz);
    }
}
