package com.junwenzheng.execution.routing;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.Side;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SmartOrderRouter {
    private final Map<String, VenueConfiguration>
            configurations;

    public SmartOrderRouter(
            List<VenueConfiguration> configurations
    ) {
        if (configurations == null) {
            throw new IllegalArgumentException(
                    "configurations are required"
            );
        }

        Map<String, VenueConfiguration> byVenue =
                new HashMap<>();

        for (
                VenueConfiguration configuration :
                configurations
        ) {
            if (configuration == null) {
                throw new IllegalArgumentException(
                        "configuration is required"
                );
            }

            VenueConfiguration previous =
                    byVenue.put(
                            configuration.venue(),
                            configuration
                    );

            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate venue configuration: "
                                + configuration.venue()
                );
            }
        }

        this.configurations =
                Map.copyOf(byVenue);
    }

    public static SmartOrderRouter unconstrained() {
        return new SmartOrderRouter(List.of());
    }

    public RoutingDecision route(
            Side side,
            int requestedQuantity,
            List<MarketEvent> venueEvents
    ) {
        if (side == null) {
            throw new IllegalArgumentException(
                    "side is required"
            );
        }

        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException(
                    "requestedQuantity must be positive"
            );
        }

        validateSnapshot(venueEvents);

        List<RouteCandidate> candidates =
                venueEvents.stream()
                        .map(
                                event -> candidate(
                                        side,
                                        event
                                )
                        )
                        .filter(
                                candidate ->
                                        candidate
                                                .availableQuantity()
                                                > 0L
                        )
                        .sorted(
                                candidateComparator(side)
                        )
                        .toList();

        List<RouteAllocation> allocations =
                new ArrayList<>();

        int remaining = requestedQuantity;

        for (RouteCandidate candidate : candidates) {
            if (remaining == 0) {
                break;
            }

            int routedQuantity =
                    Math.toIntExact(
                            Math.min(
                                    (long) remaining,
                                    candidate
                                            .availableQuantity()
                            )
                    );

            allocations.add(
                    new RouteAllocation(
                            candidate.event(),
                            routedQuantity,
                            candidate
                                    .availableQuantity(),
                            candidate.effectivePrice()
                    )
            );

            remaining -= routedQuantity;
        }

        return new RoutingDecision(
                requestedQuantity,
                allocations
        );
    }

    private RouteCandidate candidate(
            Side side,
            MarketEvent event
    ) {
        VenueConfiguration configuration =
                configurations.getOrDefault(
                        event.venue(),
                        defaultConfiguration(
                                event.venue()
                        )
                );

        long participationCapacity =
                (long) Math.floor(
                        event.volume()
                                * configuration
                                .maxParticipation()
                );

        long availableQuantity =
                Math.min(
                        event.queueDepth(),
                        participationCapacity
                );

        double routingCostMultiplier =
                configuration
                        .totalRoutingCostBps()
                        / 10_000.0;

        double effectivePrice =
                side == Side.BUY
                        ? event.ask()
                        * (
                        1.0
                                + routingCostMultiplier
                )
                        : event.bid()
                        * (
                        1.0
                                - routingCostMultiplier
                );

        if (
                !Double.isFinite(effectivePrice)
                        || effectivePrice <= 0.0
        ) {
            throw new IllegalStateException(
                    "routing configuration produced "
                            + "an invalid effective price"
            );
        }

        return new RouteCandidate(
                event,
                availableQuantity,
                effectivePrice
        );
    }

    private static Comparator<RouteCandidate>
    candidateComparator(
            Side side
    ) {
        Comparator<RouteCandidate> byPrice =
                Comparator.comparingDouble(
                        RouteCandidate::effectivePrice
                );

        if (side == Side.SELL) {
            byPrice = byPrice.reversed();
        }

        return byPrice
                .thenComparing(
                        candidate ->
                                candidate.event()
                                        .venue()
                )
                .thenComparingLong(
                        candidate ->
                                candidate.event()
                                        .sourceSequence()
                );
    }

    private static void validateSnapshot(
            List<MarketEvent> venueEvents
    ) {
        if (
                venueEvents == null
                        || venueEvents.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "venueEvents are required"
            );
        }

        MarketEvent first =
                venueEvents.getFirst();

        Set<String> venues =
                new HashSet<>();

        for (MarketEvent event : venueEvents) {
            if (event == null) {
                throw new IllegalArgumentException(
                        "market event is required"
                );
            }

            if (
                    event.timestampMs()
                            != first.timestampMs()
            ) {
                throw new IllegalArgumentException(
                        "routing snapshot timestamps "
                                + "must match"
                );
            }

            if (
                    !event.symbol()
                            .equals(first.symbol())
            ) {
                throw new IllegalArgumentException(
                        "routing snapshot symbols "
                                + "must match"
                );
            }

            if (!venues.add(event.venue())) {
                throw new IllegalArgumentException(
                        "duplicate venue in routing snapshot: "
                                + event.venue()
                );
            }
        }
    }

    private static VenueConfiguration
    defaultConfiguration(
            String venue
    ) {
        return new VenueConfiguration(
                venue,
                0.0,
                1.0,
                0.0
        );
    }

    private record RouteCandidate(
            MarketEvent event,
            long availableQuantity,
            double effectivePrice
    ) {
    }
}
