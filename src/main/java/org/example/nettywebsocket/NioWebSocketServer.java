package org.example.nettywebsocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.example.nettywebsocket.config.WebSocketProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * WebSocket服务
 * 实现 InitializingBean ： 确保在Spring容器初始化时启动WebSocket服务器
 * 实现 DisposableBean ： 确保在Spring关闭时关闭服务器
 */
@Slf4j
@Component
public class NioWebSocketServer implements InitializingBean, DisposableBean {

    @Autowired
    private WebSocketProperties webSocketProperties;
    @Autowired
    private NioWebSocketChannelInitializer webSocketChannelInitializer;

    /**
     * bossGroup 和 workGroup 都说用来处理事件的线程组。
     * bossGroup用于接收 客户端的连接请求。
     * workGroup用于处理 已接受的连接，执行I/O操作，如读取、写入数据等。
     * 通常bossGroup配置较少的线程，因为只需要处理连接请求。
     * 而workGroup可能需要更多的线程，因为它负责处理实际的网络I/O操作。
     */
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;

    // 服务器绑定操作的异步结果
    private ChannelFuture channelFuture;

    /**
     * Spring容器初始化时执行
     * 开启Netty服务
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            log.info("Netty Server info : boss:{} work:{} port:{} path:{}", webSocketProperties.getBoss(), webSocketProperties.getWork(), webSocketProperties.getPort(), webSocketProperties.getPath());
            bossGroup = new NioEventLoopGroup(webSocketProperties.getBoss());
            workGroup = new NioEventLoopGroup(webSocketProperties.getWork());

            // 初始化Netty服务器配置
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 具体配置
            serverBootstrap
                    // 服务器配置，这里设置最多阻塞排队1024个请求
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // 设置bossGroup和workGroup
                    .group(bossGroup, workGroup)
                    // 指定通道类型，这里NioServerSocketChannel是基于Java NIO的通道类型
                    // 用于服务器端的TCP连接，支持异步IO，适合处理大量并发连接
                    .channel(NioServerSocketChannel.class)
                    // 地址，指定端口
                    .localAddress(webSocketProperties.getPort())
                    // 配置子管道处理器
                    // 其中webSocketChannelInitializer负责初始化新连接的管道
                    .childHandler(webSocketChannelInitializer);

            // 启动Netty服务，并获取异步回执
            channelFuture = serverBootstrap.bind().sync();
        } finally {
            // 处理返回结果
            if (channelFuture != null && channelFuture.isSuccess()) {
                // 成功
                log.warn("Netty server startup on port: {} (websocket) with context path '{}'", webSocketProperties.getPort(), webSocketProperties.getPath());
            } else {
                // 失败
                log.error("Netty server startup failed.");
                if (bossGroup != null)
                    bossGroup.shutdownGracefully().sync();
                if (workGroup != null)
                    workGroup.shutdownGracefully().sync();
            }
        }
    }

    /**
     * Spring容器关闭时执行
     * 关闭Netty服务
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        log.info("Shutting down Netty server...");
        if (bossGroup != null)
            bossGroup.shutdownGracefully().sync();
        if (workGroup != null)
            workGroup.shutdownGracefully().sync();
        if (channelFuture != null)
            channelFuture.channel().closeFuture().syncUninterruptibly();
        log.info("Netty server shutdown.");
    }
}
