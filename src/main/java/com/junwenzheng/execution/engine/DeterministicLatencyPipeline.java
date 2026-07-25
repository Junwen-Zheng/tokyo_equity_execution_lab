package com.junwenzheng.execution.engine;

public final class DeterministicLatencyPipeline {
    private final LatencyProfile profile;

    public DeterministicLatencyPipeline(
            LatencyProfile profile
    ) {
        if (profile == null) {
            throw new IllegalArgumentException(
                    "profile is required"
            );
        }

        this.profile = profile;
    }

    public long decisionTimestampMs(
            long eventTimestampMs
    ) {
        return advance(
                eventTimestampMs,
                profile.decisionLatencyMs(),
                "decision"
        );
    }

    public long riskTimestampMs(
            long decisionTimestampMs
    ) {
        return advance(
                decisionTimestampMs,
                profile.riskLatencyMs(),
                "risk"
        );
    }

    public long acknowledgementTimestampMs(
            long riskTimestampMs
    ) {
        return advance(
                riskTimestampMs,
                profile.acknowledgementLatencyMs(),
                "acknowledgement"
        );
    }

    public long fillTimestampMs(
            long acknowledgementTimestampMs
    ) {
        return advance(
                acknowledgementTimestampMs,
                profile.fillLatencyMs(),
                "fill"
        );
    }

    public long cancellationTimestampMs(
            long previousTimestampMs
    ) {
        return advance(
                previousTimestampMs,
                profile.cancellationLatencyMs(),
                "cancellation"
        );
    }

    private static long advance(
            long timestampMs,
            long latencyMs,
            String stage
    ) {
        if (timestampMs < 0L) {
            throw new IllegalArgumentException(
                    "base timestamp must be non-negative"
            );
        }

        try {
            return Math.addExact(
                    timestampMs,
                    latencyMs
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    stage + " timestamp overflow",
                    exception
            );
        }
    }
}
