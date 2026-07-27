# Design notes

## Market-data schema

The replay engine expects CSV with this schema:

```text
timestamp_ms,symbol,bid,ask,last,volume
```

This is intentionally minimal. Real data can be normalised into this format from trade/quote snapshots or order-book feeds.

## State ownership

The strategy decides *how much it wants to trade now*. The execution engine decides whether that child order passes risk checks, how much gets filled, what price is assigned, and how positions are updated.

## Strategy interface

Each strategy receives:

- the parent order
- current market event
- elapsed replay state
- current remaining quantity

It returns an `ExecutionDecision` with target child quantity and reason.

## Fill model

The default fill model approximates execution using:

- side-dependent touch price (buy at ask, sell at bid)
- configurable max participation of event volume
- deterministic market-impact/slippage term based on child participation

This avoids claiming real exchange matching while still creating realistic constraints for evaluation.

## Smart order routing

The execution simulator supports multiple venues at the same timestamp using
venue-aware market rows with these fields:

    timestamp_ms,symbol,venue,bid,ask,last,volume,queue_depth

An optional final event-type field is also supported.

The deterministic router ranks destinations using:

1. side-aware effective price after fees
2. adverse-selection penalty
3. displayed queue depth
4. venue-specific participation capacity
5. venue name and source sequence for deterministic tie-breaking

A scheduling algorithm produces one desired quantity for a consolidated
market snapshot. The router may split that quantity across several venues.

Each allocation becomes a venue-specific child order with its own risk
decision, lifecycle, fill attempt, and terminal state.

## Tokyo equity market rules

Tokyo-specific behaviour is implemented as an opt-in rule layer rather than
being imposed on every replay. This preserves compatibility with the existing
synthetic data, whose timestamps are relative offsets rather than milliseconds
since midnight in Japan.

`TokyoSessionSchedule` classifies timestamps into:

- morning opening auction at 09:00
- morning continuous trading from after 09:00 until 11:30
- morning closing auction at 11:30
- lunch break until 12:30
- afternoon opening auction at 12:30
- afternoon continuous trading until 15:25
- non-executing pre-close from 15:25 until 15:30
- afternoon closing auction at 15:30
- closed periods outside those windows

`TokyoTickSizeTable` supports separate price-band schedules for TOPIX 500
constituents and other domestic issues. Bid, ask, last, and parent arrival
prices are validated against the selected table.

`TokyoEquityRules` enforces configurable board-lot quantities, with 100 shares
as the standard domestic-equity setting. Desired quantities, routed venue
capacity, and fills are rounded down to complete lots.

The execution engine exposes `tokyo(...)` and `routedTokyo(...)` factories.
Legacy simulator factories retain their original unrestricted behaviour.

For routed Tokyo execution, each venue exposes only complete-lot liquidity.
The fill model also prevents odd-lot fills after participation and queue-ahead
constraints have been applied.

## Transaction cost analysis

The TCA layer consumes an immutable `SimulationResult` after execution. It does
not alter order state, fill generation, routing, or replay behaviour.

For each fill, the analysis decomposes side-adjusted implementation shortfall
into:

1. **Delay cost** — movement from the parent arrival price to the fill's
   contemporaneous reference midpoint.
2. **Execution cost** — movement from that reference midpoint to the fill
   price.
3. **Spread cost** — the fill model's stored spread component converted from
   basis points to currency cost.
4. **Impact cost** — the fill model's stored impact component converted from
   basis points to currency cost.
5. **Residual execution cost** — execution cost less modeled spread and impact.

For unfilled quantity, opportunity cost uses the final replay midpoint:

    sideSign × (terminal midpoint - arrival price) × unfilled quantity

Total implementation shortfall is:

    filled shortfall + opportunity cost

The basis-point denominator is the complete parent arrival notional, including
unfilled quantity. This makes partially filled strategies directly comparable
with fully filled strategies and prevents a low fill rate from appearing
artificially inexpensive.

Venue attribution groups fills by destination and reports quantity, executed
notional, arrival-price shortfall, and venue shortfall in basis points.

The records enforce four reconciliation invariants:

- delay cost plus execution cost equals filled implementation shortfall
- spread plus impact plus residual equals execution cost
- filled shortfall plus opportunity cost equals total shortfall
- venue shortfall sums to filled implementation shortfall
