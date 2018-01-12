package org.sj.iot;

import org.sj.iot.configuration.RedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 云端服务启动
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@SpringBootApplication
@EnableConfigurationProperties({RedisProperties.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
