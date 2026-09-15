package com.explorewithnk.springapp.service;

import com.explorewithnk.springapp.model.TaskItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String KEY_PREFIX = "task:";

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheTask(TaskItem task) {
        if (task == null || task.getId() == null) {
            return;
        }
        try {
            String key = KEY_PREFIX + task.getId();
            redisTemplate.opsForValue().set(key, task, Duration.ofMinutes(10));
            log.info("Valkey: Cached task #{} with key '{}'", task.getId(), key);
        } catch (Exception e) {
            log.warn("Valkey: Failed to cache task #{}: {}", task.getId(), e.getMessage());
        }
    }

    public TaskItem getCachedTask(Long id) {
        try {
            String key = KEY_PREFIX + id;
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof TaskItem taskItem) {
                log.info("Valkey: Cache HIT for task #{}", id);
                return taskItem;
            }
        } catch (Exception e) {
            log.warn("Valkey: Failed to read cached task #{}: {}", id, e.getMessage());
        }
        return null;
    }

    public void evictTask(Long id) {
        try {
            String key = KEY_PREFIX + id;
            redisTemplate.delete(key);
            log.info("Valkey: Evicted cache for task #{}", id);
        } catch (Exception e) {
            log.warn("Valkey: Failed to evict cached task #{}: {}", id, e.getMessage());
        }
    }

    public boolean ping() {
        try {
            String pingResult = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pingResult);
        } catch (Exception e) {
            log.warn("Valkey: Ping check failed: {}", e.getMessage());
            return false;
        }
    }
}
