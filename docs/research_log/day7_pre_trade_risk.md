# Day 7 — Pre-Trade Risk Controls

## Objective

Replace the legacy boolean-only risk gate with explicit, validated,
position-aware pre-trade risk decisions.

## Risk limits

`RiskManager` now validates and enforces:

- maximum child quantity
- maximum child notional
- maximum absolute projected position
- finite positive reference prices
- finite positive configuration values

The existing two-argument constructor remains supported and defaults the
absolute-position limit to `Integer.MAX_VALUE`.

## Explicit risk decisions

Risk evaluation now returns a `RiskDecision` containing:

- child order ID
- allowed or rejected status
- decision reason
- evaluated remaining quantity
- reference price
- child notional
- current position
- projected position

Supported reasons are:

- `ALLOWED`
- `INVALID_ORDER_QUANTITY`
- `INVALID_REFERENCE_PRICE`
- `MAX_CHILD_QUANTITY`
- `MAX_CHILD_NOTIONAL`
- `MAX_ABSOLUTE_POSITION`

## Position-aware evaluation

Projected position is calculated as:

`current position + signed child remaining quantity`

Buy orders increase projected position.

Sell orders decrease projected position.

Both long and short absolute-position breaches are rejected.

Risk-reducing orders remain permitted even when the starting position is
already above the configured limit, provided the projected position is within
the limit.

## Remaining quantity

Risk evaluation uses child remaining quantity rather than original order
quantity.

This makes repeated or partially filled child evaluation consistent with the
order lifecycle.

## Invalid prices and overflow

The risk gate rejects:

- zero reference prices
- negative reference prices
- non-finite reference prices
- non-finite notional calculations

The Day 1 characterization that accepted zero and negative reference prices
was removed because the defect is now fixed.

## Simulator integration

`ExecutionSimulator` now evaluates risk using the current symbol position from
`PositionTracker`.

Every generated child has one retained risk decision.

Rejected children preserve the specific rejection reason and projected
exposure that caused the rejection.

`SimulationResult` exposes an immutable risk-decision list.

## Verification

The suite contains 61 tests covering:

- existing execution behaviour
- deterministic market replay
- lifecycle and latency ordering
- liquidity-aware fills
- constructor validation
- invalid reference prices
- child quantity limits
- child notional limits
- long and short position limits
- risk-reducing orders
- remaining-quantity evaluation
- notional overflow
- accumulated simulator positions
- retained rejection reasons
- immutable risk-decision results

## Day 7 conclusion

Pre-trade risk is now explicit, auditable, and position aware. Risk rejection
is no longer represented only as a boolean result.
