package com.example.chichi.config;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


public class CustomTestRedisContainer {
    public static final GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:7.2"))
            .withExposedPorts(6379);
    public static final RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

    public static void setup() {
        redisContainer.start();
        String redisHost = redisContainer.getHost();
        Integer redisPort = redisContainer.getFirstMappedPort();

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        LettuceConnectionFactory lettuce = new LettuceConnectionFactory(redisConfig);
        lettuce.afterPropertiesSet();

        redisTemplate.setConnectionFactory(lettuce);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();
    }
}
