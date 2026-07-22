package com.junwenzheng.execution.engine;

import com.junwenzheng.execution.order.Fill;

public sealed interface FillOutcome
        permits FillOutcome.Filled,
        FillOutcome.NoFill {

    long participationCap();

    long queueAheadQuantity();

    long executableLiquidity();

    record Filled(
            Fill fill,
            long participationCap,
            long queueAheadQuantity,
            long executableLiquidity
    ) implements FillOutcome {
        public Filled {
            if (fill == null) {
                throw new IllegalArgumentException(
                        "fill is required"
                );
            }

            validateLiquidity(
                    participationCap,
                    queueAheadQuantity,
                    executableLiquidity
            );

            if (
                    fill.quantity()
                            > executableLiquidity
            ) {
                throw new IllegalArgumentException(
                        "fill exceeds executable liquidity"
                );
            }
        }
    }

    record NoFill(
            String reason,
            long participationCap,
            long queueAheadQuantity,
            long executableLiquidity
    ) implements FillOutcome {
        public NoFill {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "reason is required"
                );
            }

            validateLiquidity(
                    participationCap,
                    queueAheadQuantity,
                    executableLiquidity
            );

            if (executableLiquidity != 0L) {
                throw new IllegalArgumentException(
                        "no-fill outcome must have zero "
                                + "executable liquidity"
                );
            }

            reason = reason.trim();
        }
    }

    private static void validateLiquidity(
            long participationCap,
            long queueAheadQuantity,
            long executableLiquidity
    ) {
        if (
                participationCap < 0L
                        || queueAheadQuantity < 0L
                        || executableLiquidity < 0L
        ) {
            throw new IllegalArgumentException(
                    "liquidity quantities must be non-negative"
            );
        }

        if (
                queueAheadQuantity
                        + executableLiquidity
                        != participationCap
        ) {
            throw new IllegalArgumentException(
                    "queue and executable liquidity "
                            + "must equal participation cap"
            );
        }
    }
}
