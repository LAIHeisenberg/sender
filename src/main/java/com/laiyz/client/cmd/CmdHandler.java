package com.laiyz.client.cmd;

import com.laiyz.proto.BFileMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CmdHandler {

    public abstract void handle(ChannelHandlerContext ctx, BFileMsg.BFileRsp rsp, ByteBuf msg);

    public void handleNext(ChannelHandlerContext ctx, BFileMsg.BFileRsp rsp, ByteBuf msg) {
        handle(ctx, rsp, msg);

        // TODO add last file complete check and start next here, following code cannot be used
//        BFileInfo nextFile = CtxUtil.reqNextFile(ctx);
//        log.info("@@@@@@@@@@@@@@@@ request next file : {} @@@@@@@@@@", JSON.toJSONString(nextFile));
//        if (nextFile == null) {
//            log.info("all files received, can stop client now(ChunkedWriteHandler need to wait).");
//            ClientCache.cleanAll();
//            System.exit(0);
//        }
    }


}
