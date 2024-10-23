package org.example.nettywebsocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.example.nettywebsocket.config.WebSocketProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Channel初始化
 */
@Component
public class NioWebSocketChannelInitializer extends ChannelInitializer<SocketChannel>{

    @Autowired
    private WebSocketProperties webSocketProperties;
    @Autowired
    private NioWebSocketHandler nioWebSocketHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        // 获取管道，并添加处理器到管道中
        socketChannel.pipeline()
                // Netty编解码器，将HTTP请求和响应编码和解码成帧
                .addLast(new HttpServerCodec())
                // 用于支持HTTP分块传输
                .addLast(new ChunkedWriteHandler())
                // 聚合HTTP请求和响应信息，将多个信息片段聚合成一个完整的消息
                .addLast(new HttpObjectAggregator(8192))
                // 自定义WebSocket处理器
                .addLast(nioWebSocketHandler)
                // Netty的WebSocket处理器，负责处理WebSocket的协议升级（HTTP -> WS）、消息处理和关闭处理。
                // 同时指定路径、子协议以及最大消息大小等等
                .addLast(new WebSocketServerProtocolHandler(webSocketProperties.getPath(), null, true, 65536));
    }
}
