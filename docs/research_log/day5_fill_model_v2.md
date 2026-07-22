# Day 5 — Fill Model v2

## Objective

Replace the legacy guaranteed-fill model with an explicit, deterministic
liquidity and queue model.

## Explicit fill outcomes

Fill attempts now return one of two outcomes:

- `Filled`
- `NoFill`

A fill is no longer manufactured when executable liquidity is unavailable.

No-fill reasons include:

- zero event volume
- participation cap rounded to zero
- queue-ahead quantity consumed all executable liquidity

## Participation constraint

Executable quantity begins with a deterministic participation cap:

`floor(event volume * maximum participation)`

The model does not force a minimum one-share fill.

## Queue-ahead model

A configurable queue-ahead fraction consumes part of the participation cap.

The outcome records:

- participation cap
- queue-ahead quantity
- executable liquidity

This makes the liquidity calculation observable and testable.

## Quantity constraints

Fill quantity is capped by:

- executable event liquidity
- the child order's remaining quantity

Inactive, rejected, cancelled, and completed children cannot be filled.

## Price decomposition

Each fill now records:

- reference midpoint
- spread cost in basis points
- participation-impact cost in basis points
- total cost in basis points
- final execution price

For buys, execution cost is applied above the midpoint.

For sells, execution cost is applied below the midpoint.

## Validation

The model validates:

- finite participation parameters
- queue fraction in the range `[0, 1]`
- non-negative impact coefficient
- active child state
- matching child and market symbols
- monotonic event and lifecycle timestamps
- finite positive generated prices

## Simulator integration

`ExecutionSimulator` now handles no-fill outcomes explicitly.

When no fill occurs:

1. the acknowledged child is cancelled
2. no fill is added
3. no position is changed
4. no parent quantity is filled

Incomplete parents remain cancelled at the end of replay processing.

## Legacy defect removal

The Day 1 characterization asserting a forced one-share fill on zero volume
was removed because the defect has now been intentionally fixed.

The remaining legacy characterizations continue to document unresolved
behaviour scheduled for later days.

## Verification

The suite contains 42 tests covering:

- existing baseline behaviour
- deterministic market replay
- order lifecycle transitions
- zero-volume no-fill behaviour
- participation-cap rounding
- deterministic queue depletion
- child remaining-quantity limits
- inactive child rejection
- symbol validation
- spread and impact decomposition
- buy and sell price direction
- invalid model configuration
- simulator no-fill lifecycle integration

## Day 5 conclusion

Fill generation is now explicit, liquidity constrained, queue aware, and
deterministic. The simulator no longer fabricates fills in the absence of
market volume.
