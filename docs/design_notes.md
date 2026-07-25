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

Regulatory rules, auction eligibility, and venue-session restrictions remain
future extensions.
