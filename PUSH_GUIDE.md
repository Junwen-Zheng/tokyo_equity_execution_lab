# Push guide

Do not fake or backdate commit history. This repository already contains multiple real commits that separate the work into research/design, data replay, strategy implementation, execution engine, metrics, tests, and reporting.

Suggested GitHub push:

```bash
git remote add origin git@github.com:Junwen-Zheng/tokyo-equity-execution-lab.git
git branch -M main
git push -u origin main
```

Before applying, run:

```bash
./scripts/run_tests.sh
./scripts/run_demo.sh
git log --oneline --graph --decorate --all
```
