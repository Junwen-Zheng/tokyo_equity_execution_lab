# Day 8 — Execution Algorithm Correctness

## Objective

Correct TWAP, VWAP, and POV scheduling semantics and separate oracle VWAP
from an online implementation that does not depend on future replay volume.

## TWAP correction

The TWAP slice parameter now acts as a minimum executable slice rather than
an accidental maximum cap.

The algorithm calculates:

1. cumulative target from elapsed replay progress
2. schedule deficit against actual parent fills
3. a child quantity at least as large as the configured minimum slice
4. a final cap at the parent remaining quantity

When the parent is already at or ahead of schedule, no child is generated.

## Oracle VWAP

The existing VWAP implementation is now explicitly named `VWAP_ORACLE`.

It uses the full replay total volume to calculate the realised cumulative
market-volume fraction.

This implementation is retained as an offline benchmark because it has
look-ahead access to the complete replay tape.

## Online VWAP

`OnlineVwapAlgorithm` uses:

- observed cumulative market volume
- a fixed ex-ante forecast of total session volume
- a maximum child slice quantity

It never reads `ReplayProgress.totalVolume()` when making decisions.

Its strategy name is `VWAP_ONLINE` so reports clearly distinguish it from the
oracle benchmark.

## POV correction

POV now tracks a cumulative participation target:

`floor(cumulative observed volume * participation rate)`

The algorithm compares this target with actual parent filled quantity.

This allows it to catch up after previous underfills rather than treating each
market event independently.

The generated child remains capped by:

- maximum slice quantity
- parent remaining quantity

## Replay progress validation

`ReplayProgress` now rejects:

- non-positive event counts
- event indexes outside replay bounds
- negative cumulative or total volume
- cumulative volume above total replay volume

It exposes separate methods for:

- elapsed event progress
- oracle replay-volume fraction
- observed volume against a supplied forecast

The legacy `volumeFraction()` method remains as an alias for oracle volume
fraction.

## Execution decision validation

Execution decisions now reject:

- negative child quantities
- null or blank decision reasons

No-trade decisions remain represented by a zero child quantity.

## Configuration validation

TWAP, oracle VWAP, online VWAP, and POV now reject invalid configuration,
including:

- non-positive slice quantities
- invalid participation rates
- non-finite participation rates
- non-positive online volume forecasts

## Demo integration

The executable comparison now includes:

- `TWAP`
- `VWAP_ORACLE`
- `VWAP_ONLINE`
- `POV`

The online volume forecast is constructed before simulation and passed
explicitly to the online strategy.

## Legacy characterization removal

The remaining legacy characterization tests were removed because both
documented defects are fixed:

- TWAP no longer caps schedule deficits at its minimum-slice setting
- online VWAP no longer depends on full future replay volume

Oracle VWAP retains future replay access intentionally and is labelled
accordingly.

## Verification

The suite contains 71 tests covering:

- corrected TWAP minimum-slice behaviour
- TWAP schedule-deficit execution
- TWAP no-trade behaviour when on schedule
- explicit oracle VWAP behaviour
- online VWAP independence from replay total volume
- online VWAP slice limits
- cumulative POV catch-up
- POV no-trade behaviour at target
- algorithm constructor validation
- replay progress validation
- execution decision validation
- distinct strategy names
- all previous lifecycle, fill, latency, replay, and risk controls

## Day 8 conclusion

The execution algorithms now expose their information assumptions explicitly.
TWAP and POV use correct cumulative catch-up logic, while oracle and online
VWAP are separate strategies rather than an ambiguous single implementation.
