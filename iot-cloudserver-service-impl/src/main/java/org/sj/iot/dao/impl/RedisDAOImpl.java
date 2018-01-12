package org.sj.iot.dao.impl;

import org.sj.iot.dao.IRedisDAO;
import org.sj.iot.util.Constants;
import org.sj.iot.util.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-03
 */
@Repository
public class RedisDAOImpl implements IRedisDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDAOImpl.class);

    @Value("${lock.prefix}")
    private String lockPrefix;

    @Autowired
    private JedisPool pool;


    @Override
    public void open(Consumer<Jedis> process) {
        Objects.requireNonNull(process, "处理函数不能为空");
        try (Jedis jedis = pool.getResource()) {
            LOGGER.debug("打开一个Redis连接!");
            long start = System.nanoTime();
            process.accept(jedis); // 将连接交给处理函数
            LOGGER.debug("关闭一个Redis连接, 处理函数一共消耗: {} 毫秒!", Tools.diffNanoTime(start));
        } catch (Exception e) {
            LOGGER.error("redis连接出现异常: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String get(String key) {
        byte[] keyBS = key.getBytes(Constants.UTF8_CHARSET);
        String[] single = new String[1];
        this.open(jedis -> {
            byte[] valueBS = jedis.get(keyBS); // 获取
            if (valueBS != null && valueBS.length > 0) {
                single[0] = new String(valueBS);
            }
        });
        return single[0];
    }

    @Override
    public boolean set(String key, String value, int expire) {
        if (expire == 0) {
            return this.delete(key);
        }
        boolean[] flag = new boolean[1];
        byte[] keyBS = key.getBytes(Constants.UTF8_CHARSET);
        byte[] valueBS = value.getBytes(Constants.UTF8_CHARSET);
        this.open(jedis -> {
            String state = null;
            if (expire == -1) {
                state = jedis.set(keyBS, valueBS); // 永久存储
            } else {
                state = jedis.setex(keyBS, expire, valueBS); // 时效性
            }
            if ("OK".equals(state)) {
                flag[0] = true;
            }
        });
        return flag[0];
    }

    @Override
    public boolean exists(String key) {
        boolean[] flag = new boolean[1];
        byte[] keyBS = key.getBytes(Constants.UTF8_CHARSET);
        this.open(jedis -> {
            Boolean result = jedis.exists(keyBS);
            flag[0] = Boolean.TRUE.equals(result);
        });
        return flag[0];
    }

    @Override
    public boolean delete(String key) {
        byte[] keyBS = key.getBytes(Constants.UTF8_CHARSET);
        boolean[] flag = new boolean[1];
        this.open(jedis -> {
            Long result = jedis.del(keyBS);
            flag[0] = Long.valueOf(1).equals(result);
        });
        return flag[0];
    }

    @Override
    public void publish(String channel, String message) {
        open(jedis -> {
            jedis.publish(channel, message);
        });
    }

    @Override
    public long incrementAndGet(String key, Long max) {
        byte[] keyBS = key.getBytes(Constants.UTF8_CHARSET);
        long[] num = new long[1];
        this.open(jedis -> {
            Long incr = jedis.incr(keyBS);
            if (max != null && max.compareTo(incr) > 0) {
                jedis.set(keyBS, new byte[1]);
            }
            num[0] = incr;
        });
        return num[0];
    }

    @Override
    public boolean tryLock(String key) {
        return this.tryLock(key, this.timeout);
    }

    /**
     * 获取锁名
     */
    private String getLockKey(String key) {
        return String.format("LOCK:%s:%s", lockPrefix, key);
    }

    @Override
    public void unlock(String key) {
        String lockKey = getLockKey(key);
        boolean[] single = new boolean[1];
        try {
            this.open(jedis -> {
                String serverId = jedis.get(lockKey);
                if (serverId == null || !serverId.equals(Tools.getServiceIdBase64())) {
                    LOGGER.error("释放锁出现失败: 锁 {} 被释放或非当前持有!", key);
                    return;
                }
                Long result = jedis.del(lockKey);
                single[0] = Long.valueOf(1).equals(result);
            });
        } catch (Exception e) {
            LOGGER.error("释 {} 放锁出现异常: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean tryLock(String key, int timeout) {
        boolean[] single = new boolean[1];
        String lockKey = getLockKey(key);
        try {
            this.open(jedis -> {
                Long result = jedis.setnx(lockKey, Tools.getServiceIdBase64());
                if (Long.valueOf(0).equals(result)) {
                    // 设置失败, 已存在
                    return;
                }
                jedis.expire(lockKey, timeout < 0 ? this.timeout : timeout);
                single[0] = true;
            });
        } catch (Exception e) {
            LOGGER.error("尝试获取锁 {} 出现异常: {}", key, e.getMessage());
        }
        return single[0];
    }

    @Override
    public boolean taskAsLock(Runnable task) {
        return this.taskAsLock(null, task);
    }

    private static final Map<String, TimeUnit> cacheUnit = new ConcurrentHashMap<>();

    @Value("${lock.timeout}")
    private int timeout;

    private int getSeconds(String pattern) {
        TimeUnit unit = cacheUnit.get(pattern);
        if (unit == null) {
            TimeUnit milliseconds = TimeUnit.MILLISECONDS;
            long time = Tools.getDateTime(pattern);
            long diff = Tools.getCurrentTimeMillis() - time; // 取指定格式的时间与当前时间的差值
            if (milliseconds.toSeconds(diff) == 0) {
                // 锁定秒级
                unit = TimeUnit.SECONDS;
            } else if (milliseconds.toMinutes(diff) == 0) {
                // 锁定分钟级
                unit = TimeUnit.MINUTES;
            } else if (milliseconds.toHours(diff) == 0) {
                // 锁定小时级
                unit = TimeUnit.HOURS;
            } else if (milliseconds.toDays(diff) == 0) {
                // 锁定天级
                unit = TimeUnit.DAYS;
            }
            if (unit != null) {
                cacheUnit.put(pattern, unit);
            }
        }
        if (unit != null) {
            return (int) unit.toSeconds(1);
        }
        return timeout;
    }

    @Override
    public boolean taskAsLock(String pattern, Runnable task) {
        boolean holdLock = false; // 持有锁标识
        if (task == null) {
            LOGGER.error("未找到需要被执行的任务!");
            return holdLock;
        }
        int seconds = timeout;
        if (pattern != null) {
            seconds = getSeconds(pattern);
        }
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        String className = null;
        String methodName = null;
        for (int i = 2; i < stackTraces.length; i++) {
            // 跳过getStackTrace()、taskAsLock()方法的元素
            StackTraceElement stackTrace = stackTraces[i];
            className = stackTrace.getClassName();
            if (this.getClass().getName().equals(stackTrace.getClassName())) {
                // 当前对象其他方法调用的, 寻找下一个
                continue;
            }
            methodName = stackTrace.getMethodName();
            break;
        }
        String key = String.format("%s/%s", className, methodName);
        try {
            if (holdLock = this.tryLock(key, seconds)) {
                // 获取锁,执行任务
                LOGGER.debug("争抢到资源锁任务开始执行任务: {}.{}", className, methodName);
                long start = System.nanoTime();
                task.run();
                LOGGER.debug("任务执行完成 耗时 {} 毫秒!", Tools.diffNanoTime(start));
            } else {
                LOGGER.debug("未找到争抢到资源锁任务不会被执行: {}.{}", className, methodName);
            }
        } catch (Exception e) {
            LOGGER.error("执行任务 {}.{} 出错: {}", className, methodName, e.getMessage());
            throw e;
        } finally {
            /*if (holdLock) {
                // 释放锁
                LOGGER.debug("释放资源锁: {}.{}", className, methodName);
                this.unlock(key);
            }*/
        }
        return holdLock;
    }
}
