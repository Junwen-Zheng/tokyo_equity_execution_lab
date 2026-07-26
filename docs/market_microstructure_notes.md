# Market microstructure notes

This project is intentionally framed as execution technology rather than alpha research. The core question is not whether the system can predict future returns, but whether an execution engine can make explicit trade-offs between urgency, liquidity access, slippage, and operational risk.

## Concepts represented in the replay

- **Bid/ask spread**: used as a simple proxy for immediate execution cost.
- **Market VWAP**: used as a benchmark for whether a schedule stayed close to the tape's traded volume profile.
- **Participation**: used to prevent child orders from unrealistically consuming the full displayed event volume.
- **Implementation shortfall**: measured against the parent order's arrival price.
- **Rejected child orders**: used as a simple risk-control signal when child order size or notional exceeds configured limits.

## Additional microstructure represented in v2

- **Displayed queue depth**: constrains venue capacity and deterministic routing.
- **Venue costs**: fees and adverse-selection penalties affect route ranking.
- **Tokyo sessions**: continuous trading, auction boundaries, lunch, and pre-close are represented through an opt-in schedule.
- **Tick sizes**: TOPIX 500 and other-issue price bands are validated explicitly.
- **Board lots**: parent, child, route, and fill quantities can be restricted to complete trading units.

## What is deliberately out of scope

This remains an execution simulator rather than a matching engine and does not
claim exchange-level realism. It does not model order-book priority, hidden or
iceberg liquidity, order amendments, exchange-specific order types, dynamic
auction imbalance publication, daily price limits, special quotes, internalised
flow, short-sale controls, clearing, or settlement.

The Tokyo rule layer operates on milliseconds since midnight JST supplied by
the replay. It does not perform calendar, holiday, daylight-saving, or timestamp
time-zone conversion.
