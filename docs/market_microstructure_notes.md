# Market microstructure notes

This project is intentionally framed as execution technology rather than alpha research. The core question is not whether the system can predict future returns, but whether an execution engine can make explicit trade-offs between urgency, liquidity access, slippage, and operational risk.

## Concepts represented in the replay

- **Bid/ask spread**: used as a simple proxy for immediate execution cost.
- **Market VWAP**: used as a benchmark for whether a schedule stayed close to the tape's traded volume profile.
- **Participation**: used to prevent child orders from unrealistically consuming the full displayed event volume.
- **Implementation shortfall**: measured against the parent order's arrival price.
- **Rejected child orders**: used as a simple risk-control signal when child order size or notional exceeds configured limits.

## What is deliberately out of scope

This is not a matching engine and does not claim exchange-level realism. It does not model queue position, hidden liquidity, venue fees, auction periods, internalisation, short-sale constraints, or real Asia equities venue-specific rules. Those would be natural extensions after the execution loop and reporting layer are stable.
