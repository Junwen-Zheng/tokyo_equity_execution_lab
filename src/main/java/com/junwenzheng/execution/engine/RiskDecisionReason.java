package com.junwenzheng.execution.engine;

public enum RiskDecisionReason {
    ALLOWED,
    INVALID_ORDER_QUANTITY,
    INVALID_REFERENCE_PRICE,
    MAX_CHILD_QUANTITY,
    MAX_CHILD_NOTIONAL,
    MAX_ABSOLUTE_POSITION
}
