package org.sj.iot.service.impl;

import org.sj.iot.dao.IRedisDAO;
import org.sj.iot.exception.DataAccessException;
import org.sj.iot.model.Cmd;
import org.sj.iot.model.Event;
import org.sj.iot.model.Event.Type;
import org.sj.iot.model.EventListener;
import org.sj.iot.service.ITcpServerService;
import org.sj.iot.util.Constants;
import org.sj.iot.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPubSub;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * TcpServer业务操作接口实现类
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@Service
public class RedisTcpServerServiceImpl implements ITcpServerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTcpServerServiceImpl.class);

    private static final EventListener listener = new EventListener(); // 监听器

    @Autowired
    private IRedisDAO redisDAO;
    @Autowired
    private ThreadPoolExecutor executor;

    @PostConstruct
    public void init() {
        // 注册监听器
        executor.execute(() -> {
            redisDAO.open(jedis -> {
                // 监听通道, 同步阻塞
                jedis.psubscribe(new JedisPubSub() {
                    @Override
                    public void onPMessage(String pattern, String channel, String message) {
                        listener.onData(channel, message);
                    }
                }, "*");
            });
        });
        // 添加消息监听器
        addMQListener();
    }

    /**
     * 缓存消息处理器，{channel: callback}
     */
    private static final Map<String, Consumer<byte[]>> process = new ConcurrentHashMap<>();

    /**
     * 消息回调函数
     */
    private static final Consumer<Event> callback = event -> {
        String channel = event.getPath();
        Consumer<byte[]> func = process.remove(channel);
        if (func == null) {
            LOGGER.error("消息 {} 未找到对应回调函数!", channel);
            return;
        }
        func.accept(event.getData());
    };

    /**
     * 添加广播, 单播监听器
     */
    private static void addMQListener() {
        addListener(callback, String.format("%s/*", Constants.TCP_SERVER_UNICAST_MQ_PATH), Type.UPDATED); // 添加单播监听器
        addListener(callback, String.format("%s/*", Constants.TCP_SERVER_BROADCAST_MQ_PATH), Type.UPDATED); // 添加广播监听器
    }

    /**
     * 添加监听
     *
     * @param callback 回调函数
     * @param nodePath 监听路径
     * @param types    监听事件
     */
    private static void addListener(Consumer<Event> callback, String nodePath, Type... types) {
        Objects.requireNonNull(callback, "监听回调函数不能为空!");
        listener.addListener(callback, nodePath, types);
    }

    @Override
    public List<String> listGatewayOnline() {
        List<String> macs = new ArrayList<>();
        redisDAO.open(jedis -> {
            Map<String, String> map = jedis.hgetAll(Constants.DEVICE_GATEWAY_PATH);
            if (!map.isEmpty()) {
                macs.addAll(map.keySet());
            }
        });
        return macs;
    }

    private void addMessageQueue(Event event, Consumer<byte[]> callback) throws Exception {
        String channel = event.getPath();
        String data = JsonUtil.toJsonString(event);
        if (callback != null) {
            process.put(channel, callback);
        }
        redisDAO.publish(channel, data);
    }

    /**
     * 添加消息到消息对象
     */
    private void addMessage(String mqPath, Cmd cmd, Consumer<Cmd> callback) {
        String uuid = cmd.getUuid();
        String channel = String.format("%s/%s", mqPath, uuid);
        try {
            byte[] data = JsonUtil.toJsonByte(cmd);
            addMessageQueue(new Event(Type.ADDED, channel, data), (byte[] resultData) -> {
                Cmd result = JsonUtil.toObject(resultData, Cmd.class);
                if (callback != null) {
                    callback.accept(result);
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            if (e instanceof DataAccessException) {
                throw (DataAccessException) e;
            }
            throw new DataAccessException(e, 1, "远程控制失败!");
        }
    }

    @Override
    public void addUnicastCmd(Cmd cmd, Consumer<Cmd> callback) {
        addMessage(Constants.TCP_SERVER_UNICAST_MQ_PATH, cmd, callback);
    }

    @Override
    public void addBroadcastCmd(Cmd cmd, Consumer<Cmd> callback) {
        addMessage(Constants.TCP_SERVER_BROADCAST_MQ_PATH, cmd, callback);
    }
}
