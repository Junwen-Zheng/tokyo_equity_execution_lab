package com.junwenzheng.execution;

import com.junwenzheng.execution.algo.PovAlgorithm;
import com.junwenzheng.execution.engine.ExecutionSimulator;
import com.junwenzheng.execution.engine.FillModel;
import com.junwenzheng.execution.engine.LatencyEvent;
import com.junwenzheng.execution.engine.LatencyProfile;
import com.junwenzheng.execution.engine.LatencyStage;
import com.junwenzheng.execution.engine.RiskManager;
import com.junwenzheng.execution.engine.SimulationResult;
import com.junwenzheng.execution.market.MarketDataReplay;
import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ChildOrder;
import com.junwenzheng.execution.order.ParentOrder;
import com.junwenzheng.execution.order.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LatencyPipelineIntegrationTest {

    @Test
    void filledChildRecordsDeterministicStages() {
        SimulationResult result =
                simulator(
                        new LatencyProfile(
                                2L,
                                3L,
                                5L,
                                7L,
                                11L
                        )
                ).run(
                        parent(10),
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        ChildOrder child =
                result.childOrders().getFirst();

        assertEquals(1_002L, child.timestampMs());
        assertEquals(1_017L, child.lastUpdateTimestampMs());

        assertEquals(
                List.of(
                        LatencyStage.MARKET_EVENT,
                        LatencyStage.DECISION,
                        LatencyStage.RISK_CHECK,
                        LatencyStage.ACKNOWLEDGEMENT,
                        LatencyStage.FILL
                ),
                result.latencyEvents().stream()
                        .map(LatencyEvent::stage)
                        .toList()
        );

        assertEquals(
                List.of(
                        1_000L,
                        1_002L,
                        1_005L,
                        1_010L,
                        1_017L
                ),
                result.latencyEvents().stream()
                        .map(LatencyEvent::timestampMs)
                        .toList()
        );

        assertEquals(
                1_017L,
                result.fills()
                        .getFirst()
                        .timestampMs()
        );
    }

    @Test
    void partialFillRecordsCancellationLatency() {
        SimulationResult result =
                simulator(
                        new LatencyProfile(
                                1L,
                                1L,
                                1L,
                                2L,
                                4L
                        )
                ).run(
                        parent(100),
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        assertEquals(
                List.of(
                        LatencyStage.MARKET_EVENT,
                        LatencyStage.DECISION,
                        LatencyStage.RISK_CHECK,
                        LatencyStage.ACKNOWLEDGEMENT,
                        LatencyStage.FILL,
                        LatencyStage.CANCELLATION
                ),
                result.latencyEvents().stream()
                        .map(LatencyEvent::stage)
                        .toList()
        );

        assertEquals(
                1_009L,
                result.childOrders()
                        .getFirst()
                        .lastUpdateTimestampMs()
        );
    }

    @Test
    void rejectedChildRecordsRiskAndRejectionStages() {
        SimulationResult result =
                new ExecutionSimulator(
                        new RiskManager(
                                5,
                                1_000_000.0
                        ),
                        new FillModel(
                                0.10,
                                1.6
                        ),
                        new LatencyProfile(
                                1L,
                                2L,
                                3L,
                                4L,
                                5L
                        )
                ).run(
                        parent(100),
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        assertEquals(
                List.of(
                        LatencyStage.MARKET_EVENT,
                        LatencyStage.DECISION,
                        LatencyStage.RISK_CHECK,
                        LatencyStage.REJECTION
                ),
                result.latencyEvents().stream()
                        .map(LatencyEvent::stage)
                        .toList()
        );

        assertEquals(
                1_003L,
                result.childOrders()
                        .getFirst()
                        .lastUpdateTimestampMs()
        );
    }

    @Test
    void resultLatencyEventsAreImmutable() {
        SimulationResult result =
                simulator(
                        LatencyProfile.zero()
                ).run(
                        parent(10),
                        replay(100L),
                        new PovAlgorithm(
                                0.50,
                                100
                        )
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.latencyEvents().clear()
        );
    }

    private static ExecutionSimulator simulator(
            LatencyProfile profile
    ) {
        return new ExecutionSimulator(
                new RiskManager(
                        1_000,
                        1_000_000.0
                ),
                new FillModel(
                        0.10,
                        1.6
                ),
                profile
        );
    }

    private static ParentOrder parent(
            int quantity
    ) {
        return new ParentOrder(
                "JPXDEMO",
                Side.BUY,
                quantity,
                100.2
        );
    }

    private static MarketDataReplay replay(
            long volume
    ) {
        return MarketDataReplay.of(
                List.of(
                        new MarketEvent(
                                1_000L,
                                "JPXDEMO",
                                100.0,
                                100.2,
                                100.1,
                                volume
                        )
                )
        );
    }
}
