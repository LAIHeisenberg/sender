package com.laiyz.client;

import com.laiyz.client.base.ClientCmdRegister;
import com.laiyz.config.Config;
import com.laiyz.util.ConstUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class FilePullClient {

    public void startup(String serverFilePath, String distFilePath) {

        EventLoopGroup group = new NioEventLoopGroup();
        ClientCmdRegister.init();
        try {
            Bootstrap b = new Bootstrap();
                b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Unpooled.copiedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8))));

                        p.addLast(new FilePullClientHandler(serverFilePath, distFilePath));
                    }
                });
            // Start the client.
            ChannelFuture f = b.connect(Config.host(), Config.port()).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("startup client error.", e);
        }
        finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }

    }

}
