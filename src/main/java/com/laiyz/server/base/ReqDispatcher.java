package com.laiyz.server.base;

import com.laiyz.proto.BFileMsg;
import com.laiyz.server.cmd.CmdHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * client request will dispatch to different server cmdHandler according cmd
 *
 */
public class ReqDispatcher {

    public static void dispatch(ChannelHandlerContext ctx, BFileMsg.BFileReq msg) {
        CmdHandler cmdHandler = CmdRegister.getHandler(msg.getCmd());
        cmdHandler.handle(ctx, msg);
    }

}
