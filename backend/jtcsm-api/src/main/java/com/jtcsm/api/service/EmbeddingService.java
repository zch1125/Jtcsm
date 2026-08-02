package com.jtcsm.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Embedding 向量服务
 * <p>通过 Spring AI EmbeddingModel 调用 text-embedding-v3 生成语义向量，
 * 并提供 Redis 缓存层避免重复调用。</p>
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    /** 向量维度缓存前缀 */
    private static final String CACHE_PREFIX = "jtcsm:embedding:";

    /** 缓存有效期，高频查询缓存 24 小时 */
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 为单个文本生成向量
     * @param text 输入文本
     * @return 浮点向量数组
     */
    public float[] embed(String text) {
        if (!StringUtils.hasText(text)) {
            return new float[0];
        }

        // 尝试从缓存读取
        String cacheKey = CACHE_PREFIX + md5(text);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseVector(cached);
        }

        // 调用 EmbeddingModel
        if (embeddingModel == null) {
            log.warn("EmbeddingModel 未注入，返回零向量");
            return new float[1024];
        }

        try {
            float[] result = embeddingModel.embed(text);

            // 写入缓存
            redis.opsForValue().set(cacheKey, vectorToString(result), CACHE_TTL);

            return result;
        } catch (Exception e) {
            log.error("Embedding 调用失败: {}", e.getMessage());
            return new float[1024];
        }
    }

    /**
     * 为查询文本生成检索向量（针对搜索场景轻量化裁剪）
     */
    public float[] embedForQuery(String query) {
        // 裁剪过长输入，text-embedding-v3 最大 8192 token，中文约 3000 字内安全
        String safeText = query.length() > 2000 ? query.substring(0, 2000) : query;
        return embed(safeText);
    }

    /**
     * 解析缓存中的向量字符串
     */
    private float[] parseVector(String str) {
        String[] parts = str.split(",");
        float[] vec = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Float.parseFloat(parts[i]);
        }
        return vec;
    }

    /**
     * 将向量数组转为缓存字符串
     */
    private String vectorToString(float[] vec) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        return sb.toString();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
