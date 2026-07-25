package com.junwenzheng.execution;

import com.junwenzheng.execution.engine.DeterministicLatencyPipeline;
import com.junwenzheng.execution.engine.LatencyProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DeterministicLatencyPipelineTest {

    @Test
    void baselineProfileAdvancesEachStage() {
        DeterministicLatencyPipeline pipeline =
                new DeterministicLatencyPipeline(
                        LatencyProfile
                                .deterministicBaseline()
                );

        long eventTime = 1_000L;

        long decisionTime =
                pipeline.decisionTimestampMs(
                        eventTime
                );

        long riskTime =
                pipeline.riskTimestampMs(
                        decisionTime
                );

        long acknowledgementTime =
                pipeline
                        .acknowledgementTimestampMs(
                                riskTime
                        );

        long fillTime =
                pipeline.fillTimestampMs(
                        acknowledgementTime
                );

        long cancellationTime =
                pipeline.cancellationTimestampMs(
                        fillTime
                );

        assertEquals(1_001L, decisionTime);
        assertEquals(1_002L, riskTime);
        assertEquals(1_003L, acknowledgementTime);
        assertEquals(1_005L, fillTime);
        assertEquals(1_006L, cancellationTime);
    }

    @Test
    void zeroProfilePreservesTimestamp() {
        DeterministicLatencyPipeline pipeline =
                new DeterministicLatencyPipeline(
                        LatencyProfile.zero()
                );

        assertEquals(
                1_000L,
                pipeline.decisionTimestampMs(
                        1_000L
                )
        );
    }

    @Test
    void negativeLatencyIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LatencyProfile(
                        -1L,
                        0L,
                        0L,
                        0L,
                        0L
                )
        );
    }

    @Test
    void timestampOverflowIsRejected() {
        DeterministicLatencyPipeline pipeline =
                new DeterministicLatencyPipeline(
                        new LatencyProfile(
                                1L,
                                0L,
                                0L,
                                0L,
                                0L
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> pipeline.decisionTimestampMs(
                        Long.MAX_VALUE
                )
        );
    }
}
