package com.hanserwei.framework.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Sample {
        private String name;
        private LocalDateTime time;
    }

    @Test
    void toJsonString_serializesLocalDateTimeWithoutExtraModule() {
        String json = JsonUtils.toJsonString(new Sample("n", LocalDateTime.of(2026, 7, 5, 12, 0, 0)));
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"n\""));
        assertTrue(json.contains("2026"));
    }

    @Test
    void parseObject_roundTrips() {
        Sample s = JsonUtils.parseObject("{\"name\":\"x\"}", Sample.class);
        assertNotNull(s);
        assertTrue("x".equals(s.getName()));
    }
}
