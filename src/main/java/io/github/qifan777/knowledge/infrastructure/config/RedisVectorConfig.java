package io.github.qifan777.knowledge.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

// OER — 网上报销：Redis Stack 向量数据库配置
// 使用 Spring AI 2.x Builder API 手动创建 RedisVectorStore，
// 不依赖自动配置以避免与业务 Redis 连接冲突
// Jedis 7.x 用 RedisClient 替代了 JedisPooled（两者同层级，均继承 UnifiedJedis）
@Configuration
public class RedisVectorConfig {

    /**
     * 创建 RedisStack 向量数据库
     *
     * @param embeddingModel          嵌入模型（通过 OpenAI 兼容 API 调用 DashScope text-embedding-v2）
     * @param dataRedisConnectionDetails Spring Boot 4.x 统一 Redis 连接信息（不含业务 Redis）
     * @return vectorStore 向量数据库
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   DataRedisConnectionDetails dataRedisConnectionDetails) {
        RedisClient redisClient = RedisClient.create(
                dataRedisConnectionDetails.getStandalone().getHost(),
                dataRedisConnectionDetails.getStandalone().getPort(),
                dataRedisConnectionDetails.getUsername(),
                dataRedisConnectionDetails.getPassword());
        return RedisVectorStore.builder(redisClient, embeddingModel)
                .indexName("spring-ai-document-index")
                .prefix("spring-ai-document-")
                .initializeSchema(true)
                .build();
    }
}
