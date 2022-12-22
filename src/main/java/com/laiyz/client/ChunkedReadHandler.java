package com.laiyz.client;

import com.laiyz.client.base.ClientCache;
import com.laiyz.client.base.FileRspHelper;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * sslEnabled, not used zero-copy FileRegion mode,
 * ChunkedFile data will be handled by this handler
 *
 */
@Slf4j
public class ChunkedReadHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // incoming chunked file
        if (Config.sslEnabled() && StringUtils.isNotBlank(ClientCache.currRecvFileKey())) {
            // ----- chunked file data arrival
            log.info("currRecvFileKey: {}", ClientCache.currRecvFileKey());
            BFileMsg.BFileRsp rsp = ClientCache.getRspInfo(ClientCache.currRecvFileKey());
            if (rsp == null) {
                log.error("not found BFileRsp via currRecvFileKey: {}", ClientCache.currRecvFileKey());
                throw new RuntimeException("not found BFileRsp");
            }
            FileRspHelper.handleFileData(ctx, rsp, msg);
        } else { // not chunked file data, just forward msg next handler
//            ctx.fireChannelRead(msg);
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            ctx.fireChannelRead(Unpooled.wrappedBuffer(data));
            return;
        }
    }
}
