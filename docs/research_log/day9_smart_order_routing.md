# Day 9 — Multi-Venue Smart Order Routing

## Objective

Extend the simulator from a single anonymous venue to deterministic,
auditable multi-venue smart order routing.

## Venue-aware market data

Market events now retain venue and displayed queue depth.

Legacy six- and seven-column CSV rows remain supported and default to the
PRIMARY venue. Venue-aware rows support both continuous and explicitly typed
events.

Replay data can also be filtered by venue.

## Venue-aware execution records

Venue identity is retained through the complete execution path:

- market event
- child order
- fill
- simulation result

The fill model rejects a fill attempt when the child destination and market
event venue do not match.

Legacy child-order and fill constructors remain compatible by defaulting to
PRIMARY.

## Routing configuration

Each destination can define:

- fee in basis points
- maximum participation rate
- adverse-selection penalty in basis points

All configuration values are validated.

## Effective routing price

For buys, fees and penalties increase the venue ask.

For sells, fees and penalties reduce the venue bid.

The router selects the lowest effective price for buys and the highest
effective price for sells.

## Routing capacity

Available venue capacity is the minimum of:

- displayed queue depth
- event volume multiplied by the venue participation limit

A requested quantity can be split across multiple venues. Any quantity above
aggregate available capacity remains explicitly unallocated.

## Deterministic ordering

Effective-price ties are resolved by venue name and then source sequence.

Routing therefore does not depend on map iteration or unstable input order.

## Consolidated snapshots

Events sharing a timestamp and symbol form one routing snapshot.

The execution algorithm receives a consolidated event containing the best
bid, best ask, volume-weighted last price, aggregate volume, and aggregate
displayed depth.

The scheduling algorithm determines desired quantity. The router determines
destination and allocation.

## Routed lifecycle

Every allocation creates a venue-specific child order with its own:

- market-event and decision timestamps
- pre-trade risk decision
- acknowledgement
- fill or cancellation
- terminal status

Position risk includes fills accumulated across every venue.

Routing decisions are retained immutably in SimulationResult.

## Backward compatibility

Existing ExecutionSimulator constructors retain the original single-venue
path.

Multi-venue execution is enabled explicitly through
ExecutionSimulator.routed.

Single-venue simulation results contain no routing decisions.

## Verification

The suite contains 96 tests covering legacy schema compatibility, venue
parsing and filtering, venue propagation, routing priority, fee and penalty
adjustments, displayed-liquidity limits, participation limits, deterministic
ties, quantity splitting, residual quantities, routed lifecycle behaviour,
cross-venue position risk, immutable results, and unchanged legacy execution.

## Day 9 conclusion

Scheduling and venue selection are now separate execution layers. Algorithms
produce desired quantities, while the router ranks destinations, allocates
liquidity, and preserves venue identity through risk, lifecycle, and fills.
