# Changelog

## 2.0.0 — 2026-07-27

### Added

- Maven and JUnit 5 build with Java 21 verification
- typed and deterministically ordered market events
- complete parent and child order lifecycle modelling
- liquidity-, spread-, impact-, queue-, and lot-aware fills
- deterministic execution-latency pipeline
- position-aware pre-trade risk controls
- corrected TWAP, POV, online VWAP, and oracle VWAP semantics
- deterministic multi-venue smart order routing
- Tokyo session, auction, tick-size, and board-lot rules
- transaction-cost attribution and venue-level reporting
- deterministic market stress and scenario testing
- reproducible OpenJDK JMH execution benchmarks
- GitHub Actions verification
- public design, methodology, limitation, and research documentation

### Verification

- 157 automated tests
- deterministic command-line demonstration
- four JMH benchmark paths
- CI build, test, benchmark-smoke, and repository-state checks

### Scope

This release is an engineering case study for execution-system architecture.
It is not an exchange matching engine, broker simulator, live trading system,
or calibrated forecast of production-market behaviour.
