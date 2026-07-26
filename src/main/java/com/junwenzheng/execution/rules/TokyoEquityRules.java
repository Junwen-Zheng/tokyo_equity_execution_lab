package com.junwenzheng.execution.rules;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ParentOrder;

public final class TokyoEquityRules {
    public static final int STANDARD_BOARD_LOT =
            100;

    private final TokyoSessionSchedule
            sessionSchedule;

    private final TokyoTickSizeTable
            tickSizeTable;

    private final int boardLotSize;

    public TokyoEquityRules(
            TokyoTickSizeTable tickSizeTable
    ) {
        this(
                new TokyoSessionSchedule(),
                tickSizeTable,
                STANDARD_BOARD_LOT
        );
    }

    public TokyoEquityRules(
            TokyoSessionSchedule sessionSchedule,
            TokyoTickSizeTable tickSizeTable,
            int boardLotSize
    ) {
        if (sessionSchedule == null) {
            throw new IllegalArgumentException(
                    "sessionSchedule is required"
            );
        }

        if (tickSizeTable == null) {
            throw new IllegalArgumentException(
                    "tickSizeTable is required"
            );
        }

        if (boardLotSize <= 0) {
            throw new IllegalArgumentException(
                    "boardLotSize must be positive"
            );
        }

        this.sessionSchedule = sessionSchedule;
        this.tickSizeTable = tickSizeTable;
        this.boardLotSize = boardLotSize;
    }

    public static TokyoEquityRules topix500() {
        return new TokyoEquityRules(
                TokyoTickSizeTable.TOPIX_500
        );
    }

    public static TokyoEquityRules otherIssue() {
        return new TokyoEquityRules(
                TokyoTickSizeTable.OTHER_ISSUE
        );
    }

    public TokyoSessionPhase phaseAt(
            long millisecondsSinceMidnightJst
    ) {
        return sessionSchedule.phaseAt(
                millisecondsSinceMidnightJst
        );
    }

    public boolean isBoardLot(
            int quantity
    ) {
        return quantity > 0
                && quantity % boardLotSize == 0;
    }

    public boolean allowsExecution(
            MarketEvent event
    ) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "event is required"
            );
        }

        return sessionSchedule.allowsExecution(
                event.timestampMs(),
                event.type()
        );
    }

    public int normalizeBoardLotQuantity(
            int requestedQuantity,
            int maximumQuantity
    ) {
        if (requestedQuantity < 0) {
            throw new IllegalArgumentException(
                    "requestedQuantity must be non-negative"
            );
        }

        if (maximumQuantity < 0) {
            throw new IllegalArgumentException(
                    "maximumQuantity must be non-negative"
            );
        }

        int cappedQuantity =
                Math.min(
                        requestedQuantity,
                        maximumQuantity
                );

        return (
                cappedQuantity
                        / boardLotSize
        ) * boardLotSize;
    }

    public void validateParentOrder(
            ParentOrder parentOrder
    ) {
        if (parentOrder == null) {
            throw new IllegalArgumentException(
                    "parentOrder is required"
            );
        }

        validateBoardLot(
                parentOrder.quantity()
        );

        tickSizeTable.validateAligned(
                parentOrder.arrivalPrice()
        );
    }

    public void validateChildOrder(
            ChildOrder childOrder
    ) {
        if (childOrder == null) {
            throw new IllegalArgumentException(
                    "childOrder is required"
            );
        }

        validateBoardLot(
                childOrder.quantity()
        );
    }

    public void validateMarketEvent(
            MarketEvent event
    ) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "event is required"
            );
        }

        if (!allowsExecution(event)) {
            throw new IllegalArgumentException(
                    "event type "
                            + event.type()
                            + " is not executable during "
                            + phaseAt(event.timestampMs())
            );
        }

        tickSizeTable.validateAligned(
                event.bid()
        );

        tickSizeTable.validateAligned(
                event.ask()
        );

        tickSizeTable.validateAligned(
                event.last()
        );
    }

    public double tickSize(
            double price
    ) {
        return tickSizeTable.tickSize(price);
    }

    public int boardLotSize() {
        return boardLotSize;
    }

    private void validateBoardLot(
            int quantity
    ) {
        if (!isBoardLot(quantity)) {
            throw new IllegalArgumentException(
                    "quantity must be a multiple of "
                            + boardLotSize
            );
        }
    }
}
