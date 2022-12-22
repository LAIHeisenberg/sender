package com.laiyz.util;

import com.alibaba.fastjson.JSON;
import com.laiyz.client.base.ClientCache;
import com.laiyz.comm.BFileCmd;
import com.laiyz.comm.BFileInfo;
import com.laiyz.proto.BFileMsg;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CtxUtil {

    public static BFileInfo reqNextFile(ChannelHandlerContext ctx) {
        BFileInfo nextFile = ClientCache.nextFile();
        log.info("nextFile: {}", nextFile);
        if (nextFile != null) {
            log.debug("after list files, req to download nextFile: {}", JSON.toJSONString(nextFile));
            // FileReq
            BFileMsg.BFileReq req = BFileUtil.buildReq(BFileCmd.REQ_FILE, nextFile.getFilepath());
            ctx.write(req);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
            return nextFile;
        }
        return null;
    }


}
