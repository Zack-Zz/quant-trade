## 项目基本结构

```
quant-stack/
  README.md
  docs/
    quant_trading_design_doc.md
    api/
      signal-schema.json
  contracts/
    signal/                 # 协议中心
      signal.schema.json
      examples/
        etf_trend_v1.json
        stock_momentum_v1.json
      openapi.yaml          # Python服务对外API（可选）
      java/                 # 自动/手写生成的DTO（可选）
      python/               # Pydantic模型（可选）

  quant-research/           # Python：研究/回测/纸交易/信号服务
    pyproject.toml
    src/quant_research/...
    tests/
    serve/                  # FastAPI app
    scripts/

  trade-executor/           # Java：执行/风控/调度/对账
    pom.xml  (或 build.gradle)
    src/main/java/...
    src/main/resources/
    src/test/java/...

  infra/
    docker-compose.yml      # 可选：postgres, redis, mq
  scripts/
    dev-up.sh               # 一键启动（python+java）
    dev-down.sh
    smoke-test.sh           # 冒烟测试：拉信号→生成订单→paper执行
```
