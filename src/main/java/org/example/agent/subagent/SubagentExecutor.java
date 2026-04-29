package org.example.agent.subagent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class SubagentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SubagentExecutor.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private ToolCallbackProvider tools;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static final Map<String, String> SUBAGENT_PROMPTS = Map.of(
            "regulation", "你是安规查询子Agent。请查询与当前告警相关的安规条款和安全要求。",
            "metrics", "你是设备状态查询子Agent。请查询设备的实时运行状态和历史趋势。",
            "log", "你是日志分析子Agent。请查询设备运行日志，分析异常事件。",
            "ticket", "你是工单分析子Agent。请查询设备历史缺陷工单，分析关联性。",
            "risk_review", "你是风险复核子Agent。请审核诊断建议的安全性和可行性。"
    );

    public List<SubagentTask> executeParallel(List<SubagentTask> tasks) {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();

        List<Future<SubagentTask>> futures = new ArrayList<>();

        for (SubagentTask task : tasks) {
            futures.add(executorService.submit(() -> executeSingle(dashScopeApi, task)));
        }

        List<SubagentTask> results = new ArrayList<>();
        for (Future<SubagentTask> future : futures) {
            try {
                results.add(future.get(120, TimeUnit.SECONDS));
            } catch (Exception e) {
                logger.error("子Agent执行超时或失败", e);
                SubagentTask failedTask = tasks.get(results.size());
                failedTask.setStatus("FAILED");
                failedTask.setResult("执行失败: " + e.getMessage());
                results.add(failedTask);
            }
        }

        return results;
    }

    private SubagentTask executeSingle(DashScopeApi dashScopeApi, SubagentTask task) {
        long startTime = System.currentTimeMillis();
        try {
            String prompt = SUBAGENT_PROMPTS.getOrDefault(task.getSubagentName(),
                    "你是电力运维子Agent，请完成分配的任务。");

            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(DashScopeChatOptions.builder()
                            .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                            .withTemperature(0.3)
                            .withMaxToken(2000)
                            .withTopP(0.8)
                            .build())
                    .build();

            ReactAgent agent = ReactAgent.builder()
                    .name(task.getSubagentName() + "_subagent")
                    .model(chatModel)
                    .systemPrompt(prompt)
                    .tools(tools.getToolCallbacks())
                    .build();

            var response = agent.call(task.getInput());
            task.setResult(response.getText());
            task.setStatus("COMPLETED");
        } catch (Exception e) {
            logger.error("子Agent执行失败: {}", task.getSubagentName(), e);
            task.setStatus("FAILED");
            task.setResult("执行失败: " + e.getMessage());
        }

        task.setDurationMs(System.currentTimeMillis() - startTime);
        return task;
    }
}
