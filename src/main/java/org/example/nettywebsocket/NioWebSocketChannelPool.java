package org.example.nettywebsocket;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Channel池，用于存储登录已连接的管道
 * @Author: mengyu
 * @Date: 2024/10/22
 */
@Slf4j
@Component
public class NioWebSocketChannelPool {

    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 新增一个客户端通道
     *
     * @param channel
     */
    public void addChannel(Channel channel) {
        channels.add(channel);
    }

    /**
     * 移除一个客户端连接通道
     *
     * @param channel
     */
    public void removeChannel(Channel channel) {
        channels.remove(channel);
    }
}

