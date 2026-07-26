package com.junwenzheng.execution.rules;

import com.junwenzheng.execution.market.MarketEventType;

public final class TokyoSessionSchedule {
    public static final long MORNING_OPEN_MS =
            timeMs(9, 0);

    public static final long MORNING_CLOSE_MS =
            timeMs(11, 30);

    public static final long AFTERNOON_OPEN_MS =
            timeMs(12, 30);

    public static final long PRE_CLOSE_START_MS =
            timeMs(15, 25);

    public static final long AFTERNOON_CLOSE_MS =
            timeMs(15, 30);

    public static final long DAY_END_MS =
            timeMs(24, 0);

    public TokyoSessionPhase phaseAt(
            long millisecondsSinceMidnightJst
    ) {
        validateTimestamp(
                millisecondsSinceMidnightJst
        );

        if (
                millisecondsSinceMidnightJst
                        < MORNING_OPEN_MS
        ) {
            return TokyoSessionPhase.CLOSED;
        }

        if (
                millisecondsSinceMidnightJst
                        == MORNING_OPEN_MS
        ) {
            return TokyoSessionPhase
                    .MORNING_OPENING_AUCTION;
        }

        if (
                millisecondsSinceMidnightJst
                        < MORNING_CLOSE_MS
        ) {
            return TokyoSessionPhase
                    .MORNING_CONTINUOUS;
        }

        if (
                millisecondsSinceMidnightJst
                        == MORNING_CLOSE_MS
        ) {
            return TokyoSessionPhase
                    .MORNING_CLOSING_AUCTION;
        }

        if (
                millisecondsSinceMidnightJst
                        < AFTERNOON_OPEN_MS
        ) {
            return TokyoSessionPhase.LUNCH_BREAK;
        }

        if (
                millisecondsSinceMidnightJst
                        == AFTERNOON_OPEN_MS
        ) {
            return TokyoSessionPhase
                    .AFTERNOON_OPENING_AUCTION;
        }

        if (
                millisecondsSinceMidnightJst
                        < PRE_CLOSE_START_MS
        ) {
            return TokyoSessionPhase
                    .AFTERNOON_CONTINUOUS;
        }

        if (
                millisecondsSinceMidnightJst
                        < AFTERNOON_CLOSE_MS
        ) {
            return TokyoSessionPhase.PRE_CLOSE;
        }

        if (
                millisecondsSinceMidnightJst
                        == AFTERNOON_CLOSE_MS
        ) {
            return TokyoSessionPhase
                    .AFTERNOON_CLOSING_AUCTION;
        }

        return TokyoSessionPhase.CLOSED;
    }

    public boolean allowsExecution(
            long millisecondsSinceMidnightJst,
            MarketEventType eventType
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException(
                    "eventType is required"
            );
        }

        TokyoSessionPhase phase =
                phaseAt(
                        millisecondsSinceMidnightJst
                );

        return switch (eventType) {
            case CONTINUOUS ->
                    phase
                            == TokyoSessionPhase
                            .MORNING_CONTINUOUS
                            || phase
                            == TokyoSessionPhase
                            .AFTERNOON_CONTINUOUS;

            case OPENING_AUCTION ->
                    phase
                            == TokyoSessionPhase
                            .MORNING_OPENING_AUCTION
                            || phase
                            == TokyoSessionPhase
                            .AFTERNOON_OPENING_AUCTION;

            case CLOSING_AUCTION ->
                    phase
                            == TokyoSessionPhase
                            .MORNING_CLOSING_AUCTION
                            || phase
                            == TokyoSessionPhase
                            .AFTERNOON_CLOSING_AUCTION;
        };
    }

    public static long timeMs(
            int hour,
            int minute
    ) {
        if (
                hour < 0
                        || hour > 24
                        || minute < 0
                        || minute > 59
                        || (
                        hour == 24
                                && minute != 0
                )
        ) {
            throw new IllegalArgumentException(
                    "invalid time of day"
            );
        }

        return (
                (
                        (long) hour * 60L
                                + minute
                ) * 60L
        ) * 1_000L;
    }

    private static void validateTimestamp(
            long millisecondsSinceMidnightJst
    ) {
        if (
                millisecondsSinceMidnightJst < 0L
                        || millisecondsSinceMidnightJst
                        >= DAY_END_MS
        ) {
            throw new IllegalArgumentException(
                    "timestamp must be milliseconds "
                            + "since midnight JST"
            );
        }
    }
}
