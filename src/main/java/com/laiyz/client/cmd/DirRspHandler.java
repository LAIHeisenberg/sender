package com.laiyz.client.cmd;

import com.laiyz.client.base.FileRspHelper;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirRspHandler extends CmdHandler {

    @Override
    public void handle(ChannelHandlerContext ctx, BFileMsg.BFileRsp rsp, ByteBuf msg) {
        // server filepath
        String serverdir = rsp.getFilepath();
        String clientdir = BFileUtil.getClientFullPathWithCheck(serverdir, Boolean.FALSE);
        log.debug("recv server dir: {}, client dir: {}", serverdir, clientdir);
        BFileUtil.mkdir(clientdir);
        log.debug("client create dir: {}", clientdir);

        FileRspHelper.handleNext(ctx);
    }
}
