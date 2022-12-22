package com.laiyz.server.cmd;

import com.laiyz.proto.BFileMsg;
import io.netty.channel.ChannelHandlerContext;

public interface CmdHandler {

    public void handle(ChannelHandlerContext ctx, BFileMsg.BFileReq msg);

}
