# Strategy comparison notes

This repo compares three intentionally simple execution schedules. The goal is to show strategy abstraction and evaluation discipline, not to claim production algo quality.

## TWAP

TWAP slices the parent order into roughly constant child sizes. It is easy to reason about and stable under noisy volume estimates, but it can trade too aggressively during thin liquidity periods and too passively when volume is abundant.

## VWAP

VWAP attempts to align child order size with the observed cumulative-volume profile. It can reduce benchmark slippage when the replay tape has a stable volume curve, but it is vulnerable to overfitting if the expected profile is estimated from too little history.

## POV

POV trades as a percentage of observed event volume. It is naturally liquidity-aware, but it may fail to complete the parent order if the tape is thin or the participation cap is conservative.

## Practical interpretation

A real execution platform would usually combine these ideas with venue selection, risk controls, auction logic, child-order throttling, market-impact models, and trader-configurable urgency parameters.
