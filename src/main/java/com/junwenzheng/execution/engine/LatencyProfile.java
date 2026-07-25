package com.junwenzheng.execution.engine;

public record LatencyProfile(
        long decisionLatencyMs,
        long riskLatencyMs,
        long acknowledgementLatencyMs,
        long fillLatencyMs,
        long cancellationLatencyMs
) {
    public LatencyProfile {
        validate(
                decisionLatencyMs,
                "decisionLatencyMs"
        );

        validate(
                riskLatencyMs,
                "riskLatencyMs"
        );

        validate(
                acknowledgementLatencyMs,
                "acknowledgementLatencyMs"
        );

        validate(
                fillLatencyMs,
                "fillLatencyMs"
        );

        validate(
                cancellationLatencyMs,
                "cancellationLatencyMs"
        );
    }

    public static LatencyProfile deterministicBaseline() {
        return new LatencyProfile(
                1L,
                1L,
                1L,
                2L,
                1L
        );
    }

    public static LatencyProfile zero() {
        return new LatencyProfile(
                0L,
                0L,
                0L,
                0L,
                0L
        );
    }

    private static void validate(
            long latencyMs,
            String name
    ) {
        if (latencyMs < 0L) {
            throw new IllegalArgumentException(
                    name + " must be non-negative"
            );
        }
    }
}
