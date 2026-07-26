# Day 10 — Tokyo market rules

## Objective

Add an explicit, testable Tokyo cash-equity rule layer covering session
eligibility, auctions, price ticks, and board-lot quantities without breaking
the repository's existing generic replay and simulator behaviour.

## Compatibility decision

The existing synthetic replay uses relative timestamps beginning at zero.
Interpreting those values automatically as Japan time-of-day would invalidate
the demo and silently change all legacy results.

Tokyo behaviour is therefore opt-in through:

- `ExecutionSimulator.tokyo(...)`
- `ExecutionSimulator.routedTokyo(...)`

Generic and routed simulator factories retain their prior semantics.

## Session model

`TokyoSessionSchedule` accepts milliseconds since midnight JST and classifies:

- 09:00 morning opening auction
- morning continuous trading
- 11:30 morning closing auction
- lunch break
- 12:30 afternoon opening auction
- afternoon continuous trading
- 15:25–15:30 pre-close without execution
- 15:30 afternoon closing auction
- closed periods

An event is executable only when its `MarketEventType` matches the relevant
session phase.

## Tick-size model

`TokyoTickSizeTable` implements two deterministic tables:

- `TOPIX_500`
- `OTHER_ISSUE`

Each table resolves the tick from the price band and validates decimal
alignment using `BigDecimal`, avoiding binary floating-point remainder checks.

Tokyo validation applies to:

- parent arrival price
- event bid
- event ask
- event last

## Board-lot model

`TokyoEquityRules` defaults to a 100-share trading unit and also permits a
custom positive lot size for testing or alternate instruments.

The rule layer:

- rejects non-lot parent quantities
- normalises desired child quantity downward
- caps quantity by parent remaining quantity
- exposes only full-lot venue capacity
- rejects odd-lot child orders
- rounds fillable liquidity down before determining fill quantity

The fill outcome continues to report raw executable liquidity so its invariant
remains:

    queue-ahead quantity + executable liquidity = participation cap

The actual fill quantity may be lower because of board-lot rounding.

## Venue handling

A defect found during integration was that the single-venue simulator created
children with the legacy `PRIMARY` venue even when the selected event belonged
to `TSE`.

Single-venue children now inherit the event venue. This preserves the fill
model invariant that child and market-event venues must match.

## Routing behaviour

The generic router retains lot size one.

A lot-aware overload:

- requires requested quantity to be a complete lot
- rounds each venue's available capacity downward
- preserves deterministic effective-price ranking
- reports residual quantity explicitly when full-lot capacity is insufficient

## Verification

The Day 10 suite contains 127 tests.

Added coverage includes:

- session boundaries and invalid timestamps
- continuous and auction eligibility
- both Tokyo tick-size schedules
- decimal tick alignment
- parent and child board lots
- lot-aware routing
- Tokyo single-venue simulation
- Tokyo routed simulation
- lunch and pre-close filtering
- closing-auction execution
- invalid tick rejection
- odd-lot fill rejection
- fill-liquidity invariant preservation

At the final implementation checkpoint:

- 127 tests passed
- 0 failures
- 0 errors
- 0 skipped

## Remaining limitations

This is not a complete JPX implementation. It does not include daily price
limits, special quotes, detailed auction matching, order-type eligibility,
holiday calendars, order priority, hidden liquidity, short-sale rules,
clearing, or settlement.
