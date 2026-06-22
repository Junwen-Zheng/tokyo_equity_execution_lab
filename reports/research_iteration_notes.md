# Research iteration notes

## What changed after the first prototype

The initial prototype showed the basic execution loop, but it was too close to a demo. The follow-up work added clearer assumptions, microstructure diagnostics, implementation-shortfall helpers, and more explicit strategy comparison notes.

## What still needs work

- Replace synthetic data with a normalised public trade/quote dataset.
- Add sell-side parent-order examples and compare side-specific slippage.
- Add venue-level routing simulation instead of single-tape execution.
- Add more realistic queue and partial-fill assumptions.
- Add benchmark comparison before and after allocation-sensitive refactoring.

## Why this still matters

Even with simplified data, the project demonstrates the engineering path required for execution technology: deterministic replay, strategy interfaces, risk checks, fills, positions, metrics, and reproducible reports.
