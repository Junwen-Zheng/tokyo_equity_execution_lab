package com.junwenzheng.execution.routing;

import com.junwenzheng.execution.market.MarketEvent;

public record RouteAllocation(
        MarketEvent event,
        int quantity,
        long availableQuantity,
        double effectivePrice
) {
    public RouteAllocation {
        if (event == null) {
            throw new IllegalArgumentException(
                    "event is required"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "quantity must be positive"
            );
        }

        if (availableQuantity < quantity) {
            throw new IllegalArgumentException(
                    "availableQuantity cannot be "
                            + "below routed quantity"
            );
        }

        if (
                !Double.isFinite(effectivePrice)
                        || effectivePrice <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "effectivePrice must be "
                            + "finite and positive"
            );
        }
    }

    public String venue() {
        return event.venue();
    }
}
