package org.example.nettywebsocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.example.nettywebsocket.config.WebSocketProperties;
import org.example.nettywebsocket.util.RequestUriUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自定义Netty ChannelHandler
 * 用于处理WebSocket相关网络事件和数据帧
 * 继承SimpleChannelInboundHandler<WebSocketFrame> -> 能够处理入站的WebSocketFrame对象
 */
@Slf4j
@ChannelHandler.Sharable
@Component
public class NioWebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Autowired
    // ChannelPool，用于记录已连接的Channel
    private NioWebSocketChannelPool webSocketChannelPool;
    @Autowired
    private WebSocketProperties webSocketProperties;

    /**
     * Channel激活时（客户端建立连接时）执行
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端连接：{}", ctx.channel().id());
        // 将建立连接的Channel放到ChannelPool中
        webSocketChannelPool.addChannel(ctx.channel());
        super.channelActive(ctx);
    }

    /**
     * Channel非激活时（客户端断开连接时）执行
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端断开连接：{}", ctx.channel().id());
        // 将断开连接的Channel从ChannelPool中删除
        webSocketChannelPool.removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }

    /**
     * 读取数据完成后调用
     *
     * @param ctx
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        log.info("读取数据完成:{}", ctx.channel().id());
        ctx.channel().flush();
    }

    /**
     * 处理WebSocketFrame类型的入站消息
     *
     * @param ctx
     * @param frame
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 根据请求数据类型进行分发处理
        if (frame instanceof PingWebSocketFrame) {
            pingWebSocketFrameHandler(ctx, (PingWebSocketFrame) frame);
        } else if (frame instanceof TextWebSocketFrame) {
            textWebSocketFrameHandler(ctx, (TextWebSocketFrame) frame);
        } else if (frame instanceof CloseWebSocketFrame) {
            closeWebSocketFrameHandler(ctx, (CloseWebSocketFrame) frame);
        }
    }

    /**
     * 处理所有入站消息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("客户端请求数据类型：{}", msg.getClass());
        // 如果是FullHttpRequest，代表是握手请求，直接转发至对应的Handler
        if (msg instanceof FullHttpRequest) {
            fullHttpRequestHandler(ctx, (FullHttpRequest) msg);
        }
        // 如果是其他的，需要去super中进一步判断
        super.channelRead(ctx, msg);
    }

    /**
     * 处理连接请求，客户端WebSocket发送握手包时会执行这一次请求
     *
     * @param ctx
     * @param request
     */
    private void fullHttpRequestHandler(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        log.info("握手请求:{}", uri);
        Map<String, String> params = RequestUriUtils.getParams(uri);
        log.debug("客户端请求参数：{}", params);
        // 判断请求路径是否跟配置中的一致
        if (webSocketProperties.getPath().equals(RequestUriUtils.getBasePath(uri)))
            // 因为有可能携带了参数，导致客户端一直无法返回握手包，因此在校验通过后，重置请求路径
            request.setUri(webSocketProperties.getPath());
        else
            ctx.close();
    }

    /**
     * 客户端发送断开请求处理
     *
     * @param ctx
     * @param frame
     */
    private void closeWebSocketFrameHandler(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
        ctx.close();
    }

    /**
     * 创建连接之后，客户端发送的消息都会在这里处理
     *
     * @param ctx
     * @param frame
     */
    private void textWebSocketFrameHandler(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        // 客户端发送过来的内容不进行业务处理，原样返回
        String text = frame.retain().text();
        log.info("消息:{}", text);
        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(text + text);
        ctx.channel().writeAndFlush(textWebSocketFrame.retain());
    }

    /**
     * 处理客户端心跳包
     *
     * @param ctx
     * @param frame
     */
    private void pingWebSocketFrameHandler(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
    }
}
