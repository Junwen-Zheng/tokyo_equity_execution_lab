package com.junwenzheng.execution.algo;

public record ExecutionDecision(
        int childQuantity,
        String reason
) {
    public ExecutionDecision {
        if (childQuantity < 0) {
            throw new IllegalArgumentException(
                    "childQuantity must be non-negative"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "reason is required"
            );
        }

        reason = reason.trim();
    }

    public static ExecutionDecision none(
            String reason
    ) {
        return new ExecutionDecision(
                0,
                reason
        );
    }

    public boolean shouldTrade() {
        return childQuantity > 0;
    }
}
