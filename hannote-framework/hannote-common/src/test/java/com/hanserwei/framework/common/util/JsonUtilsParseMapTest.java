package com.hanserwei.framework.common.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonUtilsParseMapTest {

    @Test
    void parseMap_shouldParseLongIntegerMap() {
        String json = "{\"27\":3200,\"31\":-2}";
        Map<Long, Integer> map = JsonUtils.parseMap(json, Long.class, Integer.class);
        assertEquals(2, map.size());
        assertEquals(3200, map.get(27L));
        assertEquals(-2, map.get(31L));
    }

    @Test
    void parseMap_shouldReturnNullOnBlank() {
        assertNull(JsonUtils.parseMap(null, Long.class, Integer.class));
        assertNull(JsonUtils.parseMap("", Long.class, Integer.class));
    }
}
