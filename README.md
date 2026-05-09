# GridOpsAgent — 电力智能运维 Multi-Agent 平台

---

## 一、项目概述

### 1.1 项目背景

电力系统运维长期面临以下核心挑战：

- **告警量大且分散**：变电站设备每天产生大量告警事件，值班人员需同时操作多个系统，手动关联信息效率低
- **故障诊断依赖专家经验**：复杂故障的诊断高度依赖资深工程师经验，知识传承困难
- **安规查询效率低**：运维人员在现场作业时需要快速查阅安全规程
- **跨系统数据孤岛**：设备台账、运行指标、告警记录等数据分散在不同系统中
- **知识文档管理困难**：电力安规、设备手册、故障案例等文档缺乏统一的向量化索引

### 1.2 项目目标

构建一个基于大语言模型（LLM）的智能运维 Agent 平台，实现：

- **意图智能路由**：自动识别用户问题类型，分发到对应子图处理
- **多维度协同诊断**：Plan-Execute-Replan 动态规划，自适应采集多维证据，生成结构化诊断报告
- **知识增强检索**：融合向量检索、关键词检索、知识图谱扩展和重排序
- **RAG 文档上传与知识库构建**：支持多格式文档上传、智能切片、向量化索引
- **安全可控**：RBAC 权限控制、高风险操作审批、12 个 Hook 全链路拦截、审计日志

### 1.3 应用场景

| 场景 | 描述 | 典型用户 |
|------|------|----------|
| 电力安规问答 | 查询安全规程、作业标准、操作要求 | 运维人员、安全员 |
| 设备状态查询 | 实时查看变压器油温、开关柜局放等运行指标 | 值班人员、调度员 |
| 告警分析 | 接收告警事件，分析原因和影响范围 | 值班人员 |
| 故障诊断 | 综合多维度证据，生成结构化诊断报告 | 技术专家、运维主管 |
| 知识库管理 | 上传电力文档、构建向量索引、版本管理 | 知识管理员 |
| 知识库检索 | 检索设备手册、故障案例、专家经验文档 | 全体运维人员 |
| 通用对话 | 日常运维咨询、操作指导 | 全体运维人员 |

### 1.4 核心价值主张

- **降本增效**：将故障诊断平均耗时从小时级缩短至分钟级
- **知识沉淀**：将专家经验编码为 Skill 和知识库，实现知识复用和传承
- **风险可控**：高风险操作强制人工审批，全流程审计可追溯

---

## 二、技术架构

### 2.1 技术栈总览

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | LTS 版本，使用 record、sealed class 等特性 |
| 框架 | Spring Boot | 3.2.0 | 基础框架 |
| AI 框架 | Spring AI Alibaba | 1.1.0.0-RC2 | DashScope 大模型接入 + Graph 工作流编排 |
| 大模型 | 通义千问 (Qwen) | qwen3-max | 阿里云 DashScope API |
| 可靠性 | Resilience4j | 2.2.0 | 工具调用重试、熔断与超时保护 |
| 观测 | Spring Boot Actuator / Micrometer | 3.2.0 | 健康检查、指标采集、Prometheus 暴露 |
| 向量数据库 | Milvus | 2.6.10 | 文档向量存储与相似度检索 |
| 关系数据库 | MySQL | 8.x | 业务数据持久化 |
| ORM | MyBatis-Plus | 3.5.5 | 数据访问层 |
| 缓存 | Redis | 6.x+ | 会话缓存、向量缓存 |
| 构建 | Maven | 3.9+ | 项目构建与依赖管理 |
| 文档解析 | Apache POI / PDFBox / Jsoup | 5.2.5 / 3.0.1 / 1.17.2 | Word、Excel、PDF、HTML 文档解析 |
| 向量模型 | DashScope text-embedding-v4 | - | 文本向量化，1024 维 |

### 2.2 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                       Controller 层                          │
│     ChatController / AlarmController / KnowledgeController   │
├─────────────────────────────────────────────────────────────┤
│                    Graph 编排引擎                             │
│          StateGraph 显式编排 + CompiledGraph                  │
├─────────────────────────────────────────────────────────────┤
│                       Agent 层（5 个 Agent）                  │
│  RouterAgent / AnalysisAgent / DiagnosisAgent /              │
│  ToolAgent / RiskReviewAgent                                 │
├─────────────────────────────────────────────────────────────┤
│                     基础服务层                                │
│  MemoryService / HookEngine / CheckpointService /            │
│  RbacService / ApprovalService / SkillSelector               │
├─────────────────────────────────────────────────────────────┤
│                  RAG 检索 + 知识库构建层                       │
│  HybridSearchService / RerankService /                       │
│  KnowledgeGraphService / VectorSearchService /               │
│  KnowledgeBaseService / DocumentChunkService /               │
│  VectorEmbeddingService / VectorIndexService                 │
├─────────────────────────────────────────────────────────────┤
│                Tool 层（5 个 MCP + 3 个本地）                  │
│  MCP Power Tools (power-tools-mcp-server) /                  │
│  PowerSafetyRulesTools / InternalDocsTools / DateTimeTools   │
├─────────────────────────────────────────────────────────────┤
│                     数据持久层                                │
│  MySQL (MyBatis-Plus) / Milvus / Redis                       │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 MCP 多模块架构

当前项目已经拆分为 Maven 多模块：

| 模块 | 端口 | 角色 | 说明 |
|------|------|------|------|
| `grid-ops-agent-app` | 9900 | 主应用 / MCP Client | 提供 Chat、Alarm、Knowledge、Graph 编排、RAG、RBAC、Hook、审计等能力 |
| `power-tools-mcp-server` | 9901 | MCP Server | 通过 Streamable HTTP 暴露电力外部系统查询工具 |

主应用作为唯一 MCP Client 连接 `power-tools-mcp-server`。Agent/Graph 仍通过 Spring AI `ToolCallbackProvider` 调用工具，不直接感知 MCP 协议。

工具来源如下：

| 工具 | 来源 | 说明 |
|------|------|------|
| `getDeviceStatus` | MCP Server | 查询设备实时运行状态 |
| `getAlarmHistory` | MCP Server | 查询历史告警 |
| `getDeviceLogs` | MCP Server | 查询设备运行日志 |
| `getDefectTickets` | MCP Server | 查询缺陷工单 |
| `getDeviceProfile` | MCP Server | 查询设备台账 |
| `getCurrentDateTime` | 主应用本地工具 | 获取当前时间 |
| `queryInternalDocs` | 主应用本地工具 | 查询内部知识库 |
| `searchSafetyRules` | 主应用本地工具 | 查询安规条款 |

启动顺序必须先启动 MCP 工具服务，再启动主应用：

```bash
mvn -pl power-tools-mcp-server spring-boot:run
mvn -pl grid-ops-agent-app spring-boot:run
```

---

## 三、Agent 架构

### 3.1 Agent 模式分析

| Agent | 模式 | Temperature | MaxToken | 说明 |
|-------|------|-------------|----------|------|
| RouterAgent | 纯 LLM 分类 | 0.1 | 200 | 无工具，仅做意图分类，返回 JSON |
| ToolAgent | ReAct | 0.1 | 2000 | ReactAgent + 统一 `ToolCallbackProvider`（5 个 MCP 工具 + 3 个本地工具） |
| AnalysisAgent | ReAct | 0.3 | 3000 | ReactAgent + 统一 `ToolCallbackProvider`，多维度数据分析 |
| DiagnosisAgent | ReAct（增强） | 0.3 | 3000 | ReactAgent + 统一 `ToolCallbackProvider`，9 项结构化诊断报告 + JSON 摘要 |
| RiskReviewAgent | ReAct | 0.3 | 3000 | ReactAgent + 统一 `ToolCallbackProvider`，风险评估 + 行动建议 |

### 3.2 图级别模式

| 子图 | 模式 | 节点数 | 关键特征 |
|------|------|--------|---------|
| KnowledgeQA 子图 | RAG + ReAct 融合 + 回答评估循环 | 7 | 查询改写 → 混合检索 → 重排序 → ReAct 问答 → 质量评估（可循环） → 引用校验 |
| Diagnosis 子图 | Plan-Execute-Replan + Evidence Validation | 9 | 实体提取 → RAG 检索 → 计划生成 → 计划执行 → 证据校验 → 诊断 → 风险评估 → 重规划（可循环） → 行动建议 |
| Chat 子图 | RAG 增强对话 | 1 | ChatAgentNode 集成 HybridSearchService |

---

## 四、Graph 工作流架构

### 4.1 主流程拓扑

```
START
  ↓
PreCheckNode（输入校验 + 安全检查）
  ↓
ContextLoadNode（加载 Memory + Skill + History）
  ↓
RouterNode（意图识别路由）
  ↓
IntentConditionalEdge（3 路分发）
  ├── KNOWLEDGE_QA  → KnowledgeQA 子图
  ├── DIAGNOSIS     → Diagnosis 子图
  └── CHAT          → Chat 子图
  ↓
SafetyReviewNode（安全审查 + Hook 执行 + 审计日志）
  ↓
FinalResponseNode（格式化最终响应）
  ↓
MemorySaveNode（保存对话记忆）
  ↓
END
```

### 4.2 子图设计

**KnowledgeQA 子图**（7 节点，含回答评估循环）：
```
QueryRewriteNode → RagRetrieveNode → ToolExecuteNode → RerankNode
     ↓
ReactQaAgentNode → AnswerReviewNode ──→ CitationCheckNode
                       │                      ↓
                       └── NEED_MORE ──→ RagRetrieveNode（重新检索，最多 2 次）
```

| 节点 | 说明 |
|------|------|
| QueryRewriteNode | LLM 驱动查询改写 + 实体提取 + RBAC 权限过滤 |
| RagRetrieveNode | HybridSearchService 混合检索（向量 0.7 + 关键词 0.3） |
| ToolExecuteNode | ToolAgent 工具调用，注入 entities 信息 |
| RerankNode | RerankService 重排序 |
| ReactQaAgentNode | RAG + ReAct + 知识图谱融合问答 |
| AnswerReviewNode | LLM 驱动回答质量评估（ACCEPT / NEED_MORE） |
| CitationCheckNode | 引用来源检查 |

**Diagnosis 子图**（9 节点，含证据校验与重规划循环）：
```
EntityExtractNode → AlarmRagRetrieveNode → PlannerNode → ExecutorNode
     ↓
EvidenceValidationNode → DiagnosisNode → RiskAssessmentNode → ReplannerNode ──→ ActionRecommendNode
          │                                                        │
          └── REPLAN / FALLBACK ───────────────────────────────────┘
```

| 节点 | 说明 |
|------|------|
| EntityExtractNode | LLM 驱动 NER（设备编号、告警类型、告警等级等），有降级规则 |
| AlarmRagRetrieveNode | 告警相关 RAG 检索，利用 entities 增强查询 |
| PlannerNode | LLM 生成标准化 `PlanStep`，并通过 `PlanValidator` 修复或回退默认模板 |
| ExecutorNode | 按 `tool_name + params` 精确调用 `ToolCallback`，通过 Resilience4j 重试并输出 `StepResult` |
| EvidenceValidationNode | 基于实时状态、告警、日志、工单、台账、安规等证据覆盖度评分 |
| DiagnosisNode | DiagnosisAgent 综合诊断，输出结构化报告并写入 `diagnosis_result` |
| RiskAssessmentNode | RiskReviewAgent 风险评估，优先 JSON 解析，降级用关键词匹配 |
| ReplannerNode | LLM 驱动重规划（CONTINUE / REPLAN / FALLBACK），有启发式降级 |
| ActionRecommendNode | 基于 step_results 生成行动建议和执行摘要 |

**Chat 子图**（1 节点）：
```
ChatAgentNode（集成 HybridSearchService RAG 检索增强）
```

### 4.3 意图路由

| 意图 | 说明 | 路由目标 |
|------|------|----------|
| KNOWLEDGE_QA | 安规问答 + 设备状态查询 + 设备台账查询 | KnowledgeQA 子图 |
| DIAGNOSIS | 告警分析 + 故障诊断 + 复杂任务 | Diagnosis 子图 |
| CHAT | 日志分析 + 工单查询 + 通用对话 | Chat 子图 |

### 4.4 状态管理

PowerOpsStateFactory 定义 35 个状态键，使用 `ReplaceStrategy`（覆盖）和 `AppendStrategy`（追加，如 `step_results`、`validation_warnings`）。状态键统一由 `GraphStateKeys` 管理，节点读取可通过 `PowerOpsStateView` 适配，避免 snake_case / camelCase 混用：

| 状态键 | 策略 | 写入节点 | 读取节点 |
|--------|------|---------|---------|
| `input` | Replace | PreCheckNode | 全局 |
| `cleaned_input` | Replace | PreCheckNode | Router / Planner / Diagnosis |
| `trace_id` | Replace | PreCheckNode / ObservedNodeAction | ObservabilityService |
| `intent` | Replace | RouterNode | IntentDispatcher |
| `entities` | Replace | QueryRewriteNode / EntityExtractNode | ToolExecuteNode / AlarmRagRetrieveNode |
| `rag_results` | Replace | RagRetrieveNode / AlarmRagRetrieveNode | RerankNode / ReactQaAgentNode |
| `step_results` | **Append** | ExecutorNode | ActionRecommendNode |
| `plan_steps` | Replace | PlannerNode | ExecutorNode / ReplannerNode |
| `evidence` | Replace | ExecutorNode | DiagnosisNode / ReplannerNode |
| `evidence_score` | Replace | EvidenceValidationNode | DiagnosisNode / EvalService |
| `evidence_coverage` | Replace | EvidenceValidationNode | DiagnosisNode / EvalService |
| `evidence_warnings` | Replace | EvidenceValidationNode | FinalResponse / Observability |
| `diagnosis_result` | Replace | DiagnosisNode | RiskAssessmentNode |
| `risk_level` | Replace | RiskAssessmentNode | ReplannerNode |
| `next_action` | Replace | EvidenceValidationNode / ReplannerNode | ConditionalEdge |
| `final_response` | Replace | FinalResponseNode | Controller |
| `loop_count` | Replace | ReplannerNode / AnswerReviewNode | ReplannerNode / AnswerReviewDispatcher |
| `review_decision` | Replace | AnswerReviewNode | AnswerReviewDispatcher |

### 4.5 任务拆分、校验、重试与评估

诊断任务采用 `DiagnosisTask → TaskPlan → PlanStep[] → StepResult[]` 的结构。`PlanStep` 包含 `step_id`、`step_no`、`step_type`、`tool_name`、`params`、`expected`、`status`、`retry_count`、`required` 等字段；`StepResult` 记录工具名、状态、结果、错误类型、是否可恢复、证据类型和耗时。

关键校验链路：

| 校验器 / 节点 | 作用 |
|---------------|------|
| `InputValidator` | 输入清洗、长度截断、危险片段过滤，补齐 `task_id/session_id/trace_id` |
| `PlanValidator` | 标准化计划步骤、修复工具别名、限制步骤数量，必要时回退默认诊断模板 |
| `ToolResultValidator` | 校验工具返回非空、JSON 合法性、错误字段和 MCP 工具业务字段 |
| `EvidenceQualityEvaluator` | 按设备台账、实时状态、历史告警、运行日志、缺陷工单、安规计算证据覆盖分 |
| `DiagnosisValidator` | 检查诊断报告是否包含告警摘要、关键证据、原因、风险、建议、安全说明等内容 |

失败处理分为两层：工程失败由 Resilience4j 对 MCP 工具、LLM、向量检索等外部依赖提供 retry/circuit breaker；业务失败由 `EvidenceValidationNode` 和 `ReplannerNode` 决定 `CONTINUE`、`REPLAN`、`FALLBACK` 或进入高风险人工审批。Graph 节点统一由 `ObservedNodeAction` 包装，自动写入执行日志并在关键节点保存 checkpoint。

---

## 五、主要功能模块

### 5.1 故障诊断模块

**诊断流程**：
1. **实体提取**：LLM 驱动 NER，从用户问题中提取设备 ID、故障类型、告警等级
2. **告警 RAG 检索**：利用实体信息增强检索查询，获取相关历史案例和规程
3. **计划生成**：LLM 生成结构化排查计划（步骤 + 工具 + 参数 + 预期结果）
4. **计划执行**：按计划逐步执行工具调用，收集证据
5. **综合诊断**：基于所有证据生成 9 项结构化诊断报告 + JSON 摘要
6. **风险评估**：RiskReviewAgent 评估风险等级 + 生成行动建议
7. **重规划决策**：LLM 判断证据是否充分，不足时自动补充调查（最多 3 次）
8. **行动建议**：基于 step_results 生成处理建议和执行摘要

### 5.2 RAG 检索增强模块

**检索流程**：
```
用户查询 → HybridSearchService.hybridSearch()
  ├── 向量检索 (权重 0.7) → Milvus / InMemoryVectorStore
  ├── 关键词检索 (权重 0.3) → 内置关键词索引
  └── RRF 合并排序 (公式: score = weight / (rank + 61))
  ↓
RerankService.rerank() 重排序
  ↓
KnowledgeGraphService.buildGraphContext() 知识图谱扩展
  ↓
注入 LLM 上下文生成回答
```

**知识库构建流程**：
```
文档上传 → 文件校验 → 保存磁盘 → 版本管理
  ↓（异步处理管线）
DocumentParser 解析 → 文本清洗 → 智能切片(max_size=800, overlap=100)
  → 向量化(text-embedding-v4, 1024维) → 写入 Milvus / InMemoryVectorStore
```

支持格式：Word(.docx)、Excel(.xlsx)、PDF、HTML、TXT/MD

### 5.3 Power Tools and MCP Split

Tools are split into local tools in the main application and remote power-system tools exposed by the MCP server. The main application connects to `power-tools-mcp-server` through Spring AI MCP Client and merges remote MCP tools into the unified `ToolCallbackProvider`.

| Tool | Source | Parameters | Backend |
|------|--------|------------|---------|
| getDeviceStatus | MCP Server (`power-tools-mcp-server`) | deviceId, metrics, timeRange | SCADA / monitoring |
| getAlarmHistory | MCP Server (`power-tools-mcp-server`) | deviceId, alarmType, timeRange, limit | SCADA / alarm system |
| getDeviceLogs | MCP Server (`power-tools-mcp-server`) | deviceId, timeRange, keywords | DMS / log system |
| getDefectTickets | MCP Server (`power-tools-mcp-server`) | deviceId, defectType, timeRange | PMS / ticket system |
| getDeviceProfile | MCP Server (`power-tools-mcp-server`) | deviceId | PMS / asset registry |
| searchSafetyRules | Main app local tool | query, ruleType | REGULATION |
| queryInternalDocs | Main app local tool | query | RAG |
| getCurrentDateTime | Main app local tool | none | SYSTEM |

`power-tools-mcp-server` supports `power.mock-enabled=true` mock mode. When real SCADA/PMS/DMS integrations are added later, replace implementations in the MCP server module first; the main orchestration app should not need business-flow changes.

### 5.4 安全控制模块

**RBAC 权限控制**：

| 角色 | 权限范围 |
|------|----------|
| admin | 全部权限（含 knowledge:upload/delete、skill:manage、system:config、approval:approve） |
| operator | 对话、诊断、知识检索、告警处理、工单创建 |
| viewer | 对话、知识检索、指标查询 |

**工具权限映射**：

| 工具 | 所需权限 |
|------|----------|
| getDeviceStatus / getAlarmHistory / getDeviceLogs / getDefectTickets / searchSafetyRules / getDeviceProfile / queryInternalDocs | QUERY |
| createEmergencyTicket | TICKET_CREATE |
| shutdownDevice | DEVICE_CONTROL |
| reduceLoad | LOAD_CONTROL |

**Hook 引擎**（12 个 Hook 实现，8 个拦截点）：

| 拦截点 | Hook 链 | 功能 |
|--------|---------|------|
| PRE_ROUTE | AuditHook → PreRouteHook | 审计日志 → 输入校验/XSS 过滤/长度截断 |
| POST_ROUTE | AuditHook → PostRouteHook | 审计日志 → 意图合法性校验 |
| PRE_RAG | AuditHook → PreRagHook | 审计日志 → 查询扩展（油温→变压器等） |
| POST_RAG | AuditHook → PostRagHook | 审计日志 → RAG 质量标记 |
| PRE_TOOL_USE | AuditHook → PreToolUseHook → SafetyCheckHook | 审计日志 → RBAC 权限检查 → 安全关键词检测 |
| POST_TOOL_USE | AuditHook → PostToolUseHook → DataMaskingHook | 审计日志 → 敏感数据脱敏 → 数据脱敏 |
| PRE_DIAGNOSIS | AuditHook → PreDiagnosisHook | 审计日志 → 数据不足警告 |
| POST_DIAGNOSIS | AuditHook → PostDiagnosisHook → SafetyCheckHook → HumanApprovalHook | 审计日志 → 高风险标记 → 安全检测 → 人工审批 |

---

## 六、项目结构

```
grid-ops-agent-app/src/main/java/org/example/
├── agent/                              # Agent 层（5 个 Agent）
│   ├── router/RouterAgent              # 意图路由（纯 LLM 分类）
│   ├── analysis/AnalysisAgent          # 多维度数据分析（ReAct）
│   ├── diagnosis/DiagnosisAgent        # 综合诊断（ReAct 增强）
│   ├── tool_agent/ToolAgent            # 工具调用（ReAct）
│   ├── risk/RiskReviewAgent            # 风险评估（ReAct）
│   ├── skill/                          # Skill 管理
│   │   ├── model/Skill                 # 技能定义模型
│   │   └── service/SkillRegistry       # 技能注册中心 + SkillSelector
│   └── tool/                           # Agent 工具源码（主应用注册 3 个本地工具）
│       ├── DateTimeTools               # 日期时间
│       ├── InternalDocsTools           # 知识库检索
│       └── power/                      # 电力工具源码（searchSafetyRules 本地注册，其他 5 个已迁移 MCP）
├── graph/                              # Graph 工作流层
│   ├── PowerOpsGraphConfig             # 顶层图配置（Bean 定义）
│   ├── PowerOpsStateFactory            # 状态工厂（35 个状态键）
│   ├── GraphStateKeys                  # Graph 状态键常量
│   ├── PowerOpsStateView               # OverAllState 类型化读取适配器
│   ├── dispatcher/                     # 条件边分发器
│   │   ├── IntentDispatcher            # 意图条件边分发器
│   │   └── EvidenceValidationDispatcher # 证据校验后的诊断/重规划/降级分发
│   ├── handler/                        # StepHandler（7 个）
│   ├── model/                          # 数据模型（DiagnosisTask / TaskPlan / PlanStep / StepResult）
│   ├── validation/                     # 输入、计划、工具结果、证据、诊断校验
│   ├── node/                           # 主图节点（含 ObservedNodeAction 包装器）
│   │   ├── PreCheckNode                # 前置检查
│   │   ├── ContextLoadNode             # 上下文加载
│   │   ├── RouterNode                  # 意图路由
│   │   ├── ObservedNodeAction          # 节点观测 + checkpoint 包装器
│   │   ├── SafetyReviewNode            # 安全审查
│   │   ├── FinalResponseNode           # 最终响应
│   │   └── MemorySaveNode              # 记忆保存
│   └── subgraph/                       # 子图（3 个）
│       ├── knowledge/                  # KnowledgeQA 子图（7 节点）
│       ├── diagnosis/                  # Diagnosis 子图（含证据校验节点）
│       └── chat/                       # Chat 子图（1 节点）
├── controller/                         # 控制器层（9 个）
├── service/                            # 服务层
│   ├── RagService                      # RAG 流式检索
│   ├── KnowledgeBaseService            # 知识库管理
│   ├── VectorSearchService             # 向量检索
│   ├── VectorEmbeddingService          # 向量化
│   ├── VectorIndexService              # 向量索引管理
│   ├── DocumentChunkService            # 文档切片
│   ├── InMemoryVectorStore             # 内存向量存储
│   └── parser/                         # 文档解析器（5 种格式）
├── rag/                                # RAG 检索层
│   ├── HybridSearchService             # 混合检索（向量 + 关键词 RRF）
│   ├── RerankService                   # 重排序
│   └── KnowledgeGraphService           # 知识图谱扩展
├── memory/MemoryService                # 四层记忆管理
├── hook/                               # Hook 引擎
│   ├── HookEngine                      # Hook 执行引擎
│   ├── HookContext / HookResult        # 上下文与结果
│   └── impl/                           # 12 个 Hook 实现
├── checkpoint/CheckpointService        # 检查点服务
├── security/                           # 安全层
│   ├── RbacService                     # RBAC 权限控制
│   └── ApprovalService                 # 人工审批
├── observability/ObservabilityService  # trace/span、Agent 日志、工具调用日志
├── plugin/                             # 插件管理
├── prompt/PromptRegistry               # Prompt 模板管理
├── eval/                               # 评测框架
├── entity/                             # 数据库实体（10 个）
├── mapper/                             # MyBatis Mapper（10 个）
├── config/                             # 配置类（10 个）
├── tool/                               # 工具注册与搜索
└── client/MilvusClientFactory          # Milvus 客户端工厂
```

---

## 七、API 接口

### 7.1 对话接口 — ChatController (`/api/chat`)

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| chat | POST | `/api/chat` | 同步对话（Graph 引擎） |
| chatStream | POST | `/api/chat/stream` | SSE 流式对话（RAG 检索 + DashScope 流式生成） |
| clearSession | POST | `/api/chat/clear` | 清空会话 |
| getHistory | GET | `/api/chat/history` | 获取对话历史 |
| getSessions | GET | `/api/chat/sessions` | 获取会话列表 |

### 7.2 告警接口 — AlarmController (`/api/alarm`)

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| receiveAlarm | POST | `/api/alarm/receive` | 接收告警事件，创建 AlarmTask |
| diagnoseAlarm | POST | `/api/alarm/diagnose` | SSE 流式告警诊断（Graph 引擎） |
| getDiagnosisStatus | GET | `/api/alarm/diagnose/{taskId}/status` | 查询诊断状态 |
| resumeFromCheckpoint | POST | `/api/alarm/resume/{taskId}` | 从最新 checkpoint 恢复 Graph state，不再依赖 question 字段 |
| getCheckpoints | GET | `/api/alarm/checkpoint/{taskId}` | 获取检查点列表 |

### 7.3 知识库接口 — KnowledgeController (`/api/knowledge`)

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| uploadDocument | POST | `/api/knowledge/documents/upload` | 上传文档（异步处理管线） |
| getDocumentStatus | GET | `/api/knowledge/documents/{documentId}/status` | 查询文档处理状态 |
| listDocuments | GET | `/api/knowledge/documents` | 文档列表 |
| deleteDocument | DELETE | `/api/knowledge/documents/{documentId}` | 删除文档 |
| searchTest | POST | `/api/knowledge/search/test` | 搜索测试 |
| getDocumentVersions | GET | `/api/knowledge/documents/{documentName}/versions` | 文档版本列表 |
| rollbackDocument | POST | `/api/knowledge/documents/{documentId}/rollback` | 文档版本回滚 |

### 7.4 其他接口

| Controller | 路径前缀 | 核心接口 |
|------------|----------|----------|
| ApprovalController | `/api/approval` | 创建审批请求、审批通过/拒绝、待审批列表 |
| SkillController | `/api/skills` | 技能列表、技能详情、技能选择 |
| ObservabilityController | `/api/observability` | 链路追踪、节点耗时、工具调用、失败步骤、重规划次数 |
| ToolSearchController | `/api/tools` | 工具列表、关键词搜索、意图搜索、标签搜索、高风险工具 |
| EvalController | `/api/eval` | 运行评测，返回意图、工具选择、工具成功率、证据覆盖率、耗时、fallback 等指标 |
| MilvusCheckController | `/milvus` | Milvus 健康检查 |

---

## 八、安装启动指南

### 8.1 环境要求

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 必须 |
| Maven | 3.9+ | 项目构建 |
| MySQL | 8.x | 业务数据存储 |
| Redis | 6.x+ | 缓存（可选） |
| Milvus | 2.x | 向量数据库（可选，不启用时使用 InMemoryVectorStore） |
| DashScope API Key | - | 阿里云大模型 API 密钥（必填） |

### 8.2 Configuration

Key configuration items:

| Property | Default | Module | Description |
|----------|---------|--------|-------------|
| `spring.ai.dashscope.api-key` | - | main app | DashScope API key |
| `spring.ai.mcp.client.streamable-http.connections.power-tools.url` | `http://localhost:9901` | main app | MCP server URL used by the main app |
| `spring.ai.mcp.server.name` | `grid-ops-power-tools-mcp-server` | MCP server | MCP server name |
| `power.mock-enabled` | true | MCP server / main app | Whether tools return mock data |
| `milvus.enabled` | true | main app | Whether to use Milvus vector storage |
| `rag.top-k` | 3 | main app | Number of RAG results |
| `rag.model` | qwen3-max | main app | RAG generation model |
| `dashscope.embedding.model` | text-embedding-v4 | main app | Embedding model |
| `document.chunk.max-size` | 800 | main app | Max document chunk size |
| `document.chunk.overlap` | 100 | main app | Document chunk overlap |
| `resilience4j.retry.instances.mcpTool.maxAttempts` | 3 | main app | MCP 工具调用最大重试次数 |
| `resilience4j.retry.instances.llmCall.maxAttempts` | 2 | main app | LLM 调用最大重试次数 |
| `management.endpoints.web.exposure.include` | health,info,metrics,prometheus | main app | Actuator / Micrometer 指标端点 |

### 8.3 启动命令

```bash
# 编译项目
mvn clean compile

# 启动 MCP 工具服务（端口 9901）
mvn -pl power-tools-mcp-server spring-boot:run

# 启动主应用（端口 9900，作为 MCP Client 连接工具服务）
mvn -pl grid-ops-agent-app spring-boot:run

# 或使用 JAR 包启动
mvn clean package -DskipTests
java -jar power-tools-mcp-server/target/power-tools-mcp-server-1.0-SNAPSHOT.jar
java -jar grid-ops-agent-app/target/grid-ops-agent-1.0-SNAPSHOT.jar
```

启动成功后访问主应用：`http://localhost:9900`。MCP 工具服务监听：`http://localhost:9901`。

### 8.4 Makefile

```bash
make init      # Initialize: start Docker -> start MCP server and main app -> upload documents
make up        # Start Docker Compose (Milvus)
make start     # Start power-tools-mcp-server first, then grid-ops-agent-app
make upload    # Upload documents to the knowledge base
make down      # Stop Docker Compose
make status    # Show service status
make check     # Health-check the main app
make stop      # Stop the main app and MCP server
```

`make start` writes `mcp-server.log` for the MCP server and `server.log` for the main app.

---

## 九、数据库设计

数据库名：`power_aiops`，核心表：

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| chat_session | 对话会话 | sessionId, userId |
| chat_message | 对话消息 | sessionId, role, content, intent, agentName |
| alarm_task | 告警任务 | taskId, alarmId, deviceId, alarmLevel, status, diagnosisResult |
| agent_execution_log | Agent 执行日志 | traceId, spanId, parentSpanId, nodeName, agentName, stepName, status, durationMs |
| tool_call_log | 工具调用日志 | traceId, spanId, toolName, toolSource, requestParam, responseData, status, retryCount |
| knowledge_document | 知识文档 | documentId, documentType, status, version, enabled |
| knowledge_chunk | 知识分块 | chunkId, documentId, chunkIndex, content, tokenCount |
| knowledge_process_task | 文档处理任务 | taskId, documentId, taskType, status, currentStep |
| tool_registry | 工具注册表 | toolName, permissionLevel, riskLevel, enabled |
| checkpoint_record | 检查点记录 | taskId, stepName, agentState, planSteps, completedSteps, diagnosisDraft |

---

## 十、基础设施

### Docker Compose

项目提供 `docker-compose.yml` 和 `vector-database.yml`，一键启动向量数据库基础设施：

| 服务 | 版本 | 端口 | 说明 |
|------|------|------|------|
| Milvus | v2.5.10 | 19530 | 向量数据库 |
| etcd | v3.5.18 | 2379 | Milvus 元数据存储 |
| MinIO | latest | 9000 / 9001 | Milvus 对象存储 |
| Attu | v2.5 | 8000 | Milvus 可视化管理界面 |

---

## License

MIT
