package com.junwenzheng.execution.routing;

import java.util.List;

public record RoutingDecision(
        int requestedQuantity,
        List<RouteAllocation> allocations
) {
    public RoutingDecision {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException(
                    "requestedQuantity must be positive"
            );
        }

        if (allocations == null) {
            throw new IllegalArgumentException(
                    "allocations are required"
            );
        }

        allocations = List.copyOf(allocations);

        if (routedQuantity(allocations)
                > requestedQuantity) {
            throw new IllegalArgumentException(
                    "routed quantity exceeds request"
            );
        }
    }

    public int routedQuantity() {
        return routedQuantity(allocations);
    }

    public int unallocatedQuantity() {
        return requestedQuantity
                - routedQuantity();
    }

    public boolean fullyRouted() {
        return unallocatedQuantity() == 0;
    }

    private static int routedQuantity(
            List<RouteAllocation> allocations
    ) {
        return Math.toIntExact(
                allocations.stream()
                        .mapToLong(
                                RouteAllocation::quantity
                        )
                        .sum()
        );
    }
}
