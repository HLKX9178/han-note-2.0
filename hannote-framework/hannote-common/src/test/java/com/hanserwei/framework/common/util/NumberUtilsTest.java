package com.hanserwei.framework.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberUtilsTest {

    @Test
    void lessThanTenThousand_keepsRawNumber() {
        assertThat(NumberUtils.formatNumberString(0)).isEqualTo("0");
        assertThat(NumberUtils.formatNumberString(1)).isEqualTo("1");
        assertThat(NumberUtils.formatNumberString(9999)).isEqualTo("9999");
    }

    @Test
    void betweenTenThousandAndHundredMillion_formatsAsWan() {
        // 教程示例：137623 → 13.7万（截断，不四舍五入）
        assertThat(NumberUtils.formatNumberString(137623)).isEqualTo("13.7万");
        assertThat(NumberUtils.formatNumberString(10000)).isEqualTo("1.0万");
        assertThat(NumberUtils.formatNumberString(408000)).isEqualTo("40.8万");
        // 截断验证：19999 → 1.9万（非 2.0万）
        assertThat(NumberUtils.formatNumberString(19999)).isEqualTo("1.9万");
    }

    @Test
    void atLeastHundredMillion_cappedAt9999Wan() {
        assertThat(NumberUtils.formatNumberString(100_000_000L)).isEqualTo("9999万");
        assertThat(NumberUtils.formatNumberString(999_999_999L)).isEqualTo("9999万");
    }
}
