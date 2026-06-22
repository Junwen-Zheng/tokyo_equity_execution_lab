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

## SOR extension

Smart order routing is not fully implemented yet. The next version would add multiple venues per timestamp:

```text
timestamp_ms,symbol,venue,bid,ask,last,volume,queue_depth
```

A simple SOR policy would route child quantity across venues by:

1. best price after fees,
2. available displayed liquidity,
3. venue-specific participation cap,
4. estimated adverse-selection penalty,
5. regulatory / market-session constraints.
```
