package com.junwenzheng.execution.benchmark;

import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.market.MarketEventType;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import com.junwenzheng.execution.routing.RoutingDecision;
import com.junwenzheng.execution.routing.SmartOrderRouter;
import com.junwenzheng.execution.routing.VenueConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(
        iterations = 3,
        time = 500,
        timeUnit = TimeUnit.MILLISECONDS
)
@Measurement(
        iterations = 5,
        time = 500,
        timeUnit = TimeUnit.MILLISECONDS
)
@Fork(
        value = 2,
        jvmArgsAppend = {
                "-Xms512m",
                "-Xmx512m"
        }
)
@Threads(1)
public class ExecutionPathBenchmark {

    @Benchmark
    public double replayVwap(
            ExecutionState state
    ) {
        return state.singleVenueReplay.vwap();
    }

    @Benchmark
    public RoutingDecision routeFourVenueSnapshot(
            RoutingState state
    ) {
        return state.router.route(
                Side.BUY,
                2_000,
                state.snapshot,
                100
        );
    }

    @Benchmark
    public SimulationResult singleVenuePov(
            ExecutionState state
    ) {
        ParentOrder parentOrder =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        20_000,
                        state.singleArrivalPrice
                );

        return state.singleVenueSimulator.run(
                parentOrder,
                state.singleVenueReplay,
                state.povAlgorithm
        );
    }

    @Benchmark
    public SimulationResult routedPov(
            ExecutionState state
    ) {
        ParentOrder parentOrder =
                new ParentOrder(
                        "JPXDEMO",
                        Side.BUY,
                        20_000,
                        state.routedArrivalPrice
                );

        return state.routedSimulator.run(
                parentOrder,
                state.routedReplay,
                state.povAlgorithm
        );
    }

    @State(Scope.Thread)
    public static class ExecutionState {
        private MarketDataReplay singleVenueReplay;
        private MarketDataReplay routedReplay;
        private ExecutionSimulator
                singleVenueSimulator;
        private ExecutionSimulator routedSimulator;
        private PovAlgorithm povAlgorithm;
        private double singleArrivalPrice;
        private double routedArrivalPrice;

        @Setup(Level.Trial)
        public void setUp() {
            singleVenueReplay =
                    singleVenueReplay();

            routedReplay =
                    routedReplay();

            singleArrivalPrice =
                    singleVenueReplay.events()
                            .getFirst()
                            .ask();

            routedArrivalPrice =
                    routedReplay.events()
                            .getFirst()
                            .ask();

            RiskManager riskManager =
                    new RiskManager(
                            2_000,
                            250_000.0
                    );

            FillModel fillModel =
                    new FillModel(
                            0.12,
                            1.6
                    );

            singleVenueSimulator =
                    new ExecutionSimulator(
                            riskManager,
                            fillModel,
                            LatencyProfile.zero()
                    );

            routedSimulator =
                    ExecutionSimulator.routed(
                            riskManager,
                            fillModel,
                            LatencyProfile.zero(),
                            configuredRouter()
                    );

            povAlgorithm =
                    new PovAlgorithm(
                            0.08,
                            1_000
                    );
        }
    }

    @State(Scope.Thread)
    public static class RoutingState {
        private SmartOrderRouter router;
        private List<MarketEvent> snapshot;

        @Setup(Level.Trial)
        public void setUp() {
            router = configuredRouter();

            snapshot = List.of(
                    event(
                            0L,
                            0L,
                            "TSE",
                            99.98,
                            100.02,
                            100.00,
                            4_000L,
                            2_500L
                    ),
                    event(
                            0L,
                            1L,
                            "PTS_A",
                            99.97,
                            100.01,
                            99.99,
                            3_200L,
                            1_800L
                    ),
                    event(
                            0L,
                            2L,
                            "PTS_B",
                            99.99,
                            100.03,
                            100.01,
                            2_800L,
                            1_500L
                    ),
                    event(
                            0L,
                            3L,
                            "DARK",
                            99.96,
                            100.00,
                            99.98,
                            2_200L,
                            1_000L
                    )
            );
        }
    }

    private static MarketDataReplay
    singleVenueReplay() {
        List<MarketEvent> events =
                new ArrayList<>();

        for (int index = 0; index < 480; index++) {
            double midpoint =
                    100.0
                            + index * 0.005
                            + Math.sin(
                            index / 12.0
                    ) * 0.10;

            double halfSpread =
                    0.010
                            + (
                            index % 5
                    ) * 0.001;

            long volume =
                    2_000L
                            + (
                            index % 20
                    ) * 100L;

            events.add(
                    event(
                            index * 1_000L,
                            index,
                            "PRIMARY",
                            midpoint - halfSpread,
                            midpoint + halfSpread,
                            midpoint,
                            volume,
                            volume
                    )
            );
        }

        return MarketDataReplay.of(events);
    }

    private static MarketDataReplay
    routedReplay() {
        List<MarketEvent> events =
                new ArrayList<>();

        String[] venues = {
                "TSE",
                "PTS_A",
                "PTS_B",
                "DARK"
        };

        double[] midpointOffsets = {
                0.000,
                -0.015,
                0.010,
                -0.005
        };

        long sequence = 0L;

        for (
                int snapshotIndex = 0;
                snapshotIndex < 120;
                snapshotIndex++
        ) {
            double baseMidpoint =
                    100.0
                            + snapshotIndex * 0.02
                            + Math.sin(
                            snapshotIndex / 8.0
                    ) * 0.08;

            for (
                    int venueIndex = 0;
                    venueIndex < venues.length;
                    venueIndex++
            ) {
                double midpoint =
                        baseMidpoint
                                + midpointOffsets[
                                venueIndex
                                ];

                double halfSpread =
                        0.010
                                + venueIndex * 0.002;

                long volume =
                        1_800L
                                + (
                                snapshotIndex % 12
                        ) * 100L
                                + venueIndex * 150L;

                long queueDepth =
                        1_000L
                                + (
                                snapshotIndex % 8
                        ) * 75L
                                + venueIndex * 100L;

                events.add(
                        event(
                                snapshotIndex
                                        * 1_000L,
                                sequence,
                                venues[venueIndex],
                                midpoint - halfSpread,
                                midpoint + halfSpread,
                                midpoint,
                                volume,
                                queueDepth
                        )
                );

                sequence++;
            }
        }

        return MarketDataReplay.of(events);
    }

    private static SmartOrderRouter
    configuredRouter() {
        return new SmartOrderRouter(
                List.of(
                        new VenueConfiguration(
                                "TSE",
                                0.10,
                                0.60,
                                0.20
                        ),
                        new VenueConfiguration(
                                "PTS_A",
                                -0.05,
                                0.50,
                                0.35
                        ),
                        new VenueConfiguration(
                                "PTS_B",
                                0.05,
                                0.45,
                                0.15
                        ),
                        new VenueConfiguration(
                                "DARK",
                                0.00,
                                0.35,
                                0.50
                        )
                )
        );
    }

    private static MarketEvent event(
            long timestampMs,
            long sourceSequence,
            String venue,
            double bid,
            double ask,
            double last,
            long volume,
            long queueDepth
    ) {
        return new MarketEvent(
                timestampMs,
                sourceSequence,
                MarketEventType.CONTINUOUS,
                "JPXDEMO",
                venue,
                bid,
                ask,
                last,
                volume,
                queueDepth
        );
    }
}
