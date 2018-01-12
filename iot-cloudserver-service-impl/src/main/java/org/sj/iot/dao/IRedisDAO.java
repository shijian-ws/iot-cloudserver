package org.sj.iot.dao;

import redis.clients.jedis.Jedis;

import java.util.function.Consumer;

/**
 * Redis数据访问接口
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
public interface IRedisDAO {
    /**
     * 打开一个连接处理业务并自动释放
     */
    void open(Consumer<Jedis> process);

    /**
     * 获取key对应的value值
     */
    String get(String key);

    /**
     * 将键与值存储进redis数据库
     *
     * @param key
     * @param value
     * @param expire 超时时间, -1:永不超时, 0:立即超时，立即删除, 单位: 秒
     * @return
     */
    boolean set(String key, String value, int expire);

    /**
     * 检查一个key是否存在
     */
    boolean exists(String key);

    /**
     * 移除key对应的值
     */
    boolean delete(String key);

    /**
     * 自增一个Key的值并获取, 如果存在最大值则在达到最大值时设置为0
     */
    long incrementAndGet(String key, Long max);

    /**
     * 向指定频道推送一个消息
     */
    void publish(String channel, String message);

    /**
     * 获取锁, 如果不自动释放将在5分钟后失效
     */
    boolean tryLock(String key);

    /**
     * 释放锁
     */
    void unlock(String key);

    /**
     * 获取锁, 并指定锁超时时间, 单位: 秒
     */
    boolean tryLock(String key, int timeout);

    /**
     * 集群环境下争抢资源锁执行一次同步阻塞任务
     *
     * @param task 任务
     * @return 任务是否被执行
     */
    boolean taskAsLock(Runnable task);

    /**
     * 集群环境下争抢资源锁并锁定指定时间区域, 在指定时间区域内只有一个程序能获取资源锁
     *
     * @param pattern 日期时间格式 yyyy-MM-dd HH:锁定1小时, yyyy-MM-dd HH:mm 锁定1分钟
     * @param task    任务
     * @return 任务是否被执行
     */
    boolean taskAsLock(String pattern, Runnable task);
}
