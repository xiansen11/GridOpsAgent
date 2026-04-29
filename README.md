# GridOpsAgent — 电力智能运维 Multi-Agent 平台

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

- **意图智能路由**：自动识别用户问题类型，分发到对应专业 Agent 处理
- **多 Agent 协同诊断**：并行调度安规、指标、日志、工单等子 Agent，综合生成结构化诊断报告
- **知识增强检索**：融合向量检索、关键词检索、知识图谱扩展和重排序，提供精准的知识问答
- **RAG 文档上传与知识库构建**：支持多格式文档上传、智能切片、向量化索引，构建可检索的电力知识库
- **动态任务编排**：复杂场景支持 Plan-Execute-Replan 动态规划，固定场景走稳定子图
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
| 大模型 | 通义千问 (Qwen) | qwen3-max / qwen3-30b | 阿里云 DashScope API，用于对话、路由、诊断等 |
| 向量数据库 | Milvus | 2.6.10 (SDK) / v2.3.3 (Server) | 文档向量存储与相似度检索 |
| 关系数据库 | MySQL | 8.x | 业务数据持久化（告警、工单、日志、检查点、知识库等） |
| ORM | MyBatis-Plus | 3.5.5 | 数据访问层，提供通用 CRUD 和条件构造器 |
| 缓存 | Redis | 6.x+ | 会话缓存、向量缓存 |
| 构建 | Maven | 3.9+ | 项目构建与依赖管理 |
| 文档解析 | Apache POI / PDFBox / Jsoup | 5.2.5 / 3.0.1 / 1.17.2 | Word、Excel、PDF、HTML 文档解析 |
| 向量模型 | DashScope text-embedding-v4 | - | 文本向量化，1024 维 |
| MCP 协议 | Spring AI MCP Client | - | Model Context Protocol 客户端（WebFlux） |

### 2.2 核心依赖

```xml
<dependencies>
    <!-- Spring AI Alibaba — 大模型接入 + Agent + Graph 编排 -->
    <dependency>spring-ai-alibaba-starter-dashscope</dependency>
    <dependency>spring-ai-alibaba-agent-framework</dependency>
    <dependency>spring-ai-alibaba-graph-core</dependency>

    <!-- 数据存储 -->
    <dependency>milvus-sdk-java</dependency>
    <dependency>mysql-connector-j</dependency>
    <dependency>mybatis-plus-spring-boot3-starter</dependency>
    <dependency>spring-boot-starter-data-redis</dependency>

    <!-- 文档解析 -->
    <dependency>poi-ooxml</dependency>      <!-- Word/Excel -->
    <dependency>pdfbox</dependency>          <!-- PDF -->
    <dependency>jsoup</dependency>           <!-- HTML -->

    <!-- MCP 协议 -->
    <dependency>spring-ai-starter-mcp-client-webflux</dependency>

    <!-- 工具 -->
    <dependency>lombok</dependency>
    <dependency>jsonschema-generator</dependency>
    <dependency>gson</dependency>
</dependencies>
```

### 2.3 架构模式

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
│                       Agent 层                               │
│  RouterAgent / DiagnosisAgent / AlarmAgent /                 │
│  KnowledgeAgent / SubagentExecutor / ChatAgent               │
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
│                       Tool 层                                │
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

## 三、业务需求

### 3.1 核心业务目标

1. **智能意图识别**：准确区分安规问答、设备查询、告警分析、故障诊断等 9 类意图，确保请求精准路由
2. **多源证据融合诊断**：并行采集安规、指标、日志、工单四维证据，综合生成诊断结论，避免遗漏关键线索
3. **知识增强问答**：基于 RAG 检索电力规程、设备手册、历史案例，生成有引用支撑的回答，杜绝幻觉
4. **RAG 知识库构建**：支持多格式文档上传（PDF/Word/Excel/HTML/TXT/Markdown），自动解析、切片、向量化并写入向量库，构建可检索的知识库
5. **告警闭环处理**：从告警接入 → 诊断分析 → 行动建议 → 审批确认的全流程自动化
6. **安全合规保障**：RBAC 权限、高风险审批、安全审查、全链路审计，确保操作可追溯

### 3.2 用户角色

| 角色 | 权限范围 | 典型操作 |
|------|----------|----------|
| **admin** | 全部权限 | 对话、诊断、知识管理（上传/删除/搜索）、告警处理、审批、系统配置、用户管理、设备控制 |
| **operator** | 运维操作权限 | 对话、诊断、知识检索、告警处理、工单创建 |
| **viewer** | 只读权限 | 对话、知识检索、指标查询 |

### 3.3 关键业务流程

- **故障诊断流程**：告警触发 → 意图路由 → 实体提取 → 证据并行采集 → 综合诊断 → 风险评估 → 重规划决策 → 行动建议 → 安全审查
- **知识问答流程**：用户提问 → Query 改写 → 混合检索 → 重排序 → 知识图谱扩展 → 生成回答 → 引用检查
- **知识库构建流程**：文档上传 → 文件类型校验 → 文档解析 → 文本清洗 → 智能切片 → 切片持久化 → 向量化 → 写入向量库 → 完成
- **告警处理流程**：告警接入 → 告警解析 → 历史关联 → 关联设备 → 原因推理 → 处理建议
- **设备查询流程**：用户提问 → 实体提取 → RBAC 权限校验 → 工具选择 → 工具执行 → 数据格式化

---

## 四、系统总体架构

### 4.1 Graph 工作流架构（新编排引擎）

系统采用 Spring AI Alibaba Graph 构建显式有向图工作流，核心设计原则：

- **主流程稳定可控**：固定场景走预定义子图，路径确定、结果可预期
- **复杂场景局部动态规划**：仅在故障诊断和复杂任务中启用 Replan 机制
- **Router 优先**：意图路由优先，Planner 只在复杂任务中启用
- **Graph 显式表达**：流程通过代码定义节点和边，避免中心化上帝类
- **工具调用可控、证据可追踪、状态可恢复、结果可审计**

主流程拓扑：

```
START
  ↓
PreCheckNode（输入校验 + 安全检查）
  ↓
ContextLoadNode（加载 Memory + Skill + History）
  ↓
RouterNode（意图识别路由）
  ↓
IntentConditionalEdge（根据意图分发到子图）
  ├── SAFETY_QA        → KnowledgeQASubGraph
  ├── DEVICE_STATUS    → DeviceQuerySubGraph
  ├── ALARM_QUERY      → AlarmAnalysisSubGraph
  ├── FAULT_DIAGNOSIS  → FaultDiagnosisSubGraph
  ├── ALARM_DIAGNOSIS  → FaultDiagnosisSubGraph
  ├── COMPLEX_TASK     → DynamicPlanSubGraph
  └── GENERAL_CHAT     → ChatSubGraph
  ↓
SafetyReviewNode（安全审查 + 审计日志）
  ↓
FinalResponseNode（格式化最终响应）
  ↓
MemorySaveNode（保存对话记忆）
  ↓
END
```

### 4.2 子图设计

#### KnowledgeQASubGraph — 知识问答子图

```
QueryRewriteNode → RagRetrieveNode → RerankNode → AnswerGenerateNode → CitationCheckNode
```

| 节点 | 职责 |
|------|------|
| QueryRewriteNode | 补全设备类型前缀、消除代词歧义，提升检索召回率 |
| RagRetrieveNode | Hybrid RAG 检索（向量检索权重 0.7 + BM25 关键词检索权重 0.3 + RRF 合并排序） |
| RerankNode | 对检索结果二次排序（术语匹配率 0.4 + 完整查询包含 0.3 + 安规领域匹配 0.2 + 电力设备领域匹配 0.1） |
| AnswerGenerateNode | 注入检索上下文 + GraphRAG 知识图谱扩展 + 生成回答 |
| CitationCheckNode | 验证回答是否基于检索结果，无支撑标注"未经验证" |

#### DeviceQuerySubGraph — 设备查询子图

```
EntityExtractNode → PermissionCheckNode → ToolSelectNode → ToolExecuteNode → DataFormatNode
```

| 节点 | 职责 |
|------|------|
| EntityExtractNode | 提取设备 ID、设备类型、查询属性 |
| PermissionCheckNode | RBAC 校验用户查询权限 |
| ToolSelectNode | 根据查询属性选择对应工具 |
| ToolExecuteNode | 调用工具获取数据 |
| DataFormatNode | 格式化为用户友好描述 |

#### AlarmAnalysisSubGraph — 告警分析子图

```
AlarmParseNode → AlarmHistoryNode → RelatedDeviceNode → AlarmReasoningNode → AlarmSuggestionNode
```

| 节点 | 职责 |
|------|------|
| AlarmParseNode | 解析告警事件，提取告警类型、等级、设备信息 |
| AlarmHistoryNode | 查询该设备的历史告警记录 |
| RelatedDeviceNode | 查询关联设备信息 |
| AlarmReasoningNode | 基于告警上下文推理可能原因 |
| AlarmSuggestionNode | 生成处理建议 |

#### FaultDiagnosisSubGraph — 故障诊断子图（核心）

采用**固定诊断骨架 + 局部动态 Replan** 设计：

```
DiagnosisEntityExtractNode
  ↓
EvidenceParallelNode（并行证据收集 — 固定骨架）
  ├── regulation（安规查询子 Agent）
  ├── metrics（实时指标子 Agent）
  ├── log（日志分析子 Agent）
  └── ticket（工单历史子 Agent）
  ↓
DiagnosisNode（综合诊断）
  ↓
RiskAssessmentNode（风险评估：LOW / MEDIUM / HIGH / CRITICAL）
  ↓
DiagnosisReplannerNode（重规划决策）
  ├── CONTINUE      → ActionRecommendNode（证据充分，继续）
  ├── REPLAN        → EvidenceParallelNode（证据不足，补充调查，最多 2 次）
  ├── END           → ActionRecommendNode
  ├── FALLBACK      → ActionRecommendNode（降级标记）
  └── HUMAN_APPROVAL → ActionRecommendNode（高风险审批标记）
  ↓
ActionRecommendNode（行动建议 + 安全提示）
```

#### DynamicPlanSubGraph — 动态规划子图

仅用于复杂开放任务、跨工具任务、异常补查任务：

```
PlannerNode（LLM 生成多步骤执行计划）
  ↓
DynamicExecutorNode（StepHandlerRegistry 分发执行）
  ↓
DynamicReplannerNode（重规划决策）
  ├── CONTINUE      → DynamicExecutorNode（继续执行下一步）
  ├── REPLAN        → PlannerNode（重新规划）
  ├── END           → FinalizePlanNode
  ├── FALLBACK      → FinalizePlanNode（降级）
  └── HUMAN_APPROVAL → FinalizePlanNode（需人工审批）
  ↓
FinalizePlanNode（汇总所有步骤结果）
```

### 4.3 StepHandlerRegistry 机制

DynamicExecutorNode 不使用 switch-case，而是通过注册表模式分发，支持扩展：

```java
public interface PlanStepHandler {
    String agentType();
    StepResult execute(PlanStep step, OverAllState state);
}
```

| Handler | agentType | 职责 |
|---------|-----------|------|
| ToolStepHandler | tool | 调用指定电力工具 |
| KnowledgeStepHandler | knowledge | 知识库问答 |
| AlarmStepHandler | alarm | 告警分析 |
| DiagnosisStepHandler | diagnosis | 综合诊断 |
| RagStepHandler | rag | RAG 检索 |
| SubAgentStepHandler | subagents | 并行子 Agent |
| ApprovalStepHandler | approval | 人工审批 |
| ChatStepHandler | chat | 通用对话 |

### 4.4 灰度切换与降级

```
Controller 请求
  ├── powerops.graph.enabled=true && CompiledGraph != null
  │   ├── CompiledGraph.invoke() 成功 → 返回结果 (engine: "graph")
  │   └── CompiledGraph.invoke() 异常 → 自动降级到 AgentOrchestrationService
  └── powerops.graph.enabled=false → 直接使用旧编排 (engine: "legacy")
```

### 4.5 模块交互关系

```
┌──────────┐    ┌──────────────────┐    ┌──────────────┐
│  前端 UI   │───→│  ChatController   │───→│ Graph 引擎    │
│ index.html │    │  AlarmController │    │ (StateGraph)  │
│ admin.html │    │KnowledgeController│    └──────┬───────┘
└──────────┘    └──────────────────┘           │
                       │                        │
              ┌────────┤              ┌─────────┤
              │        │              │         │
        ┌─────▼─────┐  │       ┌──────▼──────┐  │
        │知识库上传   │  │       │ Agent 层    │  │
        │Knowledge   │  │       │RouterAgent  │  │
        │BaseService │  │       │Diagnosis    │  │
        └─────┬─────┘  │       │Alarm        │  │
              │        │       │Knowledge    │  │
    ┌─────────┤        │       │Subagents    │  │
    │   │     │        │       └──────┬──────┘  │
    │   │     │        │              │         │
    │  ┌▼──┐ ┌▼─────┐ │     ┌───────┼─────────┤
    │  │解析│ │向量化 │ │     │       │         │
    │  │器  │ │服务   │ │  ┌──▼──┐ ┌─▼──┐ ┌───▼────┐
    │  └───┘ └──────┘ │  │ RAG │ │Tool│ │Memory  │
    │                  │  │检索 │ │工具│ │ 记忆   │
    └──────────────────┘  └─────┘ └────┘ └────────┘
              │
        ┌─────▼──────┐
        │ 数据持久层   │
        │MySQL/Milvus │
        │  /Redis     │
        └────────────┘
```

---

## 五、主要功能模块

### 5.1 意图路由模块

**实现类**: [RouterAgent](src/main/java/org/example/agent/router/RouterAgent.java)

识别 9 类用户意图并路由到对应子图：

| 意图 | 说明 | 路由目标 |
|------|------|----------|
| SAFETY_QA | 安规问答 | KnowledgeQASubGraph |
| DEVICE_STATUS | 设备状态查询 | DeviceQuerySubGraph |
| ALARM_QUERY | 告警查询 | AlarmAnalysisSubGraph |
| LOG_ANALYSIS | 日志分析 | DeviceQuerySubGraph |
| TICKET_QUERY | 工单查询 | DeviceQuerySubGraph |
| DEVICE_PROFILE | 设备台账 | DeviceQuerySubGraph |
| FAULT_DIAGNOSIS | 故障诊断 | FaultDiagnosisSubGraph |
| ALARM_DIAGNOSIS | 告警诊断 | FaultDiagnosisSubGraph |
| GENERAL_CHAT | 通用对话 | ChatSubGraph |

**核心特性**：

- 温度 0.1 保证路由确定性，TopP 0.5，MaxToken 200
- 返回 JSON 格式：`{"intent": "意图类型", "confidence": 0.95, "deviceId": "设备编号", "keywords": ["关键词"]}`
- 识别失败自动降级为 `GENERAL_CHAT`

### 5.2 故障诊断模块

**实现类**: [DiagnosisAgent](src/main/java/org/example/agent/diagnosis/DiagnosisAgent.java) + [SubagentExecutor](src/main/java/org/example/agent/subagent/SubagentExecutor.java) + FaultDiagnosisSubGraph

**诊断流程**:

1. **实体提取**：从用户问题中提取设备 ID、故障现象
2. **并行证据采集**：同时调度 4 个子 Agent
   - `regulation`：查询相关安规条款
   - `metrics`：查询实时运行状态和历史趋势
   - `log`：查询设备运行日志
   - `ticket`：查询历史缺陷工单
3. **综合诊断**：基于所有证据生成 8 项结构化诊断报告
4. **风险评估**：评估诊断建议的风险等级（LOW / MEDIUM / HIGH / CRITICAL）
5. **重规划决策**：证据不足时自动补充调查（最多 2 次），高风险操作触发人工审批
6. **行动建议**：生成处理建议和安全提示

**输出格式**:

```
1. 告警摘要
2. 初步判断
3. 分析依据
4. 可能原因（按可能性排序）
5. 排查步骤
6. 处理建议
7. 安全风险提示
8. 是否建议派单
```

### 5.3 RAG 检索增强模块

**实现类**: [HybridSearchService](src/main/java/org/example/rag/HybridSearchService.java) + [RerankService](src/main/java/org/example/rag/RerankService.java) + [KnowledgeGraphService](src/main/java/org/example/rag/KnowledgeGraphService.java) + [VectorSearchService](src/main/java/org/example/service/VectorSearchService.java)

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
  ├── 术语匹配率 (0.4)
  ├── 完整查询包含 (0.3)
  ├── 安规领域匹配 (0.2)
  └── 电力设备领域匹配 (0.1)
  ↓
KnowledgeGraphService.buildGraphContext()
  ├── 查找可能原因
  ├── 查找排查步骤
  └── 查找相关安规
  ↓
注入 LLM 上下文生成回答
```

**核心特性**：

- 向量检索使用 DashScope text-embedding-v4 模型生成 Embedding（1024 维）
- Milvus 不可用时自动降级为内存向量存储（`InMemoryVectorStore`）
- 内置电力领域关键词索引，包含变压器油温、开关柜局放、倒闸操作等 10 条核心知识

### 5.4 RAG 文档上传与知识库构建模块

**实现类**: [KnowledgeBaseService](src/main/java/org/example/service/KnowledgeBaseService.java) + [DocumentChunkService](src/main/java/org/example/service/DocumentChunkService.java) + [VectorEmbeddingService](src/main/java/org/example/service/VectorEmbeddingService.java) + [VectorIndexService](src/main/java/org/example/service/VectorIndexService.java)

**完整上传处理链路**：

```
管理员上传文档 (POST /api/knowledge/documents/upload)
  ↓
文件类型校验 (validateFileType — 基于 FileUploadConfig.allowedExtensions)
  ↓
保存文件到磁盘 + 创建文档记录 (knowledge_document 表)
  ↓
版本管理 (同名文档自动递增版本号，旧版本 enabled=0)
  ↓
创建处理任务 (knowledge_process_task 表)
  ↓
异步处理 (processDocument):
  ├── PARSING: 文档解析 (6 种解析器策略模式)
  │   ├── TextDocumentParser   → txt, md
  │   ├── PdfDocumentParser    → pdf
  │   ├── WordDocumentParser   → docx
  │   ├── ExcelDocumentParser  → xlsx
  │   └── HtmlDocumentParser   → html, htm
  ├── CLEANING: 文本清洗 (特殊字符、多余空格和换行)
  ├── CHUNKING: 智能切片
  │   ├── 先按 Markdown 标题 (#~######) 分割
  │   ├── 再按段落边界细分 (超过 maxSize 时)
  │   └── 支持重叠 (overlap 字符数)
  ├── 持久化切片 (knowledge_chunk 表 — 支持断点恢复和审计)
  ├── INDEXING: 逐片向量化 + 写入向量库
  │   ├── DashScope text-embedding-v4 生成 1024 维向量
  │   ├── Milvus 写入 (milvus.enabled=true)
  │   └── InMemoryVectorStore 写入 (milvus.enabled=false)
  └── COMPLETED: 更新文档状态和嵌入计数
```

**文档生命周期管理**：

| 操作 | API | 说明 |
|------|-----|------|
| 上传 | `POST /api/knowledge/documents/upload` | 上传文档，异步处理（解析→切片→向量化→写入向量库） |
| 查询状态 | `GET /api/knowledge/documents/{id}/status` | 查询文档处理进度和状态 |
| 列表 | `GET /api/knowledge/documents` | 分页列出文档 |
| 删除 | `DELETE /api/knowledge/documents/{id}` | 删除文档 + 向量库数据 + 分片数据 + 磁盘文件 |
| 搜索测试 | `POST /api/knowledge/search/test` | 向量搜索测试 |
| 版本列表 | `GET /api/knowledge/documents/{name}/versions` | 获取文档版本列表 |
| 版本回滚 | `POST /api/knowledge/documents/{id}/rollback` | 回滚到指定版本（持久化 enabled 字段） |

**支持的文件格式**：txt, md, pdf, docx, xlsx, html

**核心特性**：

- 文件类型校验：基于 `file.upload.allowed-extensions` 配置，拒绝不支持的格式
- 版本管理：同名文档自动递增版本号，支持版本回滚（持久化到数据库）
- 分片持久化：切片结果写入 `knowledge_chunk` 表，服务重启后不丢失
- 向量库同步写入：向量化后立即写入 Milvus 或 InMemoryVectorStore，确保可检索
- 删除级联清理：删除文档时同步清理向量库数据、分片数据、处理任务和磁盘文件
- 异步处理：4 线程池异步执行，不阻塞上传接口响应
- 优雅关闭：`@PreDestroy` 确保线程池正常关闭

### 5.5 电力工具模块

| 工具类 | 功能 | 关键方法 |
|--------|------|----------|
| PowerDeviceStatusTools | 设备实时运行状态 | `getDeviceStatus(deviceId, metrics, timeRange)` |
| PowerAlarmHistoryTools | 历史告警查询 | `getAlarmHistory(deviceId, alarmType, timeRange, limit)` |
| PowerSafetyRulesTools | 安规条款查询 | `searchSafetyRules(query, ruleType)` |
| PowerDefectTicketTools | 缺陷工单查询 | `getDefectTickets(deviceId, defectType, timeRange)` |
| PowerDeviceLogsTools | 设备运行日志 | `getDeviceLogs(deviceId, timeRange, keywords)` |
| PowerDeviceProfileTools | 设备台账信息 | `getDeviceProfile(deviceId)` |
| InternalDocsTools | 内部文档检索 | `queryInternalDocs(query)` |
| DateTimeTools | 日期时间 | `getCurrentDateTime()` |

所有工具支持 Mock 模式（`power.mock-enabled=true`），无需真实设备连接即可开发测试。

### 5.6 记忆管理模块

**实现类**: [MemoryService](src/main/java/org/example/memory/MemoryService.java)

四层记忆架构：

| 层级 | 存储 | TTL | 用途 |
|------|------|-----|------|
| Session（会话） | `ConcurrentHashMap` | 2 小时 | 当前对话上下文（最近消息、当前设备等） |
| Task（任务） | `ConcurrentHashMap` | 24 小时 | 单次诊断任务数据（采集的证据、中间结果等） |
| Domain（领域） | `ConcurrentHashMap` | 无过期 | 电力领域知识（常用安规、设备参数等） |
| User（用户） | `ConcurrentHashMap` | 30 天 | 用户偏好和习惯（常用设备、查询模式等） |

`buildContextForAgent()` 方法聚合四层记忆，注入 Agent 系统提示词。支持过期清理（`cleanupExpired()`）。

### 5.7 安全控制模块

**RBAC 权限控制** ([RbacService](src/main/java/org/example/security/RbacService.java)):

```
admin    → chat, diagnose, knowledge:upload, knowledge:delete, knowledge:search,
           alarm:receive, alarm:diagnose, skill:manage, system:config,
           approval:approve, observability:view, QUERY, TICKET_CREATE,
           DEVICE_CONTROL, LOAD_CONTROL, USER_MANAGE

operator → chat, diagnose, knowledge:search, alarm:receive, alarm:diagnose,
           observability:view, QUERY, TICKET_CREATE

viewer   → chat, knowledge:search, observability:view, QUERY
```

**审批流程** ([ApprovalService](src/main/java/org/example/security/ApprovalService.java)):

```
高风险操作 → 创建审批单 (PENDING) → 审批通过 (APPROVED) / 拒绝 (REJECTED)
```

**Hook 引擎** ([HookEngine](src/main/java/org/example/hook/HookEngine.java)):

4 个拦截点：`PRE_ROUTE` → `POST_ROUTE` → `PRE_DIAGNOSIS` → `POST_DIAGNOSIS`

内置 Hook 实现：

| Hook | 拦截点 | 功能 |
|------|--------|------|
| SafetyCheckHook | PRE_ROUTE | 安全检查，拦截危险请求 |
| DataMaskingHook | POST_ROUTE | 数据脱敏 |
| PreRouteHook / PostRouteHook | PRE/POST_ROUTE | 路由前后处理 |
| PreDiagnosisHook / PostDiagnosisHook | PRE/POST_DIAGNOSIS | 诊断前后处理 |
| PreToolUseHook / PostToolUseHook | - | 工具调用前后处理 |
| HumanApprovalHook | - | 高风险操作人工审批 |
| AuditHook | - | 审计日志记录 |

### 5.8 可观测性模块

**实现类**: [ObservabilityService](src/main/java/org/example/observability/ObservabilityService.java)

- **Trace/Span 模型**：每次请求生成唯一 TraceId，每个 Agent 步骤生成 Span，支持父子 Span 嵌套
- **执行日志**：Agent 执行日志持久化到 `agent_execution_log` 表
- **工具调用日志**：工具调用日志持久化到 `tool_call_log` 表
- **链路追踪**：通过 TraceId 查询完整执行链路，包含每步耗时、Token 消耗、状态等

### 5.9 检查点与断点恢复模块

**实现类**: [CheckpointService](src/main/java/org/example/checkpoint/CheckpointService.java)

- 执行过程自动保存检查点到 MySQL（`checkpoint_record` 表）
- 状态流转：`ROUTED` → `SKILL_SELECTED` → `SUBAGENTS_START` → `SUBAGENTS_DONE` → `DIAGNOSIS_GENERATED` → `COMPLETED`
- 支持从任意检查点恢复执行（`/api/alarm/resume/{taskId}`）

### 5.10 插件管理模块

**实现类**: [PluginManager](src/main/java/org/example/plugin/PluginManager.java)

内置 4 个插件：

| 插件 ID | 名称 | 类别 |
|---------|------|------|
| power-monitoring | 电力监控插件 | monitoring |
| power-safety | 安规查询插件 | safety |
| power-diagnosis | 故障诊断插件 | diagnosis |
| power-knowledge | 知识库插件 | knowledge |

### 5.11 Prompt 管理模块

**实现类**: [PromptRegistry](src/main/java/org/example/prompt/PromptRegistry.java)

集中管理 13 个提示词模板，支持 `{{变量名}}` 变量替换：

| key | 用途 |
|-----|------|
| router | 意图路由 |
| chat | 通用对话 |
| knowledge | 知识问答 |
| alarm | 告警分析 |
| diagnosis | 故障诊断 |
| log_analysis | 日志分析 |
| ticket | 工单分析 |
| rag | RAG 检索增强 |
| subagent_regulation | 安规查询子 Agent |
| subagent_metrics | 设备状态查询子 Agent |
| subagent_log | 日志分析子 Agent |
| subagent_ticket | 工单分析子 Agent |
| subagent_risk_review | 风险复核子 Agent |

---

## 六、项目主要链路

### 6.1 智能对话链路

```
用户输入 "查询1号主变的油温"
  ↓
POST /api/chat { question: "查询1号主变的油温", sessionId: "sess-001", userId: "admin" }
  ↓
ChatController.chat()
  ├── graph.enabled=true → CompiledGraph.invoke()
  │   ↓
  │   PreCheckNode → ContextLoadNode → RouterNode
  │   ↓ (意图: DEVICE_STATUS)
  │   DeviceQuerySubGraph
  │   ├── EntityExtractNode → { deviceId: "TR-110KV-001", attribute: "oilTemp" }
  │   ├── PermissionCheckNode → RBAC 校验通过 (admin 拥有 QUERY 权限)
  │   ├── ToolSelectNode → 选择 getDeviceStatus
  │   ├── ToolExecuteNode → 调用 PowerDeviceStatusTools
  │   └── DataFormatNode → "TR-110KV-001 当前油温 72℃，负荷率 68%，冷却器运行正常"
  │   ↓
  │   SafetyReviewNode → FinalResponseNode → MemorySaveNode
  │   ↓
  └── 返回 { sessionId: "sess-001", answer: "TR-110KV-001 当前油温 72℃...", engine: "graph" }
  │
  └── graph.enabled=false → AgentOrchestrationService.handleChat()
      ↓ (降级到旧编排)
      返回 { answer: "...", engine: "legacy" }
```

### 6.2 故障诊断链路

```
告警事件触发
  ↓
POST /api/alarm/receive { deviceId: "TR-110KV-001", alarmType: "油温高", alarmLevel: "紧急", ... }
  ↓
AlarmTask 持久化到 MySQL → 返回 { taskId: "TASK-a1b2c3d4", status: "RECEIVED" }
  ↓
POST /api/alarm/diagnose { taskId: "TASK-a1b2c3d4" }
  ↓
AlarmController.diagnoseAlarm()
  ├── graph.enabled=true → CompiledGraph.invoke({ intent: "FAULT_DIAGNOSIS" })
  │   ↓
  │   FaultDiagnosisSubGraph
  │   ├── DiagnosisEntityExtractNode → 提取设备/故障实体
  │   ├── EvidenceParallelNode → 并行采集 4 维证据
  │   │   ├── regulation → 安规条款
  │   │   ├── metrics → 实时指标
  │   │   ├── log → 运行日志
  │   │   └── ticket → 历史工单
  │   ├── DiagnosisNode → 综合诊断报告
  │   ├── RiskAssessmentNode → 风险等级 HIGH
  │   ├── DiagnosisReplannerNode → next_action=HUMAN_APPROVAL
  │   └── ActionRecommendNode → 行动建议 + 高风险提醒
  │   ↓
  │   SafetyReviewNode → FinalResponseNode → MemorySaveNode
  │   ↓
  └── SSE 返回诊断结果
```

### 6.3 知识问答链路

```
用户输入 "变压器油温高的处理规程"
  ↓
POST /api/chat { question: "变压器油温高的处理规程" }
  ↓
KnowledgeQASubGraph
  ├── QueryRewriteNode → "变压器油温高告警 处理规程"
  ├── RagRetrieveNode → 混合检索 topK=6 → RRF 合并 → topK=3
  ├── RerankNode → 重排序
  ├── AnswerGenerateNode → 注入检索上下文 + GraphRAG 扩展 → 生成回答
  └── CitationCheckNode → 验证引用支撑
  ↓
SafetyReviewNode → FinalResponseNode → MemorySaveNode
```

### 6.4 RAG 文档上传与知识库构建链路

```
管理员上传文档 "主变压器运行维护规程.pdf"
  ↓
POST /api/knowledge/documents/upload
  file=@主变压器运行维护规程.pdf
  documentType=电力安规
  source=国网标准
  ↓
KnowledgeController.uploadDocument()
  ↓
KnowledgeBaseService.uploadDocument()
  ├── 文件类型校验 (pdf 在允许列表中)
  ├── 保存文件到磁盘 (uploads/DOC-a1b2c3d4.pdf)
  ├── 创建文档记录 (knowledge_document: status=UPLOADED)
  ├── 版本管理 (同名文档自动递增版本号)
  └── 创建处理任务 (knowledge_process_task: status=PENDING)
  ↓
异步处理 processDocument():
  ├── PARSING: PdfDocumentParser.parse() → 提取全文文本
  ├── CLEANING: cleanText() → 清理特殊字符和多余空白
  ├── CHUNKING: DocumentChunkService.chunkDocument()
  │   ├── 按 Markdown 标题分割章节
  │   ├── 按段落边界细分 (maxSize=800, overlap=100)
  │   └── 生成 List<DocumentChunk>
  ├── 持久化切片: 逐片写入 knowledge_chunk 表
  │   └── { chunkId: "DOC-a1b2c3d4-CHUNK-0", documentId, chapter, content, status: "ACTIVE" }
  ├── INDEXING: 逐片向量化 + 写入向量库
  │   ├── VectorEmbeddingService.generateEmbedding() → 1024 维向量
  │   ├── 构建 metadata { documentId, documentName, documentType, source, chapter, chunkIndex }
  │   ├── Milvus: insertToMilvus() (milvus.enabled=true)
  │   └── 或 InMemoryVectorStore: insertToMemory() (milvus.enabled=false)
  └── COMPLETED: 更新文档状态和嵌入计数
  ↓
返回 { documentId: "DOC-a1b2c3d4", status: "COMPLETED", chunkCount: 15, embeddingCount: 15 }
```

### 6.5 动态规划链路（复杂任务）

```
用户输入 "对比分析1号主变和2号主变的运行状态并给出维护建议"
  ↓
RouterNode → 意图: COMPLEX_TASK
  ↓
DynamicPlanSubGraph
  ├── PlannerNode → LLM 生成执行计划
  │   { steps: [
  │       { step:1, agentType:"tool", action:"getDeviceStatus", purpose:"查询1号主变状态" },
  │       { step:2, agentType:"tool", action:"getDeviceStatus", purpose:"查询2号主变状态" },
  │       { step:3, agentType:"diagnosis", action:"compare", purpose:"对比分析" },
  │       { step:4, agentType:"chat", action:"summarize", purpose:"生成维护建议" }
  │   ]}
  ├── DynamicExecutorNode → StepHandlerRegistry 分发执行
  │   ├── Step 1: ToolStepHandler → 查询1号主变
  │   ├── Step 2: ToolStepHandler → 查询2号主变
  │   ├── Step 3: DiagnosisStepHandler → 对比分析
  │   └── Step 4: ChatStepHandler → 生成建议
  ├── DynamicReplannerNode → next_action=END
  └── FinalizePlanNode → 汇总所有步骤结果
```

---

## 七、安装启动指南

### 7.1 环境要求

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 必须，项目使用 Java 17 特性（record、sealed class 等） |
| Maven | 3.9+ | 项目构建与依赖管理 |
| MySQL | 8.x | 业务数据存储（告警、工单、日志、检查点、知识库等） |
| Redis | 6.x+ | 缓存（可选，未配置时部分功能降级） |
| Milvus | 2.x | 向量数据库（可选，可降级为内存向量存储模式） |
| Docker | 20.x+ | Milvus 部署依赖 |
| DashScope API Key | - | 阿里云大模型 API 密钥（必填） |

### 7.2 依赖安装

#### MySQL 数据库

```sql
CREATE DATABASE power_aiops
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

项目启动时会通过 MyBatis-Plus 实体自动扫描创建所需表。也可手动执行 [schema.sql](src/main/resources/schema.sql) 初始化表结构，包含以下核心表：

| 表名 | 用途 |
|------|------|
| chat_session | 对话会话 |
| chat_message | 对话消息 |
| alarm_task | 告警任务 |
| agent_execution_log | Agent 执行日志 |
| tool_call_log | 工具调用日志 |
| knowledge_document | 知识文档（文档元数据、状态、版本） |
| knowledge_chunk | 知识分块（切片内容、章节、状态） |
| knowledge_process_task | 知识处理任务（处理进度、状态） |
| tool_registry | 工具注册表 |
| skill_definition | 技能定义 |
| checkpoint_record | 检查点记录 |

#### Milvus 向量数据库（可选）

```bash
# 方式一：使用项目自带的 docker-compose.yml 启动
docker-compose up -d

# 方式二：手动启动 Milvus Standalone
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.3.3
```

如不安装 Milvus，将 `milvus.enabled` 设为 `false`，系统会自动降级为内存向量存储模式。

### 7.3 配置文件说明

配置文件位于 `src/main/resources/application.yml`：

```yaml
server:
  port: 9900                    # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/power_aiops?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-api-key}    # 必填
      chat:
        options:
          timeout: 180000                             # 超时 3 分钟
      retry:
        max-attempts: 3
        backoff:
          initial-interval: 2000
          multiplier: 2
          max-interval: 10000

milvus:
  enabled: true                  # false 则使用内存向量存储
  host: localhost
  port: 19530

dashscope:
  api:
    key: ${DASHSCOPE_API_KEY:your-api-key}
  embedding:
    model: text-embedding-v4     # 文本向量化模型

file:
  upload:
    path: ./uploads              # 文件上传存储路径
    allowed-extensions: txt,md,pdf,docx,xlsx,html  # 允许的文件类型

power:
  mock-enabled: true             # true 则工具返回模拟数据，无需真实设备连接
  alarm:
    levels: 紧急,重要,一般,提示
  device-types: 变压器,开关柜,断路器,隔离开关,母线,电缆,互感器
  document-types: 电力安规,设备手册,巡检标准,故障处理规程,历史故障案例,专家经验文档,厂家说明书,标准作业指导书

powerops:
  graph:
    enabled: true                # true 使用 Graph 引擎，false 使用旧编排

rag:
  top-k: 3                       # RAG 检索返回条数
  model: "qwen3-max"             # RAG 生成模型

document:
  chunk:
    max-size: 800                # 文档分块最大字符数
    overlap: 100                 # 分块重叠字符数
```

**关键配置项**：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.ai.dashscope.api-key` | - | **必填**，阿里云 DashScope API 密钥 |
| `powerops.graph.enabled` | false | 是否启用 Graph 工作流引擎（推荐 true） |
| `power.mock-enabled` | true | 工具是否返回模拟数据（开发阶段建议 true） |
| `milvus.enabled` | true | 是否使用 Milvus 向量数据库 |
| `file.upload.allowed-extensions` | txt,md,pdf,docx,xlsx,html | 允许上传的文件类型 |
| `rag.top-k` | 3 | RAG 检索返回条数 |
| `document.chunk.max-size` | 800 | 文档分块最大字符数 |
| `document.chunk.overlap` | 100 | 分块重叠字符数 |

### 7.4 启动命令

```bash
# 1. 设置 JDK 17 环境
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# 验证 Java 版本
java -version
# 输出应包含 openjdk version "17.x.x"

# 2. 编译项目
mvn clean compile

# 3. 启动应用
mvn spring-boot:run

# 或使用 JAR 包启动
mvn clean package -DskipTests
java -jar target/grid-ops-agent-1.0-SNAPSHOT.jar
```

启动成功后访问：`http://localhost:9900`

### 7.5 一键初始化（Makefile）

项目提供了 Makefile 用于自动化初始化：

```bash
# 一键初始化：启动 Docker → 启动服务 → 上传文档
make init

# 分步操作
make up          # 启动 Docker Compose（Milvus 向量数据库）
make start       # 启动 Spring Boot 服务（后台运行）
make wait        # 等待服务就绪
make upload      # 上传 aiops-docs 目录下的文档到知识库（自动向量化）

# 其他命令
make check       # 检查服务器是否运行
make status      # 查看 Docker 容器状态
make stop        # 停止 Spring Boot 服务
make down        # 停止 Docker Compose
make restart     # 重启 Spring Boot 服务
make clean       # 清理临时文件
```

### 7.6 环境变量

| 变量 | 说明 | 优先级 |
|------|------|--------|
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API 密钥 | 高于配置文件 |
| `MYSQL_USERNAME` | MySQL 用户名 | 高于配置文件 |
| `MYSQL_PASSWORD` | MySQL 密码 | 高于配置文件 |
| `REDIS_HOST` | Redis 主机地址 | 高于配置文件 |
| `REDIS_PORT` | Redis 端口 | 高于配置文件 |
| `REDIS_PASSWORD` | Redis 密码 | 高于配置文件 |

---

## 八、使用说明

### 8.1 Web UI 操作

项目提供两个 Web 页面，通过顶部导航栏互相跳转：

| 页面 | 地址 | 功能 |
|------|------|------|
| 智能对话 | `http://localhost:9900` | 对话问答、告警接入、快捷提问 |
| 知识库管理 | `http://localhost:9900/admin.html` | 文档上传、列表管理、版本回滚、搜索测试 |

#### 智能对话页面

1. 在左侧输入框输入问题
2. 按 Enter 发送（Shift+Enter 换行）
3. 系统自动识别意图并路由到对应 Agent

**快捷提问示例**：

| 按钮 | 对应问题 |
|------|----------|
| 高压室安全 | "高压室作业有哪些安全要求？" |
| 查油温 | "查询1号主变的油温" |
| 故障诊断 | "1号主变油温异常，请诊断" |
| 历史告警 | "查询TR-110KV-001的历史告警" |
| 安规查询 | "变压器运行监视的安规要求" |
| 设备台账 | "查询TR-110KV-001的设备信息" |

#### 告警诊断

1. 在右侧"告警接入"面板填写告警信息
2. 选择设备类型和告警等级
3. 点击"提交告警并诊断"
4. 系统返回 SSE 流式诊断结果

#### 知识库管理页面

点击顶部导航栏"知识库管理"进入管理员页面，包含 3 个功能区：

**文档上传**：

1. 点击或拖拽文件到上传区域（支持 txt/md/pdf/docx/xlsx/html）
2. 选择文档类型（电力安规/设备手册/巡检标准等）
3. 填写来源、描述、标签（可选）
4. 点击"上传文档"，系统自动异步处理（解析→切片→向量化→写入向量库）
5. 上传成功后显示文档 ID，文档列表自动刷新

**文档列表**：

- 表格展示所有文档的名称、类型、格式、状态、分片数、向量数、版本、上传时间
- 状态标签颜色：COMPLETED(绿)、PARSING/CLEANING/CHUNKING/INDEXING(蓝动画)、FAILED(红)、UPLOADED(灰)
- 操作按钮：
  - **状态**：弹窗查看处理进度（当前步骤/错误信息）
  - **版本**：弹窗查看所有版本，支持回滚到指定版本
  - **回滚**：确认后回滚到该版本（其他版本自动禁用）
  - **删除**：确认后级联删除（向量库数据+分片数据+处理任务+磁盘文件）
- 自动刷新：有处理中的文档时每 5 秒自动刷新列表
- 支持按文档类型筛选和分页浏览

**搜索测试**：

1. 输入查询文本（如"变压器油温高处理规程"）
2. 设置 topK（默认 5）
3. 点击"搜索"，查看向量搜索结果（文档名、相似度、内容预览、metadata）

### 8.2 API 接口

#### 智能对话

```bash
curl -X POST http://localhost:9900/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "查询1号主变的油温",
    "sessionId": "test-session-001",
    "userId": "admin"
  }'
```

响应：

```json
{
  "sessionId": "test-session-001",
  "answer": "TR-110KV-001 当前油温 72℃，负荷率 68%，冷却器运行正常...",
  "engine": "graph"
}
```

#### SSE 流式对话

```bash
curl -X POST http://localhost:9900/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "question": "变压器油温高的处理规程",
    "sessionId": "test-session-001",
    "userId": "admin"
  }'
```

返回 SSE 事件流：`search_results` → `reasoning` → `message` → `done`

#### 告警接入

```bash
curl -X POST http://localhost:9900/api/alarm/receive \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "TR-110KV-001",
    "deviceName": "1号主变压器",
    "deviceType": "变压器",
    "alarmType": "油温高",
    "alarmLevel": "紧急",
    "currentValue": "85℃",
    "threshold": "75℃"
  }'
```

响应：

```json
{
  "taskId": "TASK-a1b2c3d4",
  "alarmId": "ALM-1710000000000",
  "status": "RECEIVED",
  "message": "告警已接收，请调用 /api/alarm/diagnose 启动诊断"
}
```

#### 告警诊断

```bash
curl -X POST http://localhost:9900/api/alarm/diagnose \
  -H "Content-Type: application/json" \
  -d '{ "taskId": "TASK-a1b2c3d4" }'
```

返回 SSE 流式诊断结果。

#### 知识库文档上传

```bash
curl -X POST http://localhost:9900/api/knowledge/documents/upload \
  -F "file=@aiops-docs/主变压器运行维护规程.md" \
  -F "documentType=电力安规" \
  -F "source=国网标准" \
  -F "description=变压器运行维护标准规程" \
  -F "tags=变压器,安规,运维"
```

响应：

```json
{
  "documentId": "DOC-a1b2c3d4",
  "documentName": "主变压器运行维护规程.md",
  "status": "UPLOADED",
  "message": "文档上传成功，正在后台处理"
}
```

支持的文件格式：txt, md, pdf, docx, xlsx, html

#### 查询文档处理状态

```bash
curl http://localhost:9900/api/knowledge/documents/DOC-a1b2c3d4/status
```

响应：

```json
{
  "documentId": "DOC-a1b2c3d4",
  "documentName": "主变压器运行维护规程.md",
  "status": "COMPLETED",
  "chunkCount": 15,
  "embeddingCount": 15,
  "processStatus": "COMPLETED",
  "currentStep": null,
  "errorMessage": null
}
```

#### 文档列表

```bash
curl "http://localhost:9900/api/knowledge/documents?documentType=电力安规&page=1&size=20"
```

#### 删除文档

```bash
curl -X DELETE http://localhost:9900/api/knowledge/documents/DOC-a1b2c3d4
```

删除操作会级联清理：向量库数据 + knowledge_chunk 分片 + knowledge_process_task 任务 + 磁盘文件。

#### 知识库搜索测试

```bash
curl -X POST http://localhost:9900/api/knowledge/search/test \
  -H "Content-Type: application/json" \
  -d '{ "query": "变压器油温高处理", "topK": 5 }'
```

#### 文档版本回滚

```bash
curl -X POST http://localhost:9900/api/knowledge/documents/DOC-a1b2c3d4/rollback
```

#### 查询诊断状态

```bash
curl http://localhost:9900/api/alarm/diagnose/TASK-a1b2c3d4/status
```

#### 断点恢复

```bash
curl -X POST http://localhost:9900/api/alarm/resume/TASK-a1b2c3d4
```

#### 查询检查点

```bash
curl http://localhost:9900/api/alarm/checkpoint/TASK-a1b2c3d4
```

#### 清空会话

```bash
curl -X POST http://localhost:9900/api/chat/clear \
  -H "Content-Type: application/json" \
  -d '{ "sessionId": "test-session-001" }'
```

### 8.3 编排引擎切换

通过配置项 `powerops.graph.enabled` 控制使用哪种编排引擎：

```yaml
powerops:
  graph:
    enabled: true   # 使用 Graph 工作流引擎（推荐）
    # enabled: false  # 使用旧编排 AgentOrchestrationService
```

- **Graph 引擎**：StateGraph 显式编排，支持子图、条件边、循环 Replan，流程可追踪
- **旧编排**：AgentOrchestrationService 中心化编排，作为降级后备

当 Graph 引擎执行异常时，系统自动降级到旧编排，无需手动切换。响应中的 `engine` 字段标识实际使用的引擎（`"graph"` 或 `"legacy"`）。

### 8.4 Mock 模式

开发阶段无需连接真实设备，启用 Mock 模式即可：

```yaml
power:
  mock-enabled: true
```

所有电力工具（设备状态、告警历史、安规查询等）将返回预设的模拟数据，方便开发和测试。

### 8.5 常见问题

| 问题 | 解决方案 |
|------|----------|
| 启动报 JDK 版本错误 | 确认 `java -version` 输出为 17+，设置 `JAVA_HOME` |
| Milvus 连接失败 | 检查 Docker 是否启动：`docker ps \| grep milvus`；或设置 `milvus.enabled=false` |
| DashScope API 报错 | 检查 API Key 是否正确，确认账户余额充足 |
| MySQL 连接失败 | 检查数据库是否创建（`power_aiops`），确认用户名密码 |
| 工具调用返回空数据 | 确认 `power.mock-enabled=true` 或真实设备接口可用 |
| Graph 引擎启动失败 | 设置 `powerops.graph.enabled=false` 使用旧编排 |
| 文档上传后搜不到 | 检查文档处理状态是否为 COMPLETED，确认 `milvus.enabled` 配置正确 |
| 上传文件类型被拒绝 | 检查 `file.upload.allowed-extensions` 配置，确认文件格式在允许列表中 |

---

## 项目结构

```
src/main/java/org/example/
├── agent/                          # Agent 层
│   ├── router/RouterAgent          # 意图路由
│   ├── diagnosis/DiagnosisAgent    # 故障诊断
│   ├── alarm/AlarmAgent            # 告警分析
│   ├── knowledge/KnowledgeAgent    # 知识问答
│   ├── log/LogAnalysisAgent        # 日志分析
│   ├── ticket/TicketAgent          # 工单分析
│   ├── skill/                      # Skill 管理
│   │   ├── model/Skill             # 技能模型
│   │   ├── service/SkillRegistry   # 技能注册中心
│   │   └── service/SkillSelector   # 技能选择器
│   ├── subagent/                   # 子 Agent
│   │   ├── SubagentExecutor        # 并行执行器
│   │   └── SubagentTask            # 任务模型
│   └── tool/                       # Agent 工具
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
│   ├── PowerOpsGraphConfig         # 顶层图配置（Bean 定义）
│   ├── PowerOpsStateFactory        # 状态工厂（KeyStrategyFactory）
│   ├── dispatcher/
│   │   └── IntentDispatcher        # 意图条件边分发器
│   ├── handler/                    # StepHandler（动态规划执行器）
│   │   ├── PlanStepHandler         # Handler 接口
│   │   ├── StepHandlerRegistry     # Handler 注册中心
│   │   ├── ToolStepHandler         # 工具执行
│   │   ├── KnowledgeStepHandler    # 知识问答
│   │   ├── AlarmStepHandler        # 告警分析
│   │   ├── DiagnosisStepHandler    # 综合诊断
│   │   ├── RagStepHandler          # RAG 检索
│   │   ├── SubAgentStepHandler     # 子 Agent
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
│   └── subgraph/                   # 子图
│       ├── knowledge/              # 知识问答子图（5 节点）
│       ├── device/                 # 设备查询子图（5 节点）
│       ├── alarm/                  # 告警分析子图（5 节点）
│       ├── diagnosis/              # 故障诊断子图（7 节点 + Replan）
│       ├── dynamic/                # 动态规划子图（5 节点 + Replan）
│       └── chat/                   # 通用对话子图（1 节点）
├── controller/                     # 控制器层
│   ├── ChatController              # 对话接口
│   ├── AlarmController             # 告警接口
│   ├── ApprovalController          # 审批接口
│   ├── KnowledgeController         # 知识库接口（上传/状态/列表/删除/搜索/版本/回滚）
│   ├── SkillController             # 技能接口
│   ├── ObservabilityController     # 可观测性接口
│   ├── EvalController              # 评测接口
│   ├── MilvusCheckController       # Milvus 健康检查
│   └── ToolSearchController        # 工具搜索接口
├── service/                        # 服务层
│   ├── AgentOrchestrationService   # 旧编排（降级后备）
│   ├── AiOpsService                # AI 运维服务
│   ├── ChatService                 # 对话服务
│   ├── KnowledgeBaseService        # 知识库管理（上传/处理/删除/回滚/搜索）
│   ├── DocumentChunkService        # 文档智能分块
│   ├── VectorEmbeddingService      # 向量化服务（DashScope API）
│   ├── VectorIndexService          # 向量索引服务（Milvus/InMemory）
│   ├── VectorSearchService         # 向量检索服务
│   ├── InMemoryVectorStore         # 内存向量存储
│   ├── RagService                  # RAG 检索增强
│   └── parser/                     # 文档解析器
│       ├── DocumentParser          # 解析接口
│       ├── PdfDocumentParser       # PDF 解析
│       ├── WordDocumentParser      # Word 解析（docx）
│       ├── ExcelDocumentParser     # Excel 解析（xlsx）
│       ├── HtmlDocumentParser      # HTML 解析
│       └── TextDocumentParser      # 纯文本/Markdown 解析
├── rag/                            # RAG 检索层
│   ├── HybridSearchService         # 混合检索（向量 + 关键词 + RRF）
│   ├── RerankService               # 重排序
│   └── KnowledgeGraphService       # 知识图谱
├── memory/MemoryService            # 四层记忆管理
├── hook/                           # Hook 引擎
│   ├── HookEngine                  # Hook 执行引擎
│   ├── AgentHook                   # Hook 接口
│   ├── HookContext                 # Hook 上下文
│   ├── HookResult                  # Hook 结果
│   └── impl/                       # Hook 实现（12 个）
├── checkpoint/CheckpointService    # 检查点服务
├── security/                       # 安全层
│   ├── RbacService                 # RBAC 权限控制
│   └── ApprovalService             # 审批服务
├── observability/ObservabilityService  # 可观测性（Trace/Span）
├── plugin/                         # 插件管理
│   ├── PluginManager               # 插件管理器
│   ├── PluginInfo                  # 插件信息
│   └── PluginLifecycle             # 插件生命周期
├── prompt/PromptRegistry           # Prompt 模板管理
├── entity/                         # 数据实体（MyBatis-Plus）
│   ├── KnowledgeDocument           # 知识文档
│   ├── KnowledgeChunk              # 知识分块
│   ├── KnowledgeProcessTask        # 知识处理任务
│   ├── AlarmTask                   # 告警任务
│   ├── CheckpointRecord            # 检查点记录
│   └── ...
├── mapper/                         # MyBatis Mapper 接口
│   ├── KnowledgeDocumentMapper     # 知识文档 Mapper
│   ├── KnowledgeChunkMapper        # 知识分块 Mapper
│   ├── KnowledgeProcessTaskMapper  # 知识处理任务 Mapper
│   └── ...
├── config/                         # 配置类
│   ├── DashScopeConfig             # DashScope 配置
│   ├── MilvusConfig                # Milvus 配置
│   ├── MyBatisPlusConfig           # MyBatis-Plus 配置
│   ├── ToolConfig                  # 工具注册配置
│   ├── HookConfig                  # Hook 注册配置
│   ├── FileUploadConfig            # 文件上传配置
│   ├── DocumentChunkConfig         # 文档分块配置
│   └── WebConfig / WebMvcConfig    # Web 配置
├── dto/                            # 数据传输对象
│   └── DocumentChunk               # 文档分块 DTO
├── constant/MilvusConstants        # Milvus 常量
├── client/MilvusClientFactory      # Milvus 客户端工厂
├── eval/                           # 评测模块
│   ├── EvalService                 # 评测服务
│   ├── EvalCase                    # 评测用例
│   └── EvalDataset                 # 评测数据集
└── tool/                           # 工具注册与搜索
    ├── ToolSearchService           # 工具搜索
    └── ToolRegistryService         # 工具注册
```
