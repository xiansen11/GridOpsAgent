package org.example.config;

import io.milvus.client.MilvusServiceClient;
import org.example.client.MilvusClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
public class MilvusConfig {

    private static final Logger logger = LoggerFactory.getLogger(MilvusConfig.class);

    @Value("${milvus.enabled:false}")
    private boolean milvusEnabled;

    @Autowired(required = false)
    private MilvusClientFactory milvusClientFactory;

    private MilvusServiceClient milvusClient;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        if (!milvusEnabled) {
            logger.info("Milvus is disabled, skipping client initialization");
            return null;
        }
        
        if (milvusClientFactory == null) {
            logger.warn("MilvusClientFactory not available, returning null client");
            return null;
        }
        
        try {
            logger.info("正在初始化 Milvus 客户端...");
            milvusClient = milvusClientFactory.createClient();
            logger.info("Milvus 客户端初始化完成");
            return milvusClient;
        } catch (Exception e) {
            logger.warn("Failed to initialize Milvus client: {}. Continuing without Milvus.", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (milvusClient != null) {
            logger.info("正在关闭 Milvus 客户端连接...");
            milvusClient.close();
            logger.info("Milvus 客户端连接已关闭");
        }
    }
}
