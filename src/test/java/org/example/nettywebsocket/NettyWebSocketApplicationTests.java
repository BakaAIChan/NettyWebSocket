package org.example.nettywebsocket;

import jakarta.annotation.Resource;
import org.example.nettywebsocket.config.WebSocketProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NettyWebSocketApplicationTests {

    @Resource
    private WebSocketProperties webSocketProperties;


    @Test
    void contextLoads() {
        System.out.println(webSocketProperties.getBoss());
    }

}
