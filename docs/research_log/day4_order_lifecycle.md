# Day 4 — Parent and Child Order Lifecycle

## Objective

Replace the simulator's implicit create-and-fill path with explicit,
validated parent and child order lifecycles.

## Parent lifecycle

`ParentOrder` now supports validated transitions through:

- `NEW`
- `WORKING`
- `PARTIALLY_FILLED`
- `FILLED`
- `CANCELLED`
- `REJECTED`

Parent orders now:

- validate side and finite positive arrival price
- reject zero and negative fills
- reject fills before entering the working state
- reject overfills instead of silently truncating them
- preserve filled and remaining quantity after cancellation
- expose terminal-state detection

## Child lifecycle

`ChildOrder` is now a lifecycle-aware class with:

- unique child-order identity
- parent-order identity
- submitted quantity
- filled and remaining quantity
- creation timestamp
- last lifecycle update timestamp
- explicit child status

Supported child transitions are:

- `NEW` to `ACKNOWLEDGED`
- `NEW` to `REJECTED`
- `ACKNOWLEDGED` to `PARTIALLY_FILLED`
- `ACKNOWLEDGED` to `FILLED`
- `PARTIALLY_FILLED` to `FILLED`
- `ACKNOWLEDGED` to `CANCELLED`
- `PARTIALLY_FILLED` to `CANCELLED`

Illegal transitions, backward lifecycle timestamps, and child overfills are
rejected.

## Fill identity

`Fill` now carries both:

- child-order ID
- parent-order ID

Fill fields validate identity, symbol, side, quantity, price, timestamp,
strategy, and reason.

## Simulator integration

`ExecutionSimulator` now:

1. creates and retains each child order
2. marks risk failures as rejected
3. acknowledges accepted children
4. validates fill identity and quantity
5. applies fills to both child and parent orders
6. records positions and fills
7. cancels any residual child quantity after the event
8. cancels an incomplete parent when replay processing ends

Execution quantities and existing metric calculations remain compatible.

## Simulation results

`SimulationResult` now retains immutable collections of:

- child orders
- fills

Rejected-child counts are derived from child lifecycle state rather than a
separate mutable counter.

## Verification

The suite contains 32 tests covering:

- build and baseline execution behaviour
- typed deterministic market replay
- deterministic event clock behaviour
- legacy defect characterization
- parent lifecycle transitions
- child lifecycle transitions
- invalid fills and timestamps
- risk rejection retention
- partial-fill cancellation
- completed parent and child orders
- immutable result collections

## Day 4 conclusion

Parent and child order state is now explicit, validated, and observable.
The simulator no longer bypasses acknowledgements, rejections, partial fills,
or cancellation transitions.
