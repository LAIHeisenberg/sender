package com.laiyz.client.cmd;

import com.laiyz.client.base.ClientCache;
import com.laiyz.client.base.FileRspHelper;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileRspHandler extends CmdHandler {

    public void handle(ChannelHandlerContext ctx, BFileMsg.BFileRsp rsp, ByteBuf msg) {
        log.info("transferring file({})...", rsp.getFilepath());

        // ssl enabled, file transferred in ChunkedFile mode,
        // keep first chunkedFile msg(BFileRsp) in client cache,
        // the rest chunkedFile msg(file data) will be handle by ChunkedReadHandler, not this FileRspHandler
        if (Config.sslEnabled()) {
            String recvFileKey = BFileUtil.getReqId(rsp.getFilepath());
            ClientCache.addRspInfo(recvFileKey, rsp);
            // subsequent chunked file data will be handle by ChunkedFileDataHandler before this
            return;
        }

        // zero-copy(FileRegion) mode, file data handling here
        FileRspHelper.handleFileData(ctx, rsp, msg);
    }



}
