package com.hanserwei.user.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SexEnumTest {

    @Test
    void isValid_acceptsKnownValues() {
        assertThat(SexEnum.isValid(0)).isTrue();
        assertThat(SexEnum.isValid(1)).isTrue();
    }

    @Test
    void isValid_rejectsUnknownOrNull() {
        assertThat(SexEnum.isValid(2)).isFalse();
        assertThat(SexEnum.isValid(null)).isFalse();
    }
}
