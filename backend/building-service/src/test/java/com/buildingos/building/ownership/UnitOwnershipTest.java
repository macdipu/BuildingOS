package com.buildingos.building.ownership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.ownership.domain.model.OwnershipChange;
import com.buildingos.building.ownership.domain.model.OwnershipPeriod;
import com.buildingos.building.ownership.domain.model.OwnershipRuleException;
import com.buildingos.building.ownership.domain.model.Share;
import com.buildingos.building.ownership.domain.model.UnitOwnership;
import com.buildingos.building.shared.domain.model.StaleVersionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** UO-06/07 allocation and transfer rules, including the REQUIREMENTS acceptance example. */
class UnitOwnershipTest {
    private static final UUID BUILDING = UUID.randomUUID();
    private static final UUID UNIT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final Instant NOW = Instant.parse("2026-09-24T06:00:00Z");

    private static Share share(String value) { return Share.of(new BigDecimal(value)); }

    private static UnitOwnership after(UnitOwnership before, OwnershipChange change) {
        var closed = change.closed().stream().map(OwnershipPeriod::id).collect(Collectors.toSet());
        List<OwnershipPeriod> open = new ArrayList<>(before.open().stream().filter(p -> !closed.contains(p.id())).toList());
        open.addAll(change.opened());
        return new UnitOwnership(BUILDING, UNIT, change.revision(), open);
    }

    private static Map<UUID, String> shares(UnitOwnership ownership) {
        return ownership.open().stream().collect(Collectors.toMap(OwnershipPeriod::ownerUserId,
                p -> p.share().percent().stripTrailingZeros().toPlainString()));
    }

    private static UnitOwnership sixtyForty() {
        var empty = new UnitOwnership(BUILDING, UNIT, 0, List.of());
        var one = after(empty, empty.assign(A, share("60"), null, ACTOR, 0, NOW));
        return after(one, one.assign(B, share("40"), null, ACTOR, 1, NOW));
    }

    @Test
    void partialTransferMovesOnlyTheShareAndKeepsHistoryOrdered() {
        var before = sixtyForty();
        var change = before.transfer(A, C, share("20"), TODAY, ACTOR, "Sale", "Deed 12", 2, NOW);
        assertThat(change.revision()).isEqualTo(3);
        assertThat(change.closed()).singleElement().satisfies(p -> {
            assertThat(p.ownerUserId()).isEqualTo(A);
            assertThat(p.share().percent()).isEqualByComparingTo("60");
            assertThat(p.endRevision()).isEqualTo(3L);
        });
        var now = after(before, change);
        assertThat(shares(now)).containsExactlyInAnyOrderEntriesOf(Map.of(A, "40", B, "40", C, "20"));
        assertThat(change.transfer().share().percent()).isEqualByComparingTo("20");

        assertThatThrownBy(() -> now.transfer(A, C, share("50"), TODAY, ACTOR, "x", null, 3, NOW))
                .hasFieldOrPropertyWithValue("code", "INSUFFICIENT_SOURCE_SHARE");
        var merged = after(now, now.transfer(A, C, share("40"), TODAY, ACTOR, "Rest", null, 3, NOW));
        assertThat(shares(merged)).containsExactlyInAnyOrderEntriesOf(Map.of(B, "40", C, "60"));
        assertThat(merged.revision()).isEqualTo(4);
    }

    @Test
    void allocationsNeverExceedOneHundredAndStayExact() {
        var full = sixtyForty();
        assertThatThrownBy(() -> full.assign(C, share("0.0001"), null, ACTOR, 2, NOW))
                .hasFieldOrPropertyWithValue("code", "SHARE_EXCEEDED");
        assertThatThrownBy(() -> full.assign(A, share("1"), null, ACTOR, 2, NOW))
                .hasFieldOrPropertyWithValue("code", "ALREADY_OWNER");
        assertThatThrownBy(() -> full.assign(C, share("1"), null, ACTOR, 1, NOW))
                .isInstanceOf(StaleVersionException.class);
        var partial = new UnitOwnership(BUILDING, UNIT, 0, List.of());
        assertThat(after(partial, partial.assign(A, share("33.3333"), null, ACTOR, 0, NOW)).allocated())
                .isEqualByComparingTo("33.3333");
    }

    @Test
    void rejectsInvalidSharesAndSelfTransfers() {
        assertThatThrownBy(() -> share("0")).hasFieldOrPropertyWithValue("code", "SHARE_NOT_POSITIVE");
        assertThatThrownBy(() -> share("100.0001")).hasFieldOrPropertyWithValue("code", "SHARE_TOO_LARGE");
        assertThatThrownBy(() -> share("1.00001")).hasFieldOrPropertyWithValue("code", "SHARE_PRECISION");
        assertThat(share("12.50000").percent()).hasToString("12.5000");
        assertThatThrownBy(() -> Share.of(null)).isInstanceOf(OwnershipRuleException.class);
        var owned = sixtyForty();
        assertThatThrownBy(() -> owned.transfer(A, A, share("1"), TODAY, ACTOR, "x", null, 2, NOW))
                .hasFieldOrPropertyWithValue("code", "SAME_OWNER");
        assertThatThrownBy(() -> owned.transfer(C, A, share("1"), TODAY, ACTOR, "x", null, 2, NOW))
                .hasFieldOrPropertyWithValue("code", "SOURCE_NOT_OWNER");
    }
}
