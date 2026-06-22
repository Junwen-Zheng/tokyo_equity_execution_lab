# Research log

## Phase 1 - Scope and role fit

The target role is Java algo execution technology. I deliberately avoided framing this as alpha research. The core question is:

> Can I build a small but inspectable execution stack that demonstrates market-data replay, order lifecycle, execution algorithms, risk checks, and execution-quality evaluation?

## Phase 2 - First implementation plan

Initial modules:

- Market-data replay from CSV
- Parent order and child order state machine
- Strategy interface with TWAP first
- Simple fill model constrained by event volume
- Execution report with fill rate and slippage

## Phase 3 - What did not work

A first design tried to make the strategy emit full `ChildOrder` objects. That made strategies too responsible for execution details. I changed the interface so strategies emit an `ExecutionDecision`, leaving fill/risk/position concerns inside the execution engine.

## Phase 4 - Evaluation design

I chose execution-quality metrics rather than PnL:

- Fill rate
- Average execution price
- Market VWAP over the replay window
- Slippage versus arrival price
- Slippage versus market VWAP
- Implementation shortfall
- Realised participation rate

This aligns better with algo-execution engineering than with alpha research.

## Phase 5 - Current result

The project now supports TWAP, VWAP, and POV strategies. The sample run generates a comparative report showing that algorithm choice materially changes fill rate and slippage under the same market tape and parent order.

## Next work

- Add a real external data adapter for public quote/trade feeds
- Add multi-venue routing model and simple SOR decision rules
- Replace the simple fill model with queue-position approximations
- Add GC/allocation profiling with a proper JMH benchmark if Maven/Gradle is introduced
