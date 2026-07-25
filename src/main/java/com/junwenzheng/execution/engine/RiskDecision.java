package com.junwenzheng.execution.engine;

public record RiskDecision(
        String childOrderId,
        boolean allowed,
        RiskDecisionReason reason,
        int evaluatedQuantity,
        double referencePrice,
        double childNotional,
        int currentPosition,
        long projectedPosition
) {
    public RiskDecision {
        if (
                childOrderId == null
                        || childOrderId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "childOrderId is required"
            );
        }

        if (reason == null) {
            throw new IllegalArgumentException(
                    "reason is required"
            );
        }

        if (evaluatedQuantity < 0) {
            throw new IllegalArgumentException(
                    "evaluatedQuantity must be non-negative"
            );
        }

        if (
                allowed
                        && reason
                        != RiskDecisionReason.ALLOWED
        ) {
            throw new IllegalArgumentException(
                    "allowed decision must use ALLOWED reason"
            );
        }

        if (
                !allowed
                        && reason
                        == RiskDecisionReason.ALLOWED
        ) {
            throw new IllegalArgumentException(
                    "rejected decision cannot use ALLOWED reason"
            );
        }

        childOrderId = childOrderId.trim();
    }
}
