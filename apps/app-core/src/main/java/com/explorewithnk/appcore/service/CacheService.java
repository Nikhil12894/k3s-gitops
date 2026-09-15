package com.explorewithnk.appcore.service;

import com.explorewithnk.appcore.model.TaskItem;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CacheService {

    private static final Logger LOG = Logger.getLogger(CacheService.class);
    private static final String KEY_PREFIX = "task:";

    @Inject
    Instance<RedisDataSource> redisDataSourceInstance;

    @Inject
    MetricsService metricsService;

    private RedisDataSource redisDataSource;
    private ValueCommands<String, TaskItem> valueCommands;

    @PostConstruct
    void init() {
        try {
            if (redisDataSourceInstance.isResolvable()) {
                this.redisDataSource = redisDataSourceInstance.get();
                this.valueCommands = redisDataSource.value(TaskItem.class);
            }
        } catch (Exception e) {
            LOG.warn("Failed to initialize Valkey/Redis value commands: " + e.getMessage());
        }
    }

    public TaskItem getTask(Long id) {
        if (valueCommands == null) {
            metricsService.incrementValkeyMiss();
            return null;
        }
        try {
            TaskItem item = valueCommands.get(KEY_PREFIX + id);
            if (item != null) {
                metricsService.incrementValkeyHit();
                LOG.debugf("Valkey cache hit for task ID %d", id);
                return item;
            } else {
                metricsService.incrementValkeyMiss();
                LOG.debugf("Valkey cache miss for task ID %d", id);
                return null;
            }
        } catch (Exception e) {
            LOG.warnf("Valkey get failed for task ID %d: %s", id, e.getMessage());
            metricsService.incrementValkeyMiss();
            return null;
        }
    }

    public void putTask(TaskItem task) {
        if (valueCommands == null || task == null || task.id == null) {
            return;
        }
        try {
            valueCommands.set(KEY_PREFIX + task.id, task);
            LOG.debugf("Valkey cached task ID %d", task.id);
        } catch (Exception e) {
            LOG.warnf("Valkey put failed for task ID %d: %s", task.id, e.getMessage());
        }
    }

    public void evictTask(Long id) {
        if (redisDataSource == null || id == null) {
            return;
        }
        try {
            redisDataSource.key().del(KEY_PREFIX + id);
            LOG.debugf("Valkey evicted task ID %d", id);
        } catch (Exception e) {
            LOG.warnf("Valkey evict failed for task ID %d: %s", id, e.getMessage());
        }
    }

    public boolean ping() {
        if (redisDataSource == null) {
            return false;
        }
        try {
            String pong = redisDataSource.execute("PING").toString();
            return pong != null && pong.contains("PONG");
        } catch (Exception e) {
            LOG.debugf("Valkey ping check failed: %s", e.getMessage());
            return false;
        }
    }
}
