package com.junwenzheng.execution.algo;

public record ExecutionDecision(int childQuantity, String reason) {
    public static ExecutionDecision none(String reason) {
        return new ExecutionDecision(0, reason);
    }

    public boolean shouldTrade() {
        return childQuantity > 0;
    }
}
