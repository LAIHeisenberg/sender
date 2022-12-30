package com.laiyz.client;

import com.laiyz.client.base.RspDispatcher;
import com.laiyz.comm.BFileCmd;
import com.laiyz.config.Config;
import com.laiyz.proto.SenderMsg;
import com.laiyz.util.BByteUtil;
import com.laiyz.util.BFileUtil;
import com.laiyz.util.ConstUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;


@Slf4j
public class FilePullClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * 服务器文件路径
     */
    private String serverFilePath;
    /**
     * 本地目标文件路径
     */
    private String distFilePath;


    public FilePullClientHandler(String serverFilePath, String distFilePath) {
        this.serverFilePath = serverFilePath;
        this.distFilePath = distFilePath;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        int len = msg.readableBytes();
        if (len < ConstUtil.sender_req_prefix_len){
            return;
        }
        msg.markReaderIndex();
        byte[] data = new byte[ConstUtil.sender_req_prefix_len];
        msg.readBytes(data);
        String prefix = BByteUtil.toStr(data);
        if (ConstUtil.sender_req_prefix.equals(prefix)) {
            int subLen = len - ConstUtil.sender_req_prefix_len;
            byte[] b = new byte[subLen];
            msg.readBytes(b);
            SenderMsg.Rsp rsp = SenderMsg.Rsp.parseFrom(b);
            switch (rsp.getCmd()){
                case BFileCmd.RSP_FILE_NOT_FIND:
                    System.out.println("远程文件不存在!");
                    break;
                case BFileCmd.RSP_COMPLETED:
                    System.out.println("\nall files received.");
                    break;
            }
        }else {
            msg.resetReaderIndex();
            RspDispatcher.dispatch(ctx, msg);
        }

        msg.release();
    }

}
