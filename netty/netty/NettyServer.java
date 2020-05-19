package org.shawn.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {
    public static void main(String[] args) throws Exception {
        /** 接收各种请求，死循环的线程池 */
        EventLoopGroup bosserGropup =null;
        /** 处理各种请求 */
        EventLoopGroup workerGropup =null;

        try {
            bosserGropup= new NioEventLoopGroup();
            workerGropup = new NioEventLoopGroup();
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bosserGropup,workerGropup).channel(NioServerSocketChannel.class).childHandler(new TestInitializer());
            ChannelFuture sync = serverBootstrap.bind(8899).sync();
            sync.channel().closeFuture().sync();
        } finally {
            bosserGropup.shutdownGracefully();
            workerGropup.shutdownGracefully();
        }
    }
}
