package com.laiyz.client;

import com.laiyz.client.base.ClientCmdRegister;
import com.laiyz.comm.BFileCmd;
import com.laiyz.config.Config;
import com.laiyz.proto.SenderMsg;
import com.laiyz.util.BFileUtil;
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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

@Slf4j
public class FilePullClient {

    public void startup(String serverFilePath, String destFilePath) {

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
                        p.addLast(new FilePullClientHandler(serverFilePath, destFilePath));
                    }
                });
            // Start the client.
//            ChannelFuture f = b.connect(Config.host(), Config.port()).sync();
            ChannelFuture f = b.connect(Config.host(), Config.port());

            f.addListener((ChannelFutureListener) cfl -> {

                if (StringUtils.isBlank(serverFilePath)){
                    throw new IllegalArgumentException("服务器文件路径不能为空");
                }
                if (StringUtils.isBlank(destFilePath)){
                    throw new IllegalArgumentException("目标文件路径不能为空");
                }

                try {
                    long fileLength = BFileUtil.getFileLength(destFilePath + Config.tempFilePostfix());
                    SenderMsg.Req req = BFileUtil.buildSenderReq(BFileCmd.REQ_PULL, serverFilePath, destFilePath, fileLength);
                    cfl.channel().write(Unpooled.wrappedBuffer(ConstUtil.sender_req_prefix.getBytes(CharsetUtil.UTF_8)));
                    cfl.channel().write(Unpooled.wrappedBuffer(req.toByteArray()));
                    cfl.channel().writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
                }catch (Exception e){}

            });
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
