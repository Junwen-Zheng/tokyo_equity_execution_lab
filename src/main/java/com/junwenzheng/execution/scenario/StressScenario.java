package com.junwenzheng.execution.scenario;

import com.junwenzheng.execution.engine.LatencyProfile;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record StressScenario(
        String name,
        double spreadMultiplier,
        double liquidityMultiplier,
        double volatilityMultiplier,
        double queueDepthMultiplier,
        double gapFraction,
        double gapBps,
        Set<String> unavailableVenues,
        long additionalLatencyMs
) {
    public StressScenario {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name is required"
            );
        }

        validatePositive(
                spreadMultiplier,
                "spreadMultiplier"
        );

        validateUnitInterval(
                liquidityMultiplier,
                "liquidityMultiplier"
        );

        validateNonNegative(
                volatilityMultiplier,
                "volatilityMultiplier"
        );

        validateUnitInterval(
                queueDepthMultiplier,
                "queueDepthMultiplier"
        );

        validateUnitInterval(
                gapFraction,
                "gapFraction"
        );

        if (
                !Double.isFinite(gapBps)
                        || gapBps <= -10_000.0
        ) {
            throw new IllegalArgumentException(
                    "gapBps must be finite and above -10000"
            );
        }

        if (unavailableVenues == null) {
            throw new IllegalArgumentException(
                    "unavailableVenues are required"
            );
        }

        TreeSet<String> normalizedVenues =
                new TreeSet<>();

        for (String venue : unavailableVenues) {
            if (venue == null || venue.isBlank()) {
                throw new IllegalArgumentException(
                        "unavailable venue is required"
                );
            }

            normalizedVenues.add(
                    venue.trim()
            );
        }

        if (additionalLatencyMs < 0L) {
            throw new IllegalArgumentException(
                    "additionalLatencyMs must be non-negative"
            );
        }

        name = name.trim();
        unavailableVenues =
                Set.copyOf(normalizedVenues);
    }

    public static StressScenario baseline() {
        return new StressScenario(
                "BASELINE",
                1.0,
                1.0,
                1.0,
                1.0,
                0.5,
                0.0,
                Set.of(),
                0L
        );
    }

    public static StressScenario wideSpread(
            double multiplier
    ) {
        return new StressScenario(
                "WIDE_SPREAD",
                multiplier,
                1.0,
                1.0,
                1.0,
                0.5,
                0.0,
                Set.of(),
                0L
        );
    }

    public static StressScenario thinLiquidity(
            double multiplier
    ) {
        return new StressScenario(
                "THIN_LIQUIDITY",
                1.0,
                multiplier,
                1.0,
                1.0,
                0.5,
                0.0,
                Set.of(),
                0L
        );
    }

    public static StressScenario highVolatility(
            double multiplier
    ) {
        return new StressScenario(
                "HIGH_VOLATILITY",
                1.0,
                1.0,
                multiplier,
                1.0,
                0.5,
                0.0,
                Set.of(),
                0L
        );
    }

    public static StressScenario priceGap(
            double gapFraction,
            double gapBps
    ) {
        return new StressScenario(
                "PRICE_GAP",
                1.0,
                1.0,
                1.0,
                1.0,
                gapFraction,
                gapBps,
                Set.of(),
                0L
        );
    }

    public static StressScenario queueDepletion(
            double multiplier
    ) {
        return new StressScenario(
                "QUEUE_DEPLETION",
                1.0,
                1.0,
                1.0,
                multiplier,
                0.5,
                0.0,
                Set.of(),
                0L
        );
    }

    public static StressScenario adverseLatency(
            long additionalLatencyMs
    ) {
        return new StressScenario(
                "ADVERSE_LATENCY",
                1.0,
                1.0,
                1.0,
                1.0,
                0.5,
                0.0,
                Set.of(),
                additionalLatencyMs
        );
    }

    public static StressScenario venueOutage(
            String venue
    ) {
        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException(
                    "venue is required"
            );
        }

        return new StressScenario(
                "VENUE_OUTAGE_" + venue.trim(),
                1.0,
                1.0,
                1.0,
                1.0,
                0.5,
                0.0,
                Set.of(venue.trim()),
                0L
        );
    }

    public static StressScenario combinedSevere() {
        return new StressScenario(
                "COMBINED_SEVERE",
                3.0,
                0.25,
                2.5,
                0.02,
                0.50,
                150.0,
                Set.of(),
                25L
        );
    }

    public static List<StressScenario> standardSuite() {
        return List.of(
                baseline(),
                wideSpread(3.0),
                thinLiquidity(0.30),
                highVolatility(2.5),
                priceGap(0.50, 150.0),
                queueDepletion(0.05),
                adverseLatency(25L),
                combinedSevere()
        );
    }

    public LatencyProfile applyTo(
            LatencyProfile baseline
    ) {
        if (baseline == null) {
            throw new IllegalArgumentException(
                    "baseline latency is required"
            );
        }

        try {
            return new LatencyProfile(
                    Math.addExact(
                            baseline.decisionLatencyMs(),
                            additionalLatencyMs
                    ),
                    Math.addExact(
                            baseline.riskLatencyMs(),
                            additionalLatencyMs
                    ),
                    Math.addExact(
                            baseline.acknowledgementLatencyMs(),
                            additionalLatencyMs
                    ),
                    Math.addExact(
                            baseline.fillLatencyMs(),
                            additionalLatencyMs
                    ),
                    Math.addExact(
                            baseline.cancellationLatencyMs(),
                            additionalLatencyMs
                    )
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "stressed latency overflow",
                    exception
            );
        }
    }

    private static void validatePositive(
            double value,
            String name
    ) {
        if (
                !Double.isFinite(value)
                        || value <= 0.0
        ) {
            throw new IllegalArgumentException(
                    name + " must be finite and positive"
            );
        }
    }

    private static void validateNonNegative(
            double value,
            String name
    ) {
        if (
                !Double.isFinite(value)
                        || value < 0.0
        ) {
            throw new IllegalArgumentException(
                    name
                            + " must be finite "
                            + "and non-negative"
            );
        }
    }

    private static void validateUnitInterval(
            double value,
            String name
    ) {
        if (
                !Double.isFinite(value)
                        || value < 0.0
                        || value > 1.0
        ) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 1]"
            );
        }
    }
}
