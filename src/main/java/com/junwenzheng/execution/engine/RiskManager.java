package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.order.ChildOrder;

public final class RiskManager {
    private final int maxChildQty;
    private final double maxChildNotional;
    private final int maxAbsolutePosition;

    public RiskManager(
            int maxChildQty,
            double maxChildNotional
    ) {
        this(
                maxChildQty,
                maxChildNotional,
                Integer.MAX_VALUE
        );
    }

    public RiskManager(
            int maxChildQty,
            double maxChildNotional,
            int maxAbsolutePosition
    ) {
        if (maxChildQty <= 0) {
            throw new IllegalArgumentException(
                    "maxChildQty must be positive"
            );
        }

        if (
                !Double.isFinite(maxChildNotional)
                        || maxChildNotional <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "maxChildNotional must be "
                            + "finite and positive"
            );
        }

        if (maxAbsolutePosition <= 0) {
            throw new IllegalArgumentException(
                    "maxAbsolutePosition must be positive"
            );
        }

        this.maxChildQty = maxChildQty;
        this.maxChildNotional = maxChildNotional;
        this.maxAbsolutePosition =
                maxAbsolutePosition;
    }

    public RiskDecision evaluate(
            ChildOrder order,
            double referencePrice,
            int currentPosition
    ) {
        if (order == null) {
            throw new IllegalArgumentException(
                    "order is required"
            );
        }

        int quantity =
                order.remainingQuantity();

        long projectedPosition =
                (long) currentPosition
                        + (
                        (long) order.side().sign()
                                * quantity
                );

        if (quantity <= 0) {
            return rejected(
                    order,
                    RiskDecisionReason
                            .INVALID_ORDER_QUANTITY,
                    quantity,
                    referencePrice,
                    Double.NaN,
                    currentPosition,
                    projectedPosition
            );
        }

        if (
                !Double.isFinite(referencePrice)
                        || referencePrice <= 0.0
        ) {
            return rejected(
                    order,
                    RiskDecisionReason
                            .INVALID_REFERENCE_PRICE,
                    quantity,
                    referencePrice,
                    Double.NaN,
                    currentPosition,
                    projectedPosition
            );
        }

        double childNotional =
                quantity * referencePrice;

        if (quantity > maxChildQty) {
            return rejected(
                    order,
                    RiskDecisionReason
                            .MAX_CHILD_QUANTITY,
                    quantity,
                    referencePrice,
                    childNotional,
                    currentPosition,
                    projectedPosition
            );
        }

        if (
                !Double.isFinite(childNotional)
                        || childNotional
                        > maxChildNotional
        ) {
            return rejected(
                    order,
                    RiskDecisionReason
                            .MAX_CHILD_NOTIONAL,
                    quantity,
                    referencePrice,
                    childNotional,
                    currentPosition,
                    projectedPosition
            );
        }

        if (
                Math.abs(projectedPosition)
                        > (long) maxAbsolutePosition
        ) {
            return rejected(
                    order,
                    RiskDecisionReason
                            .MAX_ABSOLUTE_POSITION,
                    quantity,
                    referencePrice,
                    childNotional,
                    currentPosition,
                    projectedPosition
            );
        }

        return new RiskDecision(
                order.childOrderId(),
                true,
                RiskDecisionReason.ALLOWED,
                quantity,
                referencePrice,
                childNotional,
                currentPosition,
                projectedPosition
        );
    }

    public boolean isAllowed(
            ChildOrder order,
            double referencePrice
    ) {
        return evaluate(
                order,
                referencePrice,
                0
        ).allowed();
    }

    public boolean isAllowed(
            ChildOrder order,
            double referencePrice,
            int currentPosition
    ) {
        return evaluate(
                order,
                referencePrice,
                currentPosition
        ).allowed();
    }

    private static RiskDecision rejected(
            ChildOrder order,
            RiskDecisionReason reason,
            int quantity,
            double referencePrice,
            double childNotional,
            int currentPosition,
            long projectedPosition
    ) {
        return new RiskDecision(
                order.childOrderId(),
                false,
                reason,
                Math.max(quantity, 0),
                referencePrice,
                childNotional,
                currentPosition,
                projectedPosition
        );
    }
}
