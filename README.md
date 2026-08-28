# Bounded-Noise Privacy (BNP)

A comparison of differential privacy mechanisms on vehicle trajectory data.

## Overview

This project implements and compares three privacy mechanisms:
- **Laplace DP** — add noise to each data point individually
- **Relaxed Differential Privacy (RDP)** - Laplace plus a small error allowance
- **Bounded-Noise Privacy (BNP)** — model-based noise with synthetic data generation

## Quick Start

### Build
```bash
javac -cp lib/* src/App.java
```

### Run
```bash
java -cp lib/*:src App
```

This will:
1. Load vehicle speed data from `ngsim.csv`
2. Apply all three privacy mechanisms
3. Export results to `privacy_comparison2.csv`

## Key Methods

| Method | Approach |
|--------|---------- |
| **Laplace DP** | Add noise per point |
| **RDP** | Add noise per point, plus allow small error bound |
| **BNP** | Add uniform noise from bounded region |

## Files

- `src/App.java` — Main entry point, loads data and applies mechanisms
- `src/ComparisonDP.java` — Benchmark: speed, accuracy, privacy tradeoff
- `src/SimpleModelDP.java` — Minimal BNP implementation (easiest to understand)


## Privacy-Utility Tradeoff

```
ε=0.1   → Very private (lots of noise)
ε=0.5   → Good balance of privacy & utility
ε=2.0   → Weak privacy (minimal noise)
```

Lower ε = more private but less useful. Choose based on your privacy requirements.

## Dependencies

- Apache Commons Math 3 (for Laplace distribution sampling)
- OpenCSV (for reading/writing CSV files)

See `lib/` folder for JARs.


## Author

Erik Severtson  
University of Tulsa  

Research advisor: Dr. Mohammad Khajenejad
