package com.junwenzheng.execution.engine;

public record LatencyEvent(
        String childOrderId,
        LatencyStage stage,
        long timestampMs
) {
    public LatencyEvent {
        if (
                childOrderId == null
                        || childOrderId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "childOrderId is required"
            );
        }

        if (stage == null) {
            throw new IllegalArgumentException(
                    "stage is required"
            );
        }

        if (timestampMs < 0L) {
            throw new IllegalArgumentException(
                    "timestampMs must be non-negative"
            );
        }

        childOrderId = childOrderId.trim();
    }
}
