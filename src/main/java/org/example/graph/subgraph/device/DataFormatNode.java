package org.example.graph.subgraph.device;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DataFormatNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(DataFormatNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String result = state.value("execution_result").map(Object::toString).orElse("");
        logger.info("DataFormatNode: 数据格式化");

        String formatted = result;
        if (!result.contains("查询失败") && !result.contains("执行失败")) {
            formatted = result + "\n\n📌 以上数据来自设备监控系统，如需进一步分析请告知。";
        }

        return Map.of("final_response", formatted);
    }
}
