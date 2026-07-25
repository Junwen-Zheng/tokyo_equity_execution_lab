package com.junwenzheng.execution.engine;

public enum LatencyStage {
    MARKET_EVENT,
    DECISION,
    RISK_CHECK,
    ACKNOWLEDGEMENT,
    REJECTION,
    FILL,
    CANCELLATION
}
