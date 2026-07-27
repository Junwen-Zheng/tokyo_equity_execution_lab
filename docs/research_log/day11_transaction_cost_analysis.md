# Day 11 — Transaction cost analysis

## Objective

Add a deterministic post-trade transaction cost analysis layer that attributes
execution performance without modifying the simulator or overstating exchange
realism.

## Inputs already available

The existing execution records already expose the required information:

- parent side, quantity, and arrival price
- fill quantity and price
- contemporaneous reference midpoint
- modeled spread and impact costs
- fill venue and timestamp
- replay market VWAP
- terminal replay midpoint

No order or fill schema change was required.

## Cost decomposition

For a BUY order, higher prices are adverse. For a SELL order, lower prices are
adverse. All components therefore use a side sign so positive cost consistently
means worse execution.

For each fill:

    delay cost =
        sideSign
        × (reference midpoint - arrival price)
        × quantity

    execution cost =
        sideSign
        × (fill price - reference midpoint)
        × quantity

Filled implementation shortfall is the sum of delay and execution cost.

The fill model already records spread and impact in basis points. These values
are converted to currency cost using the fill reference midpoint and quantity.
Residual execution cost captures any remaining difference between realised
execution cost and the stored model components.

## Opportunity cost

Unfilled quantity is valued at the final replay midpoint:

    opportunity cost =
        sideSign
        × (terminal midpoint - arrival price)
        × unfilled quantity

This can be positive or negative. A missed BUY during a rising market is
adverse; a missed BUY during a falling market is beneficial under this
benchmark.

## Basis-point denominator

Total shortfall basis points use complete parent arrival notional:

    parent quantity × arrival price

Using only executed notional would understate the cost of an incomplete order
and make fill rates difficult to compare.

## Venue attribution

Fills are grouped deterministically by venue. Each venue reports:

- filled quantity
- executed notional
- filled implementation shortfall
- shortfall in basis points against venue-filled arrival notional

Venue rows are sorted lexicographically for reproducible reports.

## Reporting

The demo now generates:

- `transaction_cost_summary.csv`
- `venue_cost_attribution.csv`
- `transaction_cost_report.md`

The Markdown report includes:

- total TCA comparison
- execution-cost decomposition
- venue attribution
- explicit reconciliation identities

Generated TCA reports are ignored by Git because they are reproducible runtime
artefacts.

## Findings from the deterministic sample

The partially filled online VWAP strategy has a similar filled-price slippage
to oracle VWAP but a worse total implementation shortfall after opportunity
cost is included.

This demonstrates why average fill price alone is incomplete: an execution
strategy can appear competitive on completed quantity while leaving material
economic exposure unexecuted.

The sample residual execution cost is effectively zero because the fill price
is constructed from the same spread and impact components stored on each fill.

## Verification checkpoint

Added:

- 7 TCA calculation tests
- 4 TCA report tests

Current suite:

- 138 tests passed
- 0 failures
- 0 errors
- 0 skipped

## Limitations

The analysis uses deterministic replay benchmarks. It does not include actual
broker commissions, taxes, exchange invoices, financing, borrow cost, currency
conversion, market impact after the final replay event, or implementation
shortfall against a live decision price feed.
