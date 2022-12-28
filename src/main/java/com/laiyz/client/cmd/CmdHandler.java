package com.laiyz.client.cmd;

import com.laiyz.proto.SenderMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CmdHandler {

    public abstract void handle(ChannelHandlerContext ctx, SenderMsg.Rsp rsp, ByteBuf msg);

    public void handleNext(ChannelHandlerContext ctx, SenderMsg.Rsp rsp, ByteBuf msg) {
        handle(ctx, rsp, msg);
    }


}
