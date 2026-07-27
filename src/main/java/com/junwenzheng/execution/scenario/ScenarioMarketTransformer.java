package com.junwenzheng.execution.scenario;

import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioMarketTransformer {
    private ScenarioMarketTransformer() {}

    public static MarketDataReplay apply(
            MarketDataReplay replay,
            StressScenario scenario
    ) {
        if (replay == null) {
            throw new IllegalArgumentException(
                    "replay is required"
            );
        }

        if (scenario == null) {
            throw new IllegalArgumentException(
                    "scenario is required"
            );
        }

        List<MarketEvent> source =
                replay.events();

        long gapTimestamp =
                gapTimestamp(
                        source,
                        scenario
                );

        Map<MarketKey, PriceState>
                priceStates =
                new HashMap<>();

        List<MarketEvent> transformed =
                new ArrayList<>();

        for (MarketEvent event : source) {
            if (
                    scenario.unavailableVenues()
                            .contains(event.venue())
            ) {
                continue;
            }

            MarketKey key =
                    new MarketKey(
                            event.symbol(),
                            event.venue()
                    );

            double originalMid =
                    event.mid();

            PriceState previous =
                    priceStates.get(key);

            double volatilityMid;

            if (previous == null) {
                volatilityMid =
                        originalMid;
            } else {
                volatilityMid =
                        previous.stressedMid()
                                + (
                                originalMid
                                        - previous.originalMid()
                        )
                                * scenario
                                .volatilityMultiplier();
            }

            priceStates.put(
                    key,
                    new PriceState(
                            originalMid,
                            volatilityMid
                    )
            );

            double gapMultiplier =
                    event.timestampMs()
                            >= gapTimestamp
                            ? 1.0
                            + scenario.gapBps()
                            / 10_000.0
                            : 1.0;

            double stressedMid =
                    volatilityMid
                            * gapMultiplier;

            double originalHalfSpread =
                    (
                            event.ask()
                                    - event.bid()
                    ) / 2.0;

            double stressedHalfSpread =
                    originalHalfSpread
                            * scenario
                            .spreadMultiplier();

            double bid =
                    stressedMid
                            - stressedHalfSpread;

            double ask =
                    stressedMid
                            + stressedHalfSpread;

            double lastBase =
                    volatilityMid
                            + (
                            event.last()
                                    - originalMid
                    )
                            * scenario
                            .volatilityMultiplier();

            double last =
                    lastBase
                            * gapMultiplier;

            validatePrices(
                    bid,
                    ask,
                    last,
                    scenario.name()
            );

            transformed.add(
                    new MarketEvent(
                            event.timestampMs(),
                            event.sourceSequence(),
                            event.type(),
                            event.symbol(),
                            event.venue(),
                            bid,
                            ask,
                            last,
                            scaleQuantity(
                                    event.volume(),
                                    scenario
                                            .liquidityMultiplier()
                            ),
                            scaleQuantity(
                                    event.queueDepth(),
                                    scenario
                                            .queueDepthMultiplier()
                            )
                    )
            );
        }

        if (transformed.isEmpty()) {
            throw new IllegalArgumentException(
                    "scenario removed all market events"
            );
        }

        return MarketDataReplay.of(
                transformed
        );
    }

    private static long gapTimestamp(
            List<MarketEvent> events,
            StressScenario scenario
    ) {
        if (scenario.gapBps() == 0.0) {
            return Long.MAX_VALUE;
        }

        int index =
                (int) Math.floor(
                        scenario.gapFraction()
                                * (
                                events.size() - 1
                        )
                );

        return events.get(index)
                .timestampMs();
    }

    private static long scaleQuantity(
            long quantity,
            double multiplier
    ) {
        return (long) Math.floor(
                quantity * multiplier
        );
    }

    private static void validatePrices(
            double bid,
            double ask,
            double last,
            String scenarioName
    ) {
        if (
                !Double.isFinite(bid)
                        || !Double.isFinite(ask)
                        || !Double.isFinite(last)
                        || bid <= 0.0
                        || ask <= 0.0
                        || last <= 0.0
                        || ask < bid
        ) {
            throw new IllegalArgumentException(
                    "scenario produced invalid prices: "
                            + scenarioName
            );
        }
    }

    private record MarketKey(
            String symbol,
            String venue
    ) {}

    private record PriceState(
            double originalMid,
            double stressedMid
    ) {}
}
