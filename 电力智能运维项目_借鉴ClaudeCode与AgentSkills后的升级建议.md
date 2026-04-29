# 借鉴 Claude Code / Agent Skills 最新思想后的电力智能运维项目升级建议

## 1. 修改目标

当前项目已经具备以下基础能力：

```text
SpringBoot + SpringAI-Alibaba + RAG + ReAct + Plan-Execute-Replan + Multi-Agent + MCP + SSE
```

从业务方向看，项目是合理的；但从面试竞争力看，还可以进一步升级。原方案的问题不是“场景不对”，而是工程表达还不够像一个真实落地的企业级 Agent 平台。

本次修改建议借鉴近两年 Agent 工程化中的一些新设计思想，尤其是 Claude Code / Agent SDK 中体现出来的工程模式，例如：

```text
Skills：能力包 / 工作流包
Subagents：隔离上下文的专用子 Agent
Hooks：Agent 生命周期拦截器和安全控制点
MCP：外部工具和数据源标准化接入
Permissions：工具权限和人工审批
Checkpointing：任务检查点和可恢复执行
Observability：LLM / Tool / Hook / Subagent 全链路追踪
Tool Search：大规模工具动态检索
Todo / Plan：显式任务计划和执行状态
Plugins：可安装的领域能力插件包
```

升级目标是将项目从：

```text
基于大模型的电力智能运维 Multi-Agent 系统
```

升级为：

```text
面向电力 AIOps 的企业级 Agentic 运维平台
```

也就是不再只是“RAG 问答 + 工具调用”，而是一个具备 **技能化封装、子 Agent 协作、生命周期拦截、安全权限、任务恢复、可观测性和评测闭环** 的工程化智能运维平台。

---

## 2. 当前项目为什么容易显得 Toy

### 2.1 技术点偏“会用大模型”，不够“会做 Agent 工程”

当前项目强调了：

```text
RAG
ReAct
Plan-Execute-Replan
MCP
Multi-Agent
SSE
```

这些是大模型应用的基础能力，但面试官可能会继续追问：

```text
Agent 如何控制工具调用？
Agent 如何避免无限循环？
工具调用前如何做权限校验？
RAG 检索质量如何评估？
长任务失败后如何恢复？
复杂诊断链路如何观测？
不同业务场景的 Prompt 如何复用和治理？
多 Agent 之间如何隔离上下文？
如何控制 Token 成本？
```

如果没有这些设计，项目会像一个演示系统，而不是企业级系统。

### 2.2 Multi-Agent 还只是“角色拆分”

原方案中有：

```text
Router Agent
Knowledge Agent
Planner Agent
Tool Agent
Diagnosis Agent
```

但还需要进一步体现：

```text
Agent 状态管理
Agent 生命周期事件
Agent 间上下文隔离
Agent 并行执行
工具权限控制
执行检查点
失败重试和恢复
审计日志
```

### 2.3 RAG 还只是“向量检索”

目前 RAG 虽然有文档上传、解析、清洗、切片、向量化、索引构建，但更高级的系统应该包含：

```text
Hybrid Search
Rerank
Query Rewrite
Tool Search
GraphRAG
Citation
RAG 评测
知识库版本治理
```

### 2.4 缺少 Agent 平台化抽象

真实企业项目不应该只是一组业务代码，而应该有平台化抽象：

```text
Skill Registry
Agent Registry
Tool Registry
Hook Registry
Prompt Registry
Workflow Runtime
Permission Engine
Evaluation Service
Observability Service
```

这些模块能明显提升项目的“工程含金量”。

---

## 3. 建议升级后的项目定位

建议将项目重新定义为：

> 面向电力 AIOps 场景的企业级 Agentic 运维平台，支持对话式智能问答、告警自动诊断、规程知识检索、日志分析、历史工单分析和诊断报告生成。平台采用 Skill + Multi-Agent + MCP + Hooks + Hybrid RAG 的架构，将电力运维中的知识、工具、流程和安全策略封装为可复用、可审计、可评测的 Agent 工作流。

新的项目关键词可以变成：

```text
Agentic AIOps
Skill-based Agent
Hook-driven Guardrails
Subagent Isolation
Hybrid RAG
Tool Search
MCP Tool Runtime
Checkpointable Workflow
Human-in-the-loop
Observability-first Agent
Evaluation-driven Optimization
```

---

## 4. 建议升级后的技术栈

### 4.1 简历版技术栈

```text
SpringBoot 3.x + SpringAI-Alibaba + Multi-Agent Workflow
+ Skill Registry + Hook Engine + MCP Tool Runtime
+ Hybrid RAG + Rerank + GraphRAG
+ Milvus / PGVector + Elasticsearch / OpenSearch
+ Kafka / RocketMQ + Redis + MySQL + MinIO
+ SSE / WebSocket + OpenTelemetry + Prometheus + Grafana
+ Spring Security / Sa-Token + RBAC + Human-in-the-loop
```

### 4.2 面试展开版技术栈

```text
后端框架：
SpringBoot 3.x、SpringAI-Alibaba、Spring Security / Sa-Token

Agent 运行时：
Multi-Agent Workflow、Agent State、Subagent、Hook Engine、Checkpoint、Task Runtime

Agent 能力封装：
Skill Registry、Prompt Registry、Tool Registry、Agent Registry、Workflow Template

RAG：
文档上传、解析清洗、父子切片、Embedding、Hybrid Search、Rerank、GraphRAG、Citation

向量与检索：
Milvus / PGVector、Elasticsearch / OpenSearch、BGE-M3、BGE-Reranker

工具调用：
MCP、Tool Search、Tool Schema、Tool Permission、Tool Audit

异步任务：
Kafka / RocketMQ、线程池、重试队列、死信队列、XXL-JOB

存储：
MySQL、Redis、MinIO、向量数据库

实时交互：
SSE、WebSocket

可观测性：
OpenTelemetry、Prometheus、Grafana、ELK / OpenSearch、Micrometer

安全：
RBAC、ABAC、JWT、数据脱敏、操作审计、Human-in-the-loop

评测：
RAG 评测集、Agent 任务评测、工具调用评测、专家反馈闭环
```

---

## 5. 核心升级建议一：引入 Skill 化能力包

### 5.1 为什么要引入 Skill

在 Claude Code / Agent Skills 的思想中，Skill 可以理解为一种可复用的“能力包”。它把某类任务所需的说明、流程、脚本、模板、参考资料封装到一个独立目录中，由 Agent 按需加载。

借鉴到电力运维项目中，Skill 可以用来封装不同业务场景的标准处理流程。

例如：

```text
主变油温异常诊断 Skill
开关柜局放异常诊断 Skill
高压室作业安全问答 Skill
缺陷工单检查 Skill
配网线路跳闸抢修 Skill
安规条款查询 Skill
红外测温分析 Skill
```

### 5.2 Skill 的设计形式

可以设计一个类似下面的目录结构：

```text
skills/
  transformer-oil-temp-diagnosis/
    SKILL.md
    prompts/
      planner_prompt.md
      diagnosis_prompt.md
    references/
      主变油温异常处理规程.md
      冷却系统排查手册.md
    tools.json
    output_schema.json
    examples/
      case_001.json
      case_002.json

  switchgear-partial-discharge/
    SKILL.md
    prompts/
    references/
    tools.json
    output_schema.json
    examples/
```

### 5.3 SKILL.md 示例

```markdown
# 主变油温异常诊断 Skill

## name

transformer-oil-temp-diagnosis

## description

用于处理主变压器油温异常升高、冷却器异常、温控器异常等告警场景。

## 适用场景

- 主变油温超过阈值
- 冷却器风机启动失败
- 温控器切换异常
- 高负荷下油温持续上升

## 推荐工具

- getDeviceStatus
- getDeviceLogs
- getDefectTickets
- searchSafetyRules

## 诊断流程

1. 解析告警字段。
2. 查询油温趋势。
3. 查询负荷趋势。
4. 查询冷却器运行状态。
5. 查询故障前后日志。
6. 查询历史缺陷。
7. 结合规程生成诊断建议。

## 输出格式

必须输出：
- 告警摘要
- 风险等级
- 可能原因
- 排查步骤
- 是否建议派单
- 是否需要人工确认
```

### 5.4 系统中如何使用 Skill

新的 Agent 流程可以变成：

```text
用户问题 / 告警事件
        ↓
Router Agent 识别业务场景
        ↓
Skill Selector 选择合适 Skill
        ↓
加载 Skill 中的流程、Prompt、工具清单和输出 Schema
        ↓
Planner Agent 根据 Skill 生成执行计划
        ↓
Tool Agent 调用工具
        ↓
Diagnosis Agent 按 Skill 输出诊断结果
```

### 5.5 项目亮点表达

可以在简历中写：

```text
引入 Skill 化能力包设计，将主变油温异常、开关柜局放、线路跳闸、安规问答、工单检查等业务流程封装为可复用 Skill，每个 Skill 包含场景说明、Prompt 模板、推荐工具、输出 Schema 和案例样本，实现 Agent 能力的模块化复用和版本化管理。
```

面试中可以这样讲：

> 我们没有把所有业务规则都写死在 Prompt 里，而是借鉴 Agent Skills 的思想，把不同运维场景封装成 Skill。比如主变油温异常诊断 Skill 中定义了适用场景、推荐工具、诊断步骤和输出格式。Router Agent 识别出用户问题或告警类型后，会选择对应 Skill，再由 Planner Agent 按 Skill 中的流程执行。这让系统扩展新场景时不需要改核心代码，只需要新增一个 Skill 包。

---

## 6. 核心升级建议二：引入 Hooks 作为 Agent 生命周期拦截器

### 6.1 为什么需要 Hooks

Agent 在执行过程中会调用工具、生成结论、访问外部系统。真实业务中必须在关键节点加控制点。

借鉴 Claude Code 的 Hooks 思想，可以在 Agent 生命周期中加入拦截器，用于：

```text
权限校验
参数校验
敏感操作拦截
数据脱敏
Prompt 注入检测
工具调用审计
高风险建议人工确认
结果后处理
异常告警
```

### 6.2 电力运维 Agent 的 Hook 事件设计

可以设计以下 Hook：

| Hook 名称 | 触发时机 | 用途 |
|---|---|---|
| PreRouteHook | Router Agent 路由前 | 输入清洗、意图预校验 |
| PostRouteHook | Router Agent 路由后 | 检查路由结果是否合理 |
| PreRagHook | RAG 检索前 | Query 改写、元数据过滤 |
| PostRagHook | RAG 检索后 | 召回质量检查、上下文压缩 |
| PreToolUseHook | 工具调用前 | 权限校验、参数校验、敏感操作拦截 |
| PostToolUseHook | 工具调用后 | 结果脱敏、异常检测、审计记录 |
| PreDiagnosisHook | 生成诊断前 | 检查证据是否充足 |
| PostDiagnosisHook | 生成诊断后 | JSON Schema 校验、风险等级检查 |
| HumanApprovalHook | 高风险建议触发时 | 人工确认 |
| AuditHook | 每个关键节点 | 记录审计日志 |

### 6.3 PreToolUseHook 示例

```java
public HookDecision beforeToolUse(ToolRequest request, UserContext user) {
    if (!permissionService.canUseTool(user.getRole(), request.getToolName())) {
        return HookDecision.deny("当前用户无权调用该工具");
    }

    if (request.getToolName().equals("createEmergencyTicket")
        && !user.hasPermission("TICKET_CREATE")) {
        return HookDecision.requireHumanApproval("创建紧急工单需要人工确认");
    }

    if (request.containsSensitiveDevice()) {
        request = dataMaskingService.mask(request);
    }

    return HookDecision.allow(request);
}
```

### 6.4 高风险建议拦截

电力场景中，以下操作不能让 Agent 直接执行：

```text
停电
降负荷
远程控制设备
隔离线路
派发紧急工单
修改设备状态
删除工单
关闭告警
```

可以通过 Hook 实现：

```text
Agent 生成高风险建议
        ↓
PostDiagnosisHook 检测风险等级
        ↓
HumanApprovalHook 发起人工确认
        ↓
值班负责人确认
        ↓
系统才允许进入业务流转
```

### 6.5 项目亮点表达

```text
设计 Hook Engine 作为 Agent 生命周期拦截器，在路由、RAG 检索、工具调用、诊断生成等关键节点注入权限校验、参数校验、数据脱敏、审计记录和高风险操作人工确认，提升 Agent 工具调用安全性和可控性。
```

---

## 7. 核心升级建议三：Subagent 隔离上下文与并行分析

### 7.1 为什么需要 Subagent

复杂诊断任务中，不同分析方向需要不同上下文。如果所有内容都塞进一个主 Agent，很容易导致：

```text
上下文过长
Token 成本高
不同任务相互干扰
工具调用链路混乱
诊断过程不可控
```

借鉴 Claude Code 的 Subagent 思想，可以将复杂任务拆给多个专用子 Agent，每个子 Agent 使用独立上下文完成局部任务。

### 7.2 电力运维场景下的 Subagent 设计

| Subagent | 职责 |
|---|---|
| Regulation Subagent | 检索安规和运维规程 |
| Metrics Subagent | 查询设备实时指标和趋势 |
| Log Subagent | 分析故障前后日志 |
| Ticket Subagent | 查询历史工单和相似缺陷 |
| Risk Review Subagent | 检查诊断建议风险 |
| Report Subagent | 生成最终报告 |
| Evaluation Subagent | 对答案进行自检和评分 |

### 7.3 并行执行流程

以主变油温异常为例：

```text
Main Agent 接收告警
        ↓
Planner Agent 生成诊断计划
        ↓
并行启动多个 Subagent
   ├── Regulation Subagent：检索油温异常处理规程
   ├── Metrics Subagent：查询油温、负荷、环境温度趋势
   ├── Log Subagent：分析冷却器和温控器日志
   └── Ticket Subagent：查询历史缺陷工单
        ↓
Main Agent 汇总子 Agent 结果
        ↓
Risk Review Subagent 进行风险复核
        ↓
Report Subagent 生成结构化诊断报告
```

### 7.4 好处

```text
降低主 Agent 上下文压力
支持并行执行，提高响应速度
每个 Subagent 工具权限更小
每个 Subagent Prompt 更聚焦
便于观测每个分析分支的耗时和结果
```

### 7.5 项目亮点表达

```text
借鉴 Subagent 上下文隔离思想，将复杂故障诊断拆分为规程检索、监控指标分析、日志分析、历史工单分析和风险复核等专用子 Agent 并行执行，主 Agent 仅负责计划编排和结果汇总，降低上下文污染并提升诊断效率。
```

---

## 8. 核心升级建议四：Agent 工作流检查点与可恢复执行

### 8.1 为什么需要 Checkpoint

电力告警诊断是长链路任务，可能包含：

```text
RAG 检索
多次工具调用
多个 Subagent 并行分析
大模型生成诊断
人工确认
工单流转
```

任何一步失败，不能让整个任务从头开始。

因此需要设计 Checkpoint。

### 8.2 Checkpoint 保存内容

```text
taskId
sessionId
currentStep
selectedSkill
agentState
planSteps
completedSteps
ragResults
toolResults
subagentResults
diagnosisDraft
approvalStatus
errorMessage
```

### 8.3 状态流转示例

```text
RECEIVED：任务已接收
ROUTED：已完成路由
SKILL_SELECTED：已选择 Skill
PLANNED：已生成计划
RAG_DONE：已完成知识检索
TOOLS_RUNNING：工具调用中
TOOLS_DONE：工具调用完成
SUBAGENTS_DONE：子 Agent 分析完成
DIAGNOSIS_GENERATED：已生成诊断
WAITING_APPROVAL：等待人工确认
COMPLETED：任务完成
FAILED：任务失败
RETRYING：重试中
```

### 8.4 恢复策略

```text
RAG 失败：重新检索
工具调用失败：按重试策略重试
部分 Subagent 失败：只重跑失败分支
模型生成失败：基于已有上下文重新生成
人工审批超时：通知值班负责人
任务中断：从最近 checkpoint 恢复
```

### 8.5 项目亮点表达

```text
为 Agent 诊断任务设计 Checkpoint 机制，将路由结果、Skill 选择、执行计划、RAG 结果、工具返回、子 Agent 输出和审批状态持久化，支持长耗时诊断任务失败重试、断点恢复和审计追溯。
```

---

## 9. 核心升级建议五：Tool Search 与 MCP 工具治理

### 9.1 当前 MCP 的问题

如果工具很少，可以直接写死工具列表。但真实电力系统中，工具可能很多：

```text
查询设备状态
查询实时告警
查询历史告警
查询保护动作
查询冷却器日志
查询温控器日志
查询工单
查询台账
查询安规
查询备品备件
查询人员排班
查询气象
查询 GIS 地图
查询巡检记录
```

如果每次都把所有工具描述塞给模型，会造成：

```text
Prompt 过长
Token 成本高
工具选择混乱
误调用风险增加
```

### 9.2 建议引入 Tool Search

设计 Tool Registry：

```text
toolId
toolName
description
inputSchema
outputSchema
tags
permissionLevel
riskLevel
ownerSystem
examples
```

工具选择流程：

```text
用户问题 / 告警事件
        ↓
Router Agent 识别任务类型
        ↓
Tool Search 根据语义和标签召回候选工具
        ↓
Permission Engine 过滤无权限工具
        ↓
Planner Agent 只看到候选工具
        ↓
Tool Agent 调用工具
```

### 9.3 工具治理能力

```text
工具注册
工具版本管理
工具权限配置
工具调用审计
工具风险等级
工具超时设置
工具重试策略
工具降级策略
工具输出 Schema 校验
```

### 9.4 项目亮点表达

```text
设计 MCP Tool Registry 和 Tool Search 机制，对监控、日志、工单、台账、安规等工具进行统一注册、权限控制和语义检索；Agent 执行时先根据任务意图召回候选工具，再进行权限过滤和调用，避免大规模工具列表导致上下文膨胀和误调用。
```

---

## 10. 核心升级建议六：Memory / Context 分层治理

### 10.1 为什么需要上下文分层

智能问答和诊断过程中会产生多类上下文：

```text
用户会话上下文
当前设备上下文
当前告警上下文
当前任务状态
用户偏好
班组信息
站点信息
历史诊断记录
```

如果全部放在一个上下文中，会导致混乱。

### 10.2 建议设计四层 Memory

| Memory 层级 | 内容 | 生命周期 |
|---|---|---|
| Session Memory | 当前对话上下文 | 单次会话 |
| Task Memory | 当前诊断任务状态 | 一个任务周期 |
| Domain Memory | 电力业务规则、常用术语 | 长期 |
| User / Role Memory | 用户角色、常用站点、权限 | 长期 |

### 10.3 示例

```text
Session Memory：
用户刚才问的是 1号主变油温异常。

Task Memory：
当前任务已经完成油温趋势查询，正在等待日志分析结果。

Domain Memory：
主变油温异常通常需要检查负荷、冷却器、油位、温控器。

User Memory：
当前用户是变电运维人员，只能查询设备状态和工单，不能创建紧急停电操作。
```

### 10.4 项目亮点表达

```text
设计分层上下文记忆机制，将会话记忆、任务记忆、领域记忆和用户角色记忆分离管理，支持多轮追问、任务恢复、权限判断和个性化设备查询，避免长上下文污染。
```

---

## 11. 核心升级建议七：Agent Observability 全链路追踪

### 11.1 为什么需要可观测性

Agent 系统中一次回答可能涉及：

```text
Router Agent
Skill Selector
RAG
Rerank
Planner Agent
多个 Tool Call
多个 Subagent
Hooks
Diagnosis Agent
SSE 输出
```

如果没有可观测性，线上问题很难排查。

### 11.2 借鉴 OpenTelemetry Trace 思想

建议每次用户请求或告警任务生成一个 Trace：

```text
traceId = taskId / sessionId
```

每个节点生成 Span：

```text
router.span
skill_selector.span
rag_retrieval.span
rerank.span
planner.span
tool.getDeviceStatus.span
tool.getDeviceLogs.span
subagent.log_analysis.span
hook.pre_tool_use.span
diagnosis.span
sse_push.span
```

### 11.3 每个 Span 记录内容

```text
节点名称
输入摘要
输出摘要
耗时
Token 消耗
模型名称
工具名称
工具入参摘要
工具调用状态
错误信息
重试次数
风险等级
```

### 11.4 监控指标

```text
Agent 任务成功率
平均诊断耗时
RAG 检索耗时
RAG 召回命中率
Rerank 耗时
工具调用成功率
工具平均耗时
Subagent 平均耗时
Hook 拦截次数
人工审批次数
Token 消耗
SSE 连接数
```

### 11.5 项目亮点表达

```text
基于 OpenTelemetry 思想设计 Agent 全链路可观测性，为每次问答或告警诊断生成 Trace，并记录 Router、RAG、Tool、Hook、Subagent、Diagnosis 等节点 Span，实现模型耗时、工具成功率、Token 消耗和诊断任务状态的可视化监控。
```

---

## 12. 核心升级建议八：权限模式与 Human-in-the-loop

### 12.1 为什么需要权限模式

电力运维场景涉及安全生产，Agent 不能无限制调用工具或执行操作。

建议设计权限模式：

```text
ReadOnly：只允许查询
Diagnose：允许诊断和生成建议
TicketSuggest：允许建议派单，但不直接创建
TicketCreateWithApproval：人工确认后创建工单
Admin：知识库和工具配置管理
```

### 12.2 权限控制点

```text
用户登录鉴权
角色权限控制
数据权限过滤
工具调用权限
高风险操作审批
知识库文档上传审核
工具调用审计
```

### 12.3 Human-in-the-loop 场景

以下情况必须进入人工确认：

```text
建议停电
建议降负荷
建议隔离设备
建议创建紧急工单
建议远程控制设备
知识库文档发布上线
工具权限变更
```

### 12.4 项目亮点表达

```text
设计面向电力安全生产的权限模式和 Human-in-the-loop 机制，Agent 默认只负责查询、分析和建议生成；对于停电、降负荷、紧急派单等高风险操作，通过 Hook 触发人工审批，审批通过后才允许业务系统执行。
```

---

## 13. 核心升级建议九：Agent 插件化与领域能力市场

### 13.1 为什么需要插件化

随着系统扩展，可能会新增很多能力：

```text
主变诊断
开关柜诊断
配网线路诊断
安规问答
无人机巡检
红外图谱分析
工单质检
班组排班
备品备件推荐
```

如果每个能力都改核心代码，系统会越来越难维护。

### 13.2 建议设计 Plugin

一个插件可以包含：

```text
Skills
Subagents
Hooks
MCP 工具配置
Prompt 模板
输出 Schema
评测样例
```

插件目录示例：

```text
plugins/
  transformer-diagnosis-plugin/
    skills/
    agents/
    hooks/
    mcp-tools/
    prompts/
    schemas/
    evals/

  switchgear-inspection-plugin/
    skills/
    agents/
    hooks/
    mcp-tools/
    prompts/
    schemas/
    evals/
```

### 13.3 插件管理功能

```text
插件安装
插件启用 / 禁用
插件版本管理
插件权限配置
插件依赖检查
插件灰度发布
插件回滚
```

### 13.4 项目亮点表达

```text
借鉴 Agent Plugin 思想，将电力不同设备和业务场景封装为插件包，每个插件包含 Skill、Subagent、Hook、MCP 工具配置、Prompt 模板和评测样例，支持按场景安装、启用、灰度和回滚，提升系统扩展性。
```

---

## 14. 核心升级建议十：从普通 RAG 升级为 GraphRAG + Hybrid RAG

### 14.1 为什么需要 GraphRAG

电力运维中存在大量实体关系：

```text
设备 - 部件
设备 - 站点
设备 - 告警
告警 - 原因
原因 - 检查项
检查项 - 工具
故障 - 工单
故障 - 安规
设备型号 - 厂家手册
```

单纯向量检索不能很好地表达这些关系。

### 14.2 建议构建设备故障知识图谱

实体包括：

```text
变电站
设备
部件
传感器
告警类型
故障现象
故障原因
排查步骤
安规条款
历史工单
工具
```

关系包括：

```text
设备 属于 变电站
设备 包含 部件
告警 发生于 设备
告警 可能由 故障原因 导致
故障原因 对应 排查步骤
排查步骤 需要调用 工具
故障处理 需要遵守 安规条款
历史工单 涉及 设备
```

### 14.3 GraphRAG 检索流程

```text
用户问题 / 告警事件
        ↓
实体识别：设备、告警类型、部件
        ↓
图谱扩展：找到相关部件、原因、检查项、工具
        ↓
Hybrid Search：召回相关规程和案例
        ↓
Rerank：重排序
        ↓
融合图谱路径和文档片段
        ↓
生成诊断计划和答案
```

### 14.4 示例

输入：

```text
1号主变油温异常升高
```

图谱扩展：

```text
1号主变
  → 冷却系统
  → 风机
  → 油泵
  → 温控器
  → 油温传感器
  → 可能原因：负荷过高、风机故障、温控器异常、油位异常
  → 需要工具：油温趋势查询、负荷查询、冷却器日志查询、历史工单查询
```

### 14.5 项目亮点表达

```text
在 Hybrid RAG 基础上引入设备故障知识图谱，构建设备、部件、告警、故障原因、排查步骤、安规条款和工具之间的关系，通过 GraphRAG 辅助 Planner Agent 生成更完整的排查路径。
```

---

### 



---

## 16. 核心升级建议十二：评测驱动的 Agent 优化闭环

### 16.1 为什么需要评测

大模型项目面试经常会被追问：

```text
你怎么证明回答准确？
你怎么证明 RAG 有效？
你怎么证明工具调用选对了？
你怎么优化 Prompt？
```

### 16.2 建议建立 Eval 数据集

按场景建立测试集：

```text
安规问答评测集
设备状态查询评测集
告警诊断评测集
日志分析评测集
历史工单分析评测集
复杂多轮对话评测集
```

### 16.3 评测指标

#### RAG 指标

```text
Recall@K
MRR
Context Precision
Context Relevance
Citation Accuracy
Answer Faithfulness
```

#### Agent 指标

```text
任务完成率
工具选择准确率
工具调用成功率
计划步骤完整率
Replan 合理率
诊断建议采纳率
人工纠正率
平均响应时间
平均 Token 消耗
```

#### 安全指标

```text
高风险操作拦截率
越权工具调用拦截率
Prompt 注入拦截率
敏感数据泄露率
```

### 16.4 反馈闭环

```text
用户问答 / 告警诊断
        ↓
用户点赞 / 点踩
        ↓
专家修正
        ↓
生成标注样本
        ↓
加入 Eval 集
        ↓
对比不同 Prompt / RAG / Agent 工作流效果
        ↓
灰度上线优化版本
```

### 16.5 项目亮点表达

```text
建设 RAG 与 Agent 评测闭环，针对安规问答、告警诊断、日志分析和工单查询构建评测集，量化检索召回率、答案忠实度、工具选择准确率、诊断建议采纳率和高风险操作拦截率，并将专家反馈回流到 Prompt、Skill 和检索策略优化中。
```

---

## 17. 升级后的系统模块

建议将系统模块调整为：

```text
1. 智能问答模块
2. 告警接入模块
3. Agent Workflow Runtime
4. Skill Registry 能力包管理模块
5. Subagent 编排模块
6. Hook Engine 生命周期拦截模块
7. MCP Tool Runtime 工具运行时
8. Tool Search 工具检索模块
9. RAG 文档上传与知识库构建模块
10. Hybrid RAG / GraphRAG 检索模块
11. 日志检索与时间线分析模块
12. 历史工单相似案例分析模块
13. Checkpoint 任务恢复模块
14. Human-in-the-loop 审批模块
15. SSE / WebSocket 实时推送模块
16. 权限与审计模块
17. 可观测性监控模块
18. Eval 评测与反馈闭环模块
19. 插件化领域能力管理模块
```

---

## 18. 升级后的核心链路

### 18.1 对话式问答链路

```text
用户自然语言问题
        ↓
PreRouteHook 输入清洗与安全检查
        ↓
Chat Agent 加载会话上下文
        ↓
Router Agent 识别意图
        ↓
Skill Selector 选择业务 Skill
        ↓
Tool Search / RAG Search 确定能力来源
        ↓
Planner Agent 生成执行计划
        ↓
按需启动 Subagents
        ↓
PreToolUseHook 工具权限校验
        ↓
Tool Agent 调用 MCP 工具
        ↓
PostToolUseHook 结果脱敏与审计
        ↓
Diagnosis / Answer Agent 生成结构化回答
        ↓
PostDiagnosisHook 风险检查
        ↓
SSE 流式返回
        ↓
记录 Trace、Eval 和用户反馈
```

### 18.2 告警自动诊断链路

```text
监控平台产生结构化告警
        ↓
告警接入服务校验并落库
        ↓
Kafka / RocketMQ 投递诊断任务
        ↓
Agent Worker 消费任务
        ↓
Checkpoint：RECEIVED
        ↓
Alarm Agent 解析告警
        ↓
Skill Selector 选择诊断 Skill
        ↓
Planner Agent 生成排查计划
        ↓
并行启动 Regulation / Metrics / Log / Ticket Subagents
        ↓
RAG + GraphRAG 检索规程和图谱路径
        ↓
MCP 工具查询设备状态、日志、工单
        ↓
Checkpoint：TOOLS_DONE
        ↓
Diagnosis Agent 生成诊断建议
        ↓
Risk Review Subagent 风险复核
        ↓
HumanApprovalHook 判断是否需要人工确认
        ↓
结果写入数据库
        ↓
SSE / WebSocket 推送前端
        ↓
Trace + Metrics + Eval 记录
```

---

## 19. 升级后的简历项目描述

### 19.1 项目名称

```text
面向电力 AIOps 的智能运维 Agentic 平台
```

### 19.2 项目介绍

```text
针对电力巡检、监控告警处理和设备故障排障中存在的知识分散、跨系统查询复杂、故障定位依赖经验等问题，设计并实现了一套面向电力 AIOps 的智能运维 Agentic 平台。

系统提供对话式智能问答和告警驱动自动诊断双入口，支持运维人员通过自然语言完成安规查询、设备状态查询、日志分析、历史工单检索和故障排查，也支持接收监控平台推送的结构化告警事件，自动完成告警解析、Skill 选择、知识库检索、排查计划生成、工具调用、子 Agent 并行分析、风险复核和运维建议生成。

项目借鉴 Agent Skills、Subagent、Hooks、MCP、Checkpoint 和 Observability 等最新 Agent 工程化思想，构建 Skill Registry、Hook Engine、MCP Tool Runtime、Hybrid RAG / GraphRAG、Agent Workflow Runtime 和 Eval 评测闭环，实现从“用户提问 / 告警触发”到“可审计、可恢复、可评测的诊断建议输出”的企业级智能运维闭环。
```

### 19.3 技术架构

```text
SpringBoot 3.x + SpringAI-Alibaba + Multi-Agent Workflow
+ Skill Registry + Hook Engine + MCP Tool Runtime
+ Hybrid RAG + GraphRAG + Rerank
+ Milvus / PGVector + Elasticsearch / OpenSearch
+ Kafka / RocketMQ + Redis + MySQL + MinIO
+ SSE / WebSocket + OpenTelemetry + Prometheus + Grafana
+ RBAC + Human-in-the-loop + Eval
```

---

## 20. 升级后的职责描述

### 20.1 完整版职责描述

```text
1. 基于 SpringAI-Alibaba 设计电力智能运维 Agentic 平台，构建对话式智能问答和告警自动诊断双入口，支持安规问答、设备状态查询、日志分析、历史工单分析和故障诊断等场景。

2. 参考 Agent Skills 思想设计 Skill Registry，将主变油温异常、开关柜局放、线路跳闸、安规问答、工单检查等业务流程封装为可复用 Skill，包含场景说明、Prompt 模板、推荐工具、输出 Schema 和评测样例，实现业务能力模块化复用。

3. 设计 Multi-Agent Workflow Runtime，拆分 Router、Chat、Planner、Knowledge、Tool、Log Analysis、Ticket、Risk Review、Diagnosis 等 Agent，并通过 AgentState、条件路由、Checkpoint 和 Replan 机制管理复杂任务执行链路。

4. 引入 Subagent 上下文隔离机制，将复杂故障诊断拆分为规程检索、指标分析、日志分析、历史工单分析和风险复核等专用子 Agent 并行执行，主 Agent 负责任务编排和结果汇总，降低上下文污染和 Token 消耗。

5. 设计 Hook Engine 作为 Agent 生命周期拦截器，在路由、RAG 检索、工具调用、诊断生成等关键节点注入权限校验、参数校验、数据脱敏、Prompt 注入检测、审计记录和高风险操作人工确认。

6. 负责 RAG 文档上传与知识库构建服务开发，支持电力安规、设备手册、巡检标准和故障案例文档上传，完成文档解析、文本清洗、父子切片、Embedding 向量化、向量库存储、索引构建和文档版本治理。

7. 优化 RAG 检索链路，采用 Hybrid Search（向量检索 + BM25 + 元数据过滤）与 Reranker 重排序，并结合设备故障知识图谱实现 GraphRAG，提升设备编号、专业术语、安规条款和复杂故障路径的召回准确率。

8. 设计 MCP Tool Runtime 和 Tool Search 机制，对监控告警、设备状态、设备日志、历史工单、安规查询、设备台账等工具进行统一注册、语义检索、权限过滤、Schema 校验、调用审计和失败重试。

9. 基于 Plan-Execute-Replan 实现复杂故障诊断闭环，构建“告警解析 → Skill 选择 → 知识检索 → 排查计划 → 工具调用 → 子 Agent 分析 → 动态重规划 → 风险复核 → 建议生成”的自动化排障流程。

10. 引入 Kafka / RocketMQ 对告警诊断任务进行异步解耦，配合 Checkpoint 机制实现任务状态持久化、失败重试、断点恢复和死信处理，避免大模型长耗时任务阻塞告警接入接口。

11. 接入 Elasticsearch / OpenSearch 存储设备运行日志，由 Log Analysis Subagent 基于设备 ID、告警时间窗口和关键词检索异常日志，重建故障时间线并分析异常链路。

12. 设计权限模式和 Human-in-the-loop 机制，Agent 默认只负责查询、分析和建议生成；对于停电、降负荷、紧急派单等高风险操作，通过 Hook 触发人工审批，审批通过后才允许业务系统流转。

13. 基于 OpenTelemetry 思想建设 Agent 全链路可观测性，为 Router、Skill Selector、RAG、Tool、Hook、Subagent、Diagnosis 等节点记录 Trace 和 Span，并通过 Prometheus + Grafana 监控模型耗时、Token 消耗、工具成功率和诊断任务成功率。

14. 建设 RAG 与 Agent 评测闭环，针对安规问答、告警诊断、日志分析和工单查询构建 Eval 数据集，量化检索召回率、答案忠实度、工具选择准确率、诊断建议采纳率和高风险操作拦截率。
```

### 20.2 精简版职责描述

```text
1. 基于 SpringAI-Alibaba 设计面向电力 AIOps 的 Agentic 运维平台，提供对话式智能问答和告警自动诊断双入口，支撑安规问答、设备状态查询、日志分析、工单查询和故障诊断场景。

2. 借鉴 Agent Skills 思想设计 Skill Registry，将主变油温异常、开关柜局放、线路跳闸、安规问答等业务流程封装为可复用 Skill，统一管理 Prompt、推荐工具、输出 Schema 和评测样例。

3. 设计 Multi-Agent Workflow Runtime，拆分 Router、Planner、Knowledge、Tool、Log Analysis、Ticket、Risk Review、Diagnosis 等 Agent，通过 AgentState、条件路由、Checkpoint 和 Replan 管理复杂诊断链路。

4. 引入 Subagent 上下文隔离与并行分析机制，将规程检索、指标分析、日志分析、历史工单分析和风险复核拆分为专用子 Agent，降低上下文污染并提升诊断效率。

5. 设计 Hook Engine，在工具调用、RAG 检索、诊断生成等关键节点实现权限校验、参数校验、数据脱敏、审计记录和高风险操作人工确认，提升 Agent 执行安全性。

6. 负责 RAG 知识库构建和检索优化，支持文档上传、解析清洗、父子切片、Embedding、向量库存储和索引构建，并采用 Hybrid Search + Rerank + GraphRAG 提升检索准确率。

7. 基于 MCP Tool Runtime 和 Tool Search 统一接入监控告警、设备状态、设备日志、历史工单和安规查询工具，实现工具注册、语义检索、权限过滤、Schema 校验和调用审计。

8. 引入 Kafka / RocketMQ 实现告警诊断任务异步解耦，配合 Checkpoint 支持任务失败重试和断点恢复，通过 SSE / WebSocket 实时推送诊断进度。

9. 基于 OpenTelemetry + Prometheus + Grafana 建设 Agent 可观测性，跟踪 Router、RAG、Tool、Hook、Subagent、Diagnosis 等节点耗时、Token 消耗和工具调用成功率。

10. 建设 RAG 与 Agent Eval 评测闭环，量化检索召回率、答案忠实度、工具选择准确率、诊断建议采纳率和高风险操作拦截率，并将专家反馈回流优化 Skill、Prompt 和检索策略。
```

---

## 21. 面试重点讲法

### 21.1 面试官问：你这个项目有什么新东西？

可以回答：

```text
这个项目不是简单的 RAG 问答系统，而是借鉴了 Claude Code 这类 Agentic 系统的工程化思想。

第一，我们引入了 Skill Registry，把不同电力运维场景封装成可复用的 Skill，比如主变油温异常诊断、开关柜局放诊断、线路跳闸抢修等。每个 Skill 包含 Prompt、工具清单、输出 Schema 和案例样本。

第二，我们使用 Subagent 做上下文隔离和并行分析。复杂故障诊断会拆给规程检索、指标分析、日志分析、历史工单分析和风险复核等子 Agent，主 Agent 负责计划和汇总。

第三，我们设计了 Hook Engine，在工具调用前后、RAG 检索前后、诊断生成后做权限校验、参数校验、数据脱敏、审计和高风险操作人工确认。

第四，我们通过 MCP Tool Runtime 和 Tool Search 统一管理外部系统工具，避免所有工具描述都塞进上下文，降低 Token 成本和误调用风险。

第五，我们引入 Checkpoint、OpenTelemetry 和 Eval 机制，让 Agent 任务可恢复、可观测、可评测，更像真实企业级 Agent 平台。
```

### 21.2 面试官问：Skill 和 Agent 有什么区别？

可以回答：

```text
Agent 是执行单元，负责推理、规划或调用工具；Skill 是能力包，描述某类任务应该怎么做。

比如 Diagnosis Agent 是一个通用诊断 Agent，但当它处理主变油温异常时，会加载“主变油温异常诊断 Skill”。这个 Skill 里定义了适用场景、推荐工具、排查步骤、Prompt 模板和输出 Schema。

所以 Skill 更像可复用的业务 SOP 和知识包，Agent 更像执行这些 SOP 的智能执行器。这样做的好处是新增业务场景时不需要改 Agent 核心代码，只需要新增 Skill。
```

### 21.3 面试官问：Hooks 在你的系统中解决什么问题？

可以回答：

```text
Hooks 主要解决 Agent 可控性和安全性问题。

在我们的系统里，Agent 生命周期中有多个 Hook 点，比如 PreToolUseHook、PostToolUseHook、PostDiagnosisHook 和 HumanApprovalHook。

工具调用前，PreToolUseHook 会做权限校验、参数校验和风险判断；工具调用后，PostToolUseHook 会做结果脱敏、异常检测和审计记录；诊断结果生成后，PostDiagnosisHook 会检查是否涉及停电、降负荷、紧急派单等高风险建议，如果涉及就触发 HumanApprovalHook，要求人工确认。

这样可以避免 Agent 越权调用工具或直接执行高风险操作。
```

### 21.4 面试官问：为什么要用 Subagent？

可以回答：

```text
复杂故障诊断会涉及多个方向的信息，比如规程、实时指标、日志、历史工单和风险复核。如果全部放到一个 Agent 上下文里，会导致上下文过长、Token 成本高，而且不同分析任务互相干扰。

所以我们将它拆成多个 Subagent，每个子 Agent 使用独立上下文和最小工具权限。比如 Log Analysis Subagent 只负责日志检索和时间线分析，Ticket Subagent 只负责历史工单分析，Risk Review Subagent 只负责风险复核。主 Agent 负责计划编排和结果汇总。

这样既提升了并行效率，也降低了上下文污染和误调用风险。
```

### 21.5 面试官问：怎么保证 Agent 任务稳定执行？

可以回答：

```text
我们从三个方面保证稳定性。

第一是 Checkpoint。每个诊断任务会持久化当前步骤、执行计划、RAG 结果、工具结果、子 Agent 输出和审批状态。如果某一步失败，可以从最近 checkpoint 恢复，而不是从头执行。

第二是 Hook 和权限控制。工具调用前会做参数校验、权限校验和风险判断，避免错误调用。

第三是可观测性。每个 Agent 节点、工具调用和 Hook 都会记录 Trace Span，包括耗时、Token、输入输出摘要和错误信息。通过 Prometheus 和 Grafana 可以看到任务成功率、工具成功率和模型耗时。
```

---

## 22. 最推荐的最终项目名称

建议使用下面这个名称：

```text
面向电力 AIOps 的 Agentic 智能运维平台
```

如果想更偏技术，可以写：

```text
基于 Hybrid RAG 与 Multi-Agent Workflow 的电力 AIOps 智能诊断平台
```

如果想更偏工程化，可以写：

```text
基于 Skill 化 Agent 工作流的电力智能运维平台
```

最推荐：

```text
面向电力 AIOps 的 Agentic 智能运维平台
```

这个名称比“基于大模型的电力智能运维 Agent 系统”更高级，能体现：

```text
AIOps
Agentic
平台化
智能运维
工程落地
```

---

## 23. 最终建议

如果你是为了面试包装这个项目，我建议不要把所有高级技术都堆到简历上，而是选择其中最能讲清楚的 6 个重点：

```text
1. Skill Registry
2. Hook Engine
3. Subagent 隔离和并行分析
4. Hybrid RAG + Rerank + GraphRAG
5. MCP Tool Runtime + Tool Search
6. Checkpoint + Observability + Eval
```

这 6 个点足够让项目从普通大模型应用升级成企业级 Agent 项目。

最终简历技术栈可以控制在一行：

```text
SpringBoot 3.x + SpringAI-Alibaba + Multi-Agent Workflow + Skill Registry + Hook Engine + MCP + Hybrid RAG + GraphRAG + Kafka + Redis + OpenTelemetry
```

这样既不会显得乱堆技术，又能体现工程深度。
