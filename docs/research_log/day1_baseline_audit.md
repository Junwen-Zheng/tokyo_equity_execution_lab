# Day 1 — Baseline Audit and Legacy Behaviour

## Objective

Establish a reproducible baseline before changing the execution engine.

The original repository was cloned from GitHub and verified using Eclipse
Temurin JDK 21.

## Baseline

- Main Java source files: 26
- Existing test files: 1
- Build system: shell scripts and direct `javac`
- Test framework: custom `TestRunner`
- Maven or Gradle: not present
- GitHub Actions: not present

The original test and demo scripts completed successfully.

## Characterized legacy behaviour

The Day 1 characterization suite records four existing behaviours that are
considered defects or modelling limitations for v2.

### Zero-volume fills

`FillModel` forces a minimum fill quantity of one share even when the market
event reports zero volume.

This creates impossible liquidity and will be removed when the fill model is
redesigned.

### TWAP slice semantics

`TwapAlgorithm` names its setting `minSliceQty`, but the implementation
effectively applies it as a maximum child-order quantity.

The repeated `Math.max` expression does not provide minimum-slice behaviour.

### VWAP future-volume dependency

`VwapAlgorithm` uses `ReplayProgress.totalVolume`, which is calculated from
the complete replay.

A live decision therefore changes when future replay volume changes. This is
an oracle-volume benchmark rather than an implementable online VWAP strategy.

### Invalid reference prices

`RiskManager` accepts zero and negative reference prices because it validates
only child quantity and maximum notional.

Price validity checks will be added in the v2 risk system.

## Test strategy

The characterization tests intentionally assert the current behaviour. They
are not specifications for the desired v2 design.

When each production defect is corrected, the corresponding characterization
assertion must be replaced by a test for the new expected behaviour.

## Day 1 conclusion

The original repository is operational, but its simulator assumes immediate
fills, has minimal risk controls, and mixes implementable execution logic with
future-aware replay benchmarks.

Day 2 will introduce Maven, JUnit 5, and continuous integration before
production behaviour is changed.
