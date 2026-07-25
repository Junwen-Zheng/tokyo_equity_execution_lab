package com.junwenzheng.execution.routing;

public record VenueConfiguration(
        String venue,
        double feeBps,
        double maxParticipation,
        double adverseSelectionPenaltyBps
) {
    public VenueConfiguration {
        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException(
                    "venue is required"
            );
        }

        if (!Double.isFinite(feeBps)) {
            throw new IllegalArgumentException(
                    "feeBps must be finite"
            );
        }

        if (
                !Double.isFinite(maxParticipation)
                        || maxParticipation <= 0.0
                        || maxParticipation > 1.0
        ) {
            throw new IllegalArgumentException(
                    "maxParticipation must be in (0, 1]"
            );
        }

        if (
                !Double.isFinite(
                        adverseSelectionPenaltyBps
                )
                        || adverseSelectionPenaltyBps < 0.0
        ) {
            throw new IllegalArgumentException(
                    "adverse-selection penalty must be "
                            + "finite and non-negative"
            );
        }

        venue = venue.trim();
    }

    public double totalRoutingCostBps() {
        return feeBps
                + adverseSelectionPenaltyBps;
    }
}
