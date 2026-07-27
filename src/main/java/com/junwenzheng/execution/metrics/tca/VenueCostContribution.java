package com.junwenzheng.execution.metrics.tca;

public record VenueCostContribution(
        String venue,
        int filledQuantity,
        double executedNotional,
        double implementationShortfall,
        double implementationShortfallBps
) {
    public VenueCostContribution {
        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException(
                    "venue is required"
            );
        }

        if (filledQuantity <= 0) {
            throw new IllegalArgumentException(
                    "filledQuantity must be positive"
            );
        }

        if (
                !Double.isFinite(executedNotional)
                        || executedNotional <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "executedNotional must be "
                            + "finite and positive"
            );
        }

        if (
                !Double.isFinite(
                        implementationShortfall
                )
        ) {
            throw new IllegalArgumentException(
                    "implementationShortfall "
                            + "must be finite"
            );
        }

        if (
                !Double.isFinite(
                        implementationShortfallBps
                )
        ) {
            throw new IllegalArgumentException(
                    "implementationShortfallBps "
                            + "must be finite"
            );
        }

        venue = venue.trim();
    }
}
