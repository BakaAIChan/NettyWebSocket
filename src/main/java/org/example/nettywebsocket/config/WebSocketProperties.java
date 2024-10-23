package org.example.nettywebsocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: mengyu
 * @Date: 2024/10/22
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chat.websocket")
public class WebSocketProperties {
    private Integer port; // 监听端口
    private String path; // 请求路径
    private Integer boss; // bossGroup线程数
    private Integer work; // workGroup线程数
}

