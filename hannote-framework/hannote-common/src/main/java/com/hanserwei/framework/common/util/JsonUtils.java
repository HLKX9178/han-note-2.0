package com.hanserwei.framework.common.util;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

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

    /**
     * 将 JSON 字符串解析为 {@code Map<K, V>}。
     *
     * <p>基于 Jackson 3 的 {@code TypeFactory.constructMapType} 构造带泛型的 Map 类型，
     * 用于解析计数聚合消息体（如 {@code {"27":3200}} → {@code Map<Long,Integer>}）。
     *
     * @param json       JSON 字符串
     * @param keyClass   Map key 类型
     * @param valueClass Map value 类型
     * @return 解析后的 Map；json 为 null 或空时返回 {@code null}
     */
    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return jsonMapper.readValue(json,
                jsonMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
    }

    /**
     * 将 JSON 字符串解析为 {@code List<T>}。
     *
     * <p>基于 Jackson 3 的 {@code TypeFactory.constructCollectionType} 构造带泛型的 List 类型，
     * 用于解析计数聚合消息体（如聚合后的 {@code List<AggregationCountNoteMqDTO>}）。
     *
     * @param json  JSON 字符串
     * @param clazz List 元素类型
     * @return 解析后的 List；json 为 null 或空时返回 {@code null}
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return jsonMapper.readValue(json,
                jsonMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}
