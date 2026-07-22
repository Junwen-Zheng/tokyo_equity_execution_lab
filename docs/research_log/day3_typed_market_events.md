# Day 3 — Typed Market Events and Deterministic Replay

## Objective

Strengthen the market-data boundary before changing execution, fill, or
algorithm behaviour.

## Typed market events

`MarketEvent` now includes:

- source sequence
- explicit event type
- timestamp
- symbol
- bid, ask, and last prices
- volume

Supported event types are:

- `CONTINUOUS`
- `OPENING_AUCTION`
- `CLOSING_AUCTION`

The original six-argument constructor remains available and defaults to a
continuous event with source sequence zero.

## CSV compatibility

The replay parser accepts both:

- the original six-column format
- a seven-column format with `event_type`

Existing generated datasets and demos therefore remain compatible.

## Replay ordering

Events are ordered by:

1. timestamp
2. source sequence

Source sequence is assigned according to CSV row order. Events with identical
timestamps therefore retain deterministic source ordering.

## Validation and diagnostics

The parser now provides source path and line number for malformed data.

Validation covers:

- incorrect column counts
- invalid numeric values
- unsupported event types
- non-finite prices
- non-positive prices
- crossed quotes
- negative timestamps
- negative volume

## Replay filtering

Immutable replay views can be created by:

- symbol
- timestamp window
- event type

Filters are composable and do not modify the original replay.

## Deterministic event clock

`DeterministicEventClock` advances only from replay event timestamps.

It:

- starts uninitialized
- permits equal timestamps
- rejects backward time
- exposes the current deterministic event time

`ExecutionSimulator` now uses this clock when timestamping child orders.

## Verification

The suite contains 20 tests covering:

- Maven/JUnit baseline behaviour
- legacy defect characterizations
- typed CSV parsing
- deterministic source ordering
- validation diagnostics
- immutable replay filtering
- deterministic clock behaviour

## Day 3 conclusion

The market-data boundary is now typed, validated, deterministic, and
filterable. Existing execution and fill semantics remain unchanged.
