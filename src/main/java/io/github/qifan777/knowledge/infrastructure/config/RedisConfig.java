package io.github.qifan777.knowledge.infrastructure.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

// PUB — 公共配置：Redis 模板配置
// 为业务 Redis 操作提供 String/Object 通用序列化模板
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
@Configuration
public class RedisConfig {

  /**
   * 创建通用 RedisTemplate，用于业务缓存
   *
   * @param redisConnectionFactory Spring Boot 自动注入的 LettuceConnectionFactory
   * @return string-object 序列化的 RedisTemplate
   */
  @Bean
  public RedisTemplate<String, Object> stringObjectRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> stringObjectRedisTemplate = new RedisTemplate<>();
    stringObjectRedisTemplate.setConnectionFactory(redisConnectionFactory);
    // 使用FastJson序列化object
    stringObjectRedisTemplate.setDefaultSerializer(GenericJacksonJsonRedisSerializer.builder().build());
    return stringObjectRedisTemplate;
  }
}