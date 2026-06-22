# Execution-quality assumptions

The evaluation layer focuses on transparent execution-quality diagnostics rather than profitability.

## Assumptions

1. The parent order is known at the start of the replay.
2. All strategies receive the same deterministic market-data tape.
3. A child order can fill only up to a configured percentage of the event volume.
4. Impact is modeled as a simple function of event participation, not as a learned market-impact model.
5. For BUY orders, lower arrival-price slippage and lower VWAP slippage are preferred.

## Why these assumptions are useful

The assumptions are simplified, but they expose the engineering questions an execution platform needs to handle: deterministic replay, reproducible evaluation, order lifecycle state, risk gates, fill constraints, and reportable metrics. This is the relevant bridge for Java algo/execution technology roles.

## Known failure modes

- A strategy can look good on slippage while failing to complete enough quantity.
- A strategy can overfit to the synthetic volume curve.
- A low-impact fill model can make aggressive strategies look unrealistically good.
- Synthetic data cannot replace normalised real market data.
