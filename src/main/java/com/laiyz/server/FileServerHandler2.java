package com.laiyz.server;

import com.laiyz.client.base.RspDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileServerHandler2 extends SimpleChannelInboundHandler<ByteBuf> {

    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        RspDispatcher.dispatch(ctx, msg);
    }

}
