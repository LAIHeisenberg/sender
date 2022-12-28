/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.laiyz.server;

import com.laiyz.client.ChunkedReadHandler;
import com.laiyz.client.base.ClientCmdRegister;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
import com.laiyz.proto.SenderMsg;
import com.laiyz.server.base.CmdRegister;
import com.laiyz.util.ConstUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Server that accept the path of a file an echo back its content.
 */
@Slf4j
public final class FileServer {
//    static final boolean SSL = Config.sslEnabled();
//    static final int PORT = Config.port();

    public static void startup() {
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
//            CmdRegister.init();
            ClientCmdRegister.init();
            // Configure SSL.
            final SslContext sslCtx;
            if (Config.sslEnabled()) {
                File certChainFile = Config.serverSSLCertChainFile();
                File keyFile = Config.serverSSLKeyFile();
                File rootFile = Config.serverSSLRootFile();
                sslCtx = SslContextBuilder.forServer(certChainFile, keyFile).trustManager(rootFile).clientAuth(ClientAuth.REQUIRE).build();
            } else {
                sslCtx = null;
            }


            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }

                            // --- inbound
                            // if os not support zero-copy/sslEnabled, used this, must be the first inbound handler
//                            p.addLast(new ChunkedReadHandler());

                            // inbound frameLen = chunkSize[default: 8192] + BFileRsp header)
//                            p.addLast(new DelimiterBasedFrameDecoder(10240, delimiter));

                            p.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Unpooled.copiedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8))));

                            p.addLast(new FileServerHandler());
                            p.addLast(new FileServerHandler2());
                        }
                    });

            // Start the server.
            ChannelFuture f = b.bind(Config.port()).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("server startup error.", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
