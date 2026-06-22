# Resume snippet

Tokyo Equity Execution Lab | Java Trading Systems Case Study | 2026
GitHub: https://github.com/Junwen-Zheng/tokyo-equity-execution-lab

- Built a Java 21 market-data replay and execution-simulation engine that ingests trade/quote-style CSV data, replays timestamped events, and drives TWAP, VWAP, and POV execution strategies through a shared strategy interface.
- Implemented parent/child order lifecycle, order status transitions, fill simulation, participation limits, risk checks, position tracking, rejected-child accounting, and deterministic execution reports for a BUY parent order under identical market-tape conditions.
- Evaluated execution quality with fill rate, average execution price, market VWAP, slippage versus arrival price, slippage versus VWAP, implementation-shortfall framing, and latency/throughput microbenchmarks; documented limitations and next steps for multi-venue SOR and real-data adapters.
