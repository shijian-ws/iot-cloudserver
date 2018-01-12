package org.sj.iot.service;

import org.sj.iot.exception.DataAccessException;
import org.sj.iot.model.Cmd;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TcpServer业务操作接口
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
public interface ITcpServerService {
    /**
     * 在线网关设备
     */
    List<String> listGatewayOnline();

    /**
     * 添加一个异步响应单播命令
     */
    void addUnicastCmd(Cmd cmd, Consumer<Cmd> callback);

    /**
     * 添加一个异步响应广播命令
     */
    void addBroadcastCmd(Cmd cmd, Consumer<Cmd> callback);

    /**
     * 添加一个同步响应单播命令
     *
     * @param cmd
     * @param timeout 超时时间，单位:秒
     */
    default void addSyncUnicastCmd(Cmd cmd, int timeout) {
        try {
            CountDownLatch count = new CountDownLatch(1); // 倒数器
            addUnicastCmd(cmd, result -> {
                cmd.setDataBody(result.getDataBody()); // 替换响应的数据体对象
                cmd.setSendTime(result.getSendTime());
                cmd.setAckTime(result.getAckTime());
                cmd.setAcks(result.getAcks());
                count.countDown(); // 倒数器值-1
            });
            count.await(timeout, TimeUnit.SECONDS); // 阻塞等待异步响应
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("同步等待命令响应超时: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                // 操作超时
                throw new DataAccessException(e, -1);
            }
        }
    }
}
