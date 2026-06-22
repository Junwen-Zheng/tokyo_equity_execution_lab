# Execution quality report

This report compares execution strategies under the same parent order and replayed market tape. Lower slippage is better for a BUY order.

| Strategy | Fill Rate | Avg Price | Market VWAP | Arrival Slip (bps) | VWAP Slip (bps) | Rejected Children |
|---|---:|---:|---:|---:|---:|---:|
| POV | 100.00% | 100.4507 | 102.3854 | 39.92 | -188.96 | 0 |
| TWAP | 100.00% | 102.3861 | 102.3854 | 233.36 | 0.07 | 0 |
| VWAP | 100.00% | 102.4039 | 102.3854 | 235.14 | 1.81 | 0 |

## Interpretation

The result should not be interpreted as a profitable trading signal. It is an execution-quality comparison under a deterministic replay and fill model. The useful engineering evidence is the explicit order lifecycle, reproducible market replay, strategy abstraction, risk gate, fill constraints, and evaluation output.
