package com.buildingos.building.unit.presentation.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.unit.domain.model.UnitSort;
import org.junit.jupiter.api.Test;

class UnitSortParamTest {
    @Test
    void parsesFieldAndDirectionWithAscendingDefault() {
        assertThat(UnitSortParam.parse(null)).isEqualTo(UnitSort.DEFAULT);
        assertThat(UnitSortParam.parse("floor")).isEqualTo(new UnitSort(UnitSort.Field.FLOOR, false));
        assertThat(UnitSortParam.parse("type,DESC")).isEqualTo(new UnitSort(UnitSort.Field.TYPE, true));
        assertThat(UnitSortParam.parse("unitNumber,asc")).isEqualTo(UnitSort.DEFAULT);
    }

    @Test
    void rejectsUnknownFieldsDirectionsAndExtraParts() {
        assertThatThrownBy(() -> UnitSortParam.parse("area")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UnitSortParam.parse("floor,up")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UnitSortParam.parse("floor,asc,x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numberQueryTrimsBlankToNullAndCapsLength() {
        assertThat(UnitSortParam.numberQuery("  ")).isNull();
        assertThat(UnitSortParam.numberQuery(" 4a ")).isEqualTo("4a");
        assertThatThrownBy(() -> UnitSortParam.numberQuery("x".repeat(33)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
