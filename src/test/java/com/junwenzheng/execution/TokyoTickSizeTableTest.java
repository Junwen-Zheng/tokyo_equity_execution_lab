package com.junwenzheng.execution;

import com.junwenzheng.execution.rules.TokyoTickSizeTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TokyoTickSizeTableTest {

    @Test
    void topix500TableUsesCorrectBoundaries() {
        TokyoTickSizeTable table =
                TokyoTickSizeTable.TOPIX_500;

        assertEquals(0.1, table.tickSize(1_000.0));
        assertEquals(0.5, table.tickSize(1_000.1));
        assertEquals(1.0, table.tickSize(3_000.1));
        assertEquals(5.0, table.tickSize(10_000.1));
        assertEquals(10.0, table.tickSize(30_000.1));
        assertEquals(50.0, table.tickSize(100_000.1));
        assertEquals(100.0, table.tickSize(300_000.1));
        assertEquals(500.0, table.tickSize(1_000_000.1));
        assertEquals(1_000.0, table.tickSize(3_000_000.1));
        assertEquals(5_000.0, table.tickSize(10_000_000.1));
        assertEquals(10_000.0, table.tickSize(30_000_000.1));
    }

    @Test
    void otherIssueTableUsesCorrectBoundaries() {
        TokyoTickSizeTable table =
                TokyoTickSizeTable.OTHER_ISSUE;

        assertEquals(1.0, table.tickSize(3_000.0));
        assertEquals(5.0, table.tickSize(3_000.1));
        assertEquals(10.0, table.tickSize(5_000.1));
        assertEquals(50.0, table.tickSize(30_000.1));
        assertEquals(100.0, table.tickSize(50_000.1));
        assertEquals(500.0, table.tickSize(300_000.1));
        assertEquals(1_000.0, table.tickSize(500_000.1));
        assertEquals(5_000.0, table.tickSize(3_000_000.1));
        assertEquals(10_000.0, table.tickSize(5_000_000.1));
        assertEquals(50_000.0, table.tickSize(30_000_000.1));
        assertEquals(100_000.0, table.tickSize(50_000_000.1));
    }

    @Test
    void validatesTopix500TickAlignment() {
        TokyoTickSizeTable table =
                TokyoTickSizeTable.TOPIX_500;

        assertTrue(table.isAligned(999.9));
        assertTrue(table.isAligned(1_000.0));
        assertTrue(table.isAligned(1_000.5));
        assertTrue(table.isAligned(3_001.0));

        assertFalse(table.isAligned(1_000.1));
        assertFalse(table.isAligned(3_000.5));
    }

    @Test
    void validatesOtherIssueTickAlignment() {
        TokyoTickSizeTable table =
                TokyoTickSizeTable.OTHER_ISSUE;

        assertTrue(table.isAligned(3_000.0));
        assertTrue(table.isAligned(3_005.0));
        assertTrue(table.isAligned(5_010.0));

        assertFalse(table.isAligned(3_001.0));
        assertFalse(table.isAligned(5_005.0));
    }

    @Test
    void rejectsInvalidPricesAndMisalignment() {
        TokyoTickSizeTable table =
                TokyoTickSizeTable.TOPIX_500;

        assertThrows(
                IllegalArgumentException.class,
                () -> table.tickSize(0.0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.tickSize(Double.NaN)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.validateAligned(1_000.1)
        );
    }
}
