package com.junwenzheng.execution.algo;

import com.junwenzheng.execution.market.MarketEvent;
import com.junwenzheng.execution.order.ParentOrder;

public interface ExecutionAlgorithm {
    String name();
    ExecutionDecision onEvent(ParentOrder parentOrder, MarketEvent event, ReplayProgress progress);
}
