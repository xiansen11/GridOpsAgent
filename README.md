# GridOpsAgent — 电力智能运维 Multi-Agent 平台

---

<img width="2262" height="1827" alt="image" src="https://github.com/user-attachments/assets/cf9929e0-97a2-4a65-998c-33b1377bb1f9" />

---

## 一、项目概述

### 1.1 项目背景

电力系统运维长期面临以下核心挑战：

- **告警量大且分散**：变电站设备每天产生大量告警事件，值班人员需同时操作 SCADA、PMS、巡检系统等多个系统，手动关联告警信息、设备状态和历史工单，效率低且容易遗漏关键线索
- **故障诊断依赖专家经验**：复杂故障的诊断高度依赖资深工程师的个人经验，知识传承困难，新人上手周期长
- **安规查询效率低**：运维人员在现场作业时需要快速查阅安全规程，传统纸质文档和分散的电子文档检索效率低下
- **跨系统数据孤岛**：设备台账、运行指标、告警记录、缺陷工单等数据分散在不同系统中，缺乏统一入口
- **知识文档管理困难**：电力安规、设备手册、故障案例等文档分散存储，缺乏统一的向量化索引和智能检索能力

### 1.2 项目目标

构建一个基于大语言模型（LLM）的智能运维 Agent 平台，实现：

- **意图智能路由**：自动识别用户问题类型，分发到对应子图处理
- **多维度协同诊断**：自适应采集安规、指标、日志、工单等多维证据，综合生成结构化诊断报告
- **知识增强检索**：融合向量检索、关键词检索、知识图谱扩展和重排序，提供精准的知识问答
- **RAG 文档上传与知识库构建**：支持多格式文档上传、智能切片、向量化索引，构建可检索的电力知识库
- **Plan-Execute-Replan 动态规划**：诊断子图内置重规划机制，证据不足时自动补充调查
- **安全可控**：RBAC 权限控制、高风险操作审批、安全审查、全链路审计

### 1.3 应用场景

| 场景 | 描述 | 典型用户 |
|------|------|----------|
| 电力安规问答 | 运维人员查询安全规程、作业标准、操作要求 | 运维人员、安全员 |
| 设备状态查询 | 实时查看变压器油温、开关柜局放等运行指标 | 值班人员、调度员 |
| 告警分析 | 接收告警事件，分析原因和影响范围 | 值班人员 |
| 故障诊断 | 综合多维度证据，生成结构化诊断报告和行动建议 | 技术专家、运维主管 |
| 知识库管理 | 上传电力文档、构建向量索引、版本管理 | 知识管理员 |
| 知识库检索 | 检索设备手册、故障案例、专家经验文档 | 全体运维人员 |
| 通用对话 | 日常运维咨询、操作指导 | 全体运维人员 |

### 1.4 核心价值主张

- **降本增效**：将故障诊断平均耗时从小时级缩短至分钟级，减少人工跨系统查询时间
- **知识沉淀**：将专家经验编码为 Skill 和知识库，实现知识复用和传承
- **风险可控**：高风险操作强制人工审批，全流程审计可追溯，杜绝安全隐患
- **灰度演进**：新旧编排双轨运行，支持配置级切换和异常自动降级，保障系统稳定性

---

## 二、技术架构

### 2.1 技术栈总览

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | LTS 版本，项目使用 record、sealed class 等特性 |
| 框架 | Spring Boot | 3.2.0 | 基础框架，提供自动配置、Web、数据访问等能力 |
| AI 框架 | Spring AI Alibaba | 1.1.0.0-RC2 | DashScope 大模型接入 + Graph 工作流编排 + Agent 框架 |
| 大模型 | 通义千问 (Qwen) | qwen3-max | 阿里云 DashScope API，用于对话、路由、诊断等 |
| 向量数据库 | Milvus | 2.6.10 (SDK) / v2.5.10 (Server) | 文档向量存储与相似度检索 |
| 关系数据库 | MySQL | 8.x | 业务数据持久化（告警、工单、日志、检查点、知识库等） |
| ORM | MyBatis-Plus | 3.5.5 | 数据访问层，提供通用 CRUD 和条件构造器 |
| 缓存 | Redis | 6.x+ | 会话缓存、向量缓存 |
| 构建 | Maven | 3.9+ | 项目构建与依赖管理 |
| 文档解析 | Apache POI / PDFBox / Jsoup | 5.2.5 / 3.0.1 / 1.17.2 | Word、Excel、PDF、HTML 文档解析 |
| 向量模型 | DashScope text-embedding-v4 | - | 文本向量化，1024 维 |
| MCP 协议 | Spring AI MCP Client | - | Model Context Protocol 客户端（WebFlux） |

### 2.2 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                       Controller 层                          │
│     ChatController / AlarmController / KnowledgeController   │
├─────────────────────────────────────────────────────────────┤
│                     编排引擎（双轨）                          │
│  ┌───────────────────────┐  ┌─────────────────────────────┐ │
│  │   Graph 引擎 (新)      │  │  AgentOrchestration (旧)    │ │
│  │   StateGraph 显式编排   │  │  中心化编排                 │ │
│  │   powerops.graph.enabled│  │  fallback 自动降级          │ │
│  └───────────────────────┘  └─────────────────────────────┘ │
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
│                       Tool 层（8 个工具）                      │
│  PowerDeviceStatusTools / PowerAlarmHistoryTools /           │
│  PowerSafetyRulesTools / PowerDefectTicketTools /            │
│  PowerDeviceLogsTools / PowerDeviceProfileTools /            │
│  InternalDocsTools / DateTimeTools                           │
├─────────────────────────────────────────────────────────────┤
│                     数据持久层                                │
│  MySQL (MyBatis-Plus) / Milvus / Redis                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、Agent 架构

### 3.1 Agent 模式分析

| Agent | 模式 | 说明 |
|-------|------|------|
| RouterAgent | 纯 LLM 分类 | 无工具，temperature=0.1，仅做意图分类 |
| ToolAgent | ReAct | ReactAgent + 全部 8 个工具，根据问题自主选择工具调用 |
| AnalysisAgent | ReAct | ReactAgent + 全部 8 个工具，多维度数据分析（替代原 MapReduce 并行子 Agent） |
| DiagnosisAgent | ReAct（增强） | ReactAgent + 全部 8 个工具，综合诊断 + 风险自复核 |
| RiskReviewAgent | ReAct | ReactAgent + 全部 8 个工具，风险评估 + 行动建议 |

### 3.2 图级别模式

| 子图 | 模式 | 关键特征 |
|------|------|---------|
| KnowledgeQA 子图 | Pipeline | 6 节点线性流水线，无循环 |
| Diagnosis 子图 | Plan-Execute-Replan | 自适应证据采集 + 诊断 + 风险评估 + replanner 回环（max=3） |
| Chat 子图 | 单节点 | 通用对话 |

---

## 四、Graph 工作流架构

### 4.1 设计原则

- **主流程稳定可控**：固定场景走预定义子图，路径确定、结果可预期
- **复杂场景局部动态规划**：故障诊断子图内置 Replan 机制
- **Router 优先**：意图路由优先，3 路分发到对应子图
- **Graph 显式表达**：流程通过代码定义节点和边，避免中心化上帝类
- **工具调用可控、证据可追踪、状态可恢复、结果可审计**

### 4.2 主流程拓扑

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
SafetyReviewNode（安全审查 + 审计日志）
  ↓
FinalResponseNode（格式化最终响应）
  ↓
MemorySaveNode（保存对话记忆）
  ↓
END
```

### 4.3 子图设计

#### KnowledgeQA 子图 — 知识问答与设备查询

合并了原 KnowledgeQA + DeviceQuery 两个子图，统一为"信息检索与回答"流程：

```
QueryRewriteNode → RagRetrieveNode → ToolExecuteNode → RerankNode → AnswerGenerateNode → CitationCheckNode
```

| 节点 | 职责 |
|------|------|
| QueryRewriteNode | 查询改写 + 实体提取 + RBAC 权限校验 |
| RagRetrieveNode | Hybrid RAG 检索（向量检索权重 0.7 + BM25 关键词检索权重 0.3 + RRF 合并排序） |
| ToolExecuteNode | ToolAgent 根据意图自主选择工具调用（设备状态/台账/安规等） |
| RerankNode | 对检索结果二次排序 |
| AnswerGenerateNode | 注入 RAG 上下文 + 工具结果 + 知识图谱扩展，生成回答 |
| CitationCheckNode | 验证回答是否基于检索结果，无支撑标注警告 |

#### Diagnosis 子图 — 诊断分析

合并了原 AlarmAnalysis + FaultDiagnosis + DynamicPlan 三个子图，统一为"诊断分析"流程：

```
EntityExtractNode → EvidenceCollectNode → DiagnosisNode → RiskAssessmentNode → ReplannerNode
                                                                                   │
                    ┌────────────────────────────────────────────────────────────┘
                    │
                    ├── REPLAN          → EvidenceCollectNode（证据不足，补充调查，最多 3 次）
                    ├── HUMAN_APPROVAL  → ActionRecommendNode（高风险审批标记）
                    ├── FALLBACK        → ActionRecommendNode（降级标记）
                    └── CONTINUE/END    → ActionRecommendNode（正常继续）
```

| 节点 | 职责 |
|------|------|
| EntityExtractNode | 实体提取（设备 ID + 故障类型 + 告警类型 + 告警等级） |
| EvidenceCollectNode | 自适应证据采集（紧急/重要→AnalysisAgent 全面分析；一般→ToolAgent 轻量查询） |
| DiagnosisNode | 综合诊断推理（含风险自复核，9 项结构化报告） |
| RiskAssessmentNode | RiskReviewAgent 风险评估 + 行动建议 + 安全提示 |
| ReplannerNode | 统一重规划决策（证据不足→REPLAN；高风险→HUMAN_APPROVAL） |
| ActionRecommendNode | 行动建议 + 执行过程摘要 |

**EvidenceCollectNode 自适应策略**：

| 场景 | 策略 | Agent |
|------|------|-------|
| 紧急/重要 或 DIAGNOSIS 意图 | 全面多维度分析 | AnalysisAgent |
| 一般/提示 | 轻量关键数据查询 | ToolAgent |

#### Chat 子图 — 通用对话

```
ChatAgentNode → SafetyReviewNode
```

### 4.4 意图路由

RouterAgent 识别 3 类用户意图并路由到对应子图：

| 意图 | 说明 | 路由目标 |
|------|------|----------|
| KNOWLEDGE_QA | 安规问答 + 设备状态查询 + 设备台账查询 | KnowledgeQA 子图 |
| DIAGNOSIS | 告警分析 + 故障诊断 + 告警诊断 + 复杂任务 | Diagnosis 子图 |
| CHAT | 日志分析 + 工单查询 + 通用对话 + 其他 | Chat 子图 |

### 4.5 StepHandlerRegistry 机制

| Handler | agentType | 职责 |
|---------|-----------|------|
| ToolStepHandler | tool | 调用 ToolAgent 执行工具 |
| AnalysisStepHandler | analysis | 调用 AnalysisAgent 多维度分析 |
| DiagnosisStepHandler | diagnosis | 调用 DiagnosisAgent 综合诊断 |
| RagStepHandler | rag | RAG 检索 |
| ApprovalStepHandler | approval | 人工审批 |
| ChatStepHandler | chat | 通用对话 |

### 4.6 灰度切换与降级

```
Controller 请求
  ├── powerops.graph.enabled=true && CompiledGraph != null
  │   ├── CompiledGraph.invoke() 成功 → 返回结果 (engine: "graph")
  │   └── CompiledGraph.invoke() 异常 → 自动降级到 AgentOrchestrationService
  └── powerops.graph.enabled=false → 直接使用旧编排 (engine: "legacy")
```

---

## 五、主要功能模块

### 5.1 故障诊断模块

**核心 Agent**: DiagnosisAgent + AnalysisAgent + RiskReviewAgent

**诊断流程**:

1. **实体提取**：从用户问题中提取设备 ID、故障类型、告警等级
2. **自适应证据采集**：根据严重程度选择策略
   - 紧急/重要 → AnalysisAgent 全面多维度分析（安规+设备状态+日志+工单+告警历史）
   - 一般 → ToolAgent 轻量查询
3. **综合诊断**：基于所有证据生成 9 项结构化诊断报告（含风险自复核）
4. **风险评估**：RiskReviewAgent 评估风险等级 + 生成行动建议 + 安全提示
5. **重规划决策**：证据不足时自动补充调查（最多 3 次），高风险操作触发人工审批
6. **行动建议**：生成处理建议和执行摘要

**诊断报告格式**:

```
1. 告警摘要
2. 初步判断
3. 分析依据
4. 可能原因（按可能性排序）
5. 排查步骤
6. 处理建议
7. 安全风险提示
8. 是否建议派单
9. 风险自复核（诊断结论支撑度、处理建议安全性、遗漏排查步骤、整体风险等级）
```

### 5.2 RAG 检索增强模块

**检索流程**:

```
用户查询
  ↓
HybridSearchService.hybridSearch()
  ├── 向量检索 (权重 0.7) → VectorSearchService → Milvus / 内存向量存储
  ├── 关键词检索 (权重 0.3) → 内置关键词索引
  └── RRF (Reciprocal Rank Fusion) 合并排序
  ↓
RerankService.rerank()
  ↓
KnowledgeGraphService.buildGraphContext()
  ↓
注入 LLM 上下文生成回答
```

### 5.3 RAG 文档上传与知识库构建模块

**完整上传处理链路**：

```
管理员上传文档 → 文件类型校验 → 保存文件 → 版本管理 → 异步处理:
  PARSING → CLEANING → CHUNKING → 持久化切片 → INDEXING(向量化+写入向量库) → COMPLETED
```

支持的文件格式：txt, md, pdf, docx, xlsx, html

### 5.4 电力工具模块

| 工具类 | 功能 | 数据来源 |
|--------|------|----------|
| PowerDeviceStatusTools | 设备实时运行状态 | SCADA |
| PowerAlarmHistoryTools | 历史告警查询 | SCADA |
| PowerSafetyRulesTools | 安规条款查询 | REGULATION |
| PowerDefectTicketTools | 缺陷工单查询 | PMS |
| PowerDeviceLogsTools | 设备运行日志 | DMS |
| PowerDeviceProfileTools | 设备台账信息 | PMS |
| InternalDocsTools | 内部文档检索 | RAG |
| DateTimeTools | 日期时间 | SYSTEM |

所有工具支持 Mock 模式（`power.mock-enabled=true`），无需真实设备连接即可开发测试。

### 5.5 记忆管理模块

四层记忆架构：

| 层级 | 存储 | TTL | 用途 |
|------|------|-----|------|
| Session（会话） | ConcurrentHashMap | 2 小时 | 当前对话上下文 |
| Task（任务） | ConcurrentHashMap | 24 小时 | 单次诊断任务数据 |
| Domain（领域） | ConcurrentHashMap | 无过期 | 电力领域知识 |
| User（用户） | ConcurrentHashMap | 30 天 | 用户偏好和习惯 |

### 5.6 安全控制模块

**RBAC 权限控制**：

| 角色 | 权限范围 |
|------|----------|
| admin | 全部权限 |
| operator | 对话、诊断、知识检索、告警处理 |
| viewer | 对话、知识检索、指标查询 |

**Hook 引擎**：12 个 Hook 实现，覆盖 4 个拦截点（PRE_ROUTE → POST_ROUTE → PRE_DIAGNOSIS → POST_DIAGNOSIS）及 PRE_RAG、POST_RAG、PRE_TOOL_USE、POST_TOOL_USE 等。

| Hook | 注册点 | 职责 |
|------|--------|------|
| AuditHook | 所有 Hook 点 | 审计日志记录 |
| SafetyCheckHook | PRE_TOOL_USE, POST_DIAGNOSIS | 安全检查 |
| DataMaskingHook | POST_TOOL_USE | 数据脱敏 |
| HumanApprovalHook | POST_DIAGNOSIS | 人工审批拦截 |
| PreRouteHook / PostRouteHook | PRE_ROUTE / POST_ROUTE | 路由前后处理 |
| PreRagHook / PostRagHook | PRE_RAG / POST_RAG | RAG 检索前后处理 |
| PreToolUseHook / PostToolUseHook | PRE_TOOL_USE / POST_TOOL_USE | 工具调用前后处理 |
| PreDiagnosisHook / PostDiagnosisHook | PRE_DIAGNOSIS / POST_DIAGNOSIS | 诊断前后处理 |

### 5.7 Skill 管理模块

内置 5 个 Skill，为 Agent 提供业务场景指导：

| Skill ID | 名称 | 类别 |
|----------|------|------|
| transformer-oil-temp-diagnosis | 主变油温异常诊断 | fault_diagnosis |
| switchgear-pd-diagnosis | 开关柜局放异常诊断 | fault_diagnosis |
| safety-regulation-qa | 安规条款查询 | knowledge_qa |
| defect-ticket-check | 缺陷工单检查 | ticket_analysis |
| line-trip-repair | 配网线路跳闸抢修 | fault_diagnosis |

---

## 六、项目主要链路

### 6.1 智能对话链路

```
用户输入 "查询1号主变的油温"
  ↓
POST /api/chat { question: "查询1号主变的油温" }
  ↓
PreCheckNode → ContextLoadNode → RouterNode
  ↓ (意图: KNOWLEDGE_QA)
KnowledgeQA 子图
  ├── QueryRewriteNode → { deviceId: "TR-110KV-001", attribute: "oilTemp", permission: true }
  ├── RagRetrieveNode → 混合检索
  ├── ToolExecuteNode → ToolAgent 调用 getDeviceStatus
  ├── RerankNode → 重排序
  ├── AnswerGenerateNode → 生成回答
  └── CitationCheckNode → 引用检查
  ↓
SafetyReviewNode → FinalResponseNode → MemorySaveNode
```

### 6.2 故障诊断链路

```
告警事件触发
  ↓
POST /api/alarm/diagnose { taskId: "TASK-a1b2c3d4" }
  ↓
Diagnosis 子图
  ├── EntityExtractNode → { deviceId: "TR-110KV-001", faultType: "油温异常", alarmLevel: "紧急" }
  ├── EvidenceCollectNode → AnalysisAgent 全面分析（紧急级别）
  │   ├── 安规合规性分析
  │   ├── 设备状态分析
  │   ├── 日志分析
  │   ├── 工单关联分析
  │   └── 告警历史分析
  ├── DiagnosisNode → 9 项结构化诊断报告（含风险自复核）
  ├── RiskAssessmentNode → RiskReviewAgent 风险评估 + 行动建议
  ├── ReplannerNode → next_action=HUMAN_APPROVAL（高风险）
  └── ActionRecommendNode → 行动建议 + 高风险提醒
  ↓
SafetyReviewNode → FinalResponseNode → MemorySaveNode
```

### 6.3 知识问答链路

```
用户输入 "变压器油温高的处理规程"
  ↓
KnowledgeQA 子图
  ├── QueryRewriteNode → "变压器油温高告警 处理规程"
  ├── RagRetrieveNode → 混合检索 topK=6 → RRF 合并 → topK=3
  ├── ToolExecuteNode → ToolAgent 调用 searchSafetyRules
  ├── RerankNode → 重排序
  ├── AnswerGenerateNode → 注入检索上下文 + 工具结果 + GraphRAG 扩展 → 生成回答
  └── CitationCheckNode → 验证引用支撑
```

---

## 七、API 接口

| Controller | 路径前缀 | 核心接口 |
|------------|----------|----------|
| ChatController | `/api/chat` | 智能对话（SSE 流式）、会话管理 |
| AlarmController | `/api/alarm` | 告警接收、SSE 流式诊断、断点恢复 |
| KnowledgeController | `/api/knowledge` | 文档上传/删除/查询、检索测试、版本管理/回滚 |
| ApprovalController | `/api/approval` | 人工审批流程（创建/批准/拒绝/待审批列表） |
| SkillController | `/api/skills` | Skill 技能查询、按意图/告警类型选择 |
| ObservabilityController | `/api/observability` | Trace 追踪、Agent 步骤日志、工具调用日志 |
| ToolSearchController | `/api/tools` | 工具注册表查询、按关键词/意图/标签搜索 |
| EvalController | `/api/eval` | 评估测试运行 |
| MilvusCheckController | `/milvus` | Milvus 健康检查、集合列表 |

---

## 八、安装启动指南

### 8.1 环境要求

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 必须 |
| Maven | 3.9+ | 项目构建 |
| MySQL | 8.x | 业务数据存储 |
| Redis | 6.x+ | 缓存（可选） |
| Milvus | 2.x | 向量数据库（可选，可降级为内存向量存储） |
| DashScope API Key | - | 阿里云大模型 API 密钥（必填） |

### 8.2 配置文件

关键配置项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.ai.dashscope.api-key` | - | **必填**，阿里云 DashScope API 密钥 |
| `powerops.graph.enabled` | true | 是否启用 Graph 工作流引擎（推荐 true） |
| `power.mock-enabled` | true | 工具是否返回模拟数据 |
| `milvus.enabled` | true | 是否使用 Milvus 向量数据库 |
| `rag.top-k` | 3 | RAG 检索返回条数 |
| `document.chunk.max-size` | 800 | 文档分块最大字符数 |
| `document.chunk.overlap` | 100 | 文档分块重叠字符数 |

### 8.3 启动命令

```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run

# 或使用 JAR 包启动
mvn clean package -DskipTests
java -jar target/grid-ops-agent-1.0-SNAPSHOT.jar
```

启动成功后访问：`http://localhost:9900`

### 8.4 一键初始化（Makefile）

```bash
make init      # 一键初始化：启动 Docker → 启动服务 → 上传文档
make up        # 启动 Docker Compose（Milvus）
make start     # 启动 Spring Boot 服务
make upload    # 上传文档到知识库
```

---

## 九、项目结构

```
src/main/java/org/example/
├── agent/                          # Agent 层（5 个 Agent）
│   ├── router/RouterAgent          # 意图路由（纯 LLM 分类）
│   ├── analysis/AnalysisAgent      # 多维度数据分析（ReAct）
│   ├── diagnosis/DiagnosisAgent    # 综合诊断 + 风险自复核（ReAct）
│   ├── tool_agent/ToolAgent        # 工具调用（ReAct）
│   ├── risk/RiskReviewAgent        # 风险评估 + 行动建议（ReAct）
│   ├── skill/                      # Skill 管理
│   │   ├── model/Skill             # 技能模型
│   │   ├── service/SkillRegistry   # 技能注册中心
│   │   └── service/SkillSelector   # 技能选择器
│   └── tool/                       # Agent 工具（8 个）
│       ├── DateTimeTools           # 日期时间
│       ├── InternalDocsTools       # 内部文档检索
│       └── power/                  # 电力业务工具
│           ├── PowerDeviceStatusTools    # 设备状态
│           ├── PowerAlarmHistoryTools    # 告警历史
│           ├── PowerSafetyRulesTools     # 安规查询
│           ├── PowerDefectTicketTools    # 缺陷工单
│           ├── PowerDeviceLogsTools      # 设备日志
│           └── PowerDeviceProfileTools   # 设备台账
├── graph/                          # Graph 工作流层
│   ├── PowerOpsGraphConfig         # 顶层图配置
│   ├── PowerOpsStateFactory        # 状态工厂
│   ├── dispatcher/
│   │   └── IntentDispatcher        # 意图条件边分发器（3 路）
│   ├── handler/                    # StepHandler
│   │   ├── PlanStepHandler         # Handler 接口
│   │   ├── StepHandlerRegistry     # Handler 注册中心
│   │   ├── ToolStepHandler         # 工具执行
│   │   ├── AnalysisStepHandler     # 多维度分析
│   │   ├── DiagnosisStepHandler    # 综合诊断
│   │   ├── RagStepHandler          # RAG 检索
│   │   ├── ApprovalStepHandler     # 审批
│   │   └── ChatStepHandler         # 通用对话
│   ├── model/                      # 数据模型
│   │   ├── PlanStep                # 执行步骤
│   │   └── StepResult              # 执行结果
│   ├── node/                       # 主图节点
│   │   ├── PreCheckNode            # 输入校验
│   │   ├── ContextLoadNode         # 上下文加载
│   │   ├── RouterNode              # 意图路由
│   │   ├── SafetyReviewNode        # 安全审查
│   │   ├── FinalResponseNode       # 响应格式化
│   │   └── MemorySaveNode          # 记忆保存
│   └── subgraph/                   # 子图（3 个）
│       ├── knowledge/              # 知识问答子图（6 节点 Pipeline）
│       ├── diagnosis/              # 诊断子图（6 节点 + Replan）
│       └── chat/                   # 通用对话子图（1 节点）
├── controller/                     # 控制器层（9 个）
├── service/                        # 服务层
│   ├── AgentOrchestrationService   # 旧编排引擎（Legacy）
│   ├── ChatService                 # 对话服务
│   ├── RagService                  # RAG 顶层服务
│   ├── KnowledgeBaseService        # 知识库管理
│   ├── DocumentChunkService        # 文档分块
│   ├── VectorEmbeddingService      # 文本向量化
│   ├── VectorIndexService          # 向量索引管理
│   ├── VectorSearchService         # 向量搜索
│   └── InMemoryVectorStore         # 内存向量存储（Milvus 降级方案）
├── rag/                            # RAG 检索层
│   ├── HybridSearchService         # 混合检索（向量+关键词+知识图谱）
│   ├── RerankService               # 重排序
│   └── KnowledgeGraphService       # 知识图谱
├── memory/MemoryService            # 四层记忆管理
├── hook/                           # Hook 引擎
│   ├── AgentHook                   # Hook 接口
│   ├── HookEngine                  # Hook 执行引擎
│   └── impl/                       # 12 个 Hook 实现
├── checkpoint/CheckpointService    # 检查点服务（断点恢复）
├── security/                       # 安全层
│   ├── RbacService                 # RBAC 权限控制
│   └── ApprovalService             # 审批服务
├── observability/ObservabilityService  # 可观测性
├── plugin/                         # 插件管理
├── prompt/PromptRegistry           # Prompt 模板管理
├── entity/                         # 数据库实体
├── mapper/                         # MyBatis Mapper
├── config/                         # 配置类
└── tool/                           # 工具注册与搜索
```

---

## 十、数据库设计

数据库名：`power_aiops`，核心表：

| 表名 | 用途 |
|------|------|
| chat_session | 对话会话 |
| chat_message | 对话消息（含意图、Agent 名、工具调用 JSON） |
| alarm_task | 告警任务（含诊断结果 JSON、检查点数据 JSON） |
| agent_execution_log | Agent 执行日志（trace 追踪） |
| tool_call_log | 工具调用日志 |
| knowledge_document | 知识文档（含版本管理） |
| knowledge_chunk | 知识分块 |
| knowledge_process_task | 知识处理任务 |
| tool_registry | 工具注册表（含权限等级、风险等级） |
| skill_definition | Skill 技能定义 |
| checkpoint_record | 检查点记录（支持断点恢复） |

---

## 十一、基础设施

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
