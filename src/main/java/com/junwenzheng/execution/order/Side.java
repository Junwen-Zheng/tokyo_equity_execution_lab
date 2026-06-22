package com.junwenzheng.execution.order;

public enum Side {
    BUY(1), SELL(-1);

    private final int sign;

    Side(int sign) {
        this.sign = sign;
    }

    public int sign() {
        return sign;
    }
}
