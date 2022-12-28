package com.laiyz.client.base;

import com.alibaba.fastjson.JSON;
import com.laiyz.client.task.impl.FileTask;
import com.laiyz.client.task.impl.FileTaskListener;
import com.laiyz.comm.BFileCmd;
import com.laiyz.comm.BFileInfo;
import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.SenderMsg;
import com.laiyz.util.ConstUtil;
import com.laiyz.util.CtxUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * used by FileRspHandler & ChunkedReadHandler
 */
@Slf4j
public class FileRspHelper {

    public static void handleFileData(ChannelHandlerContext ctx, SenderMsg.Rsp rsp, ByteBuf msg, boolean writeProgress ) {
        byte[] fileData = parseFileData(msg);
        FileTask fileTask = null;
        if ((fileTask = ClientCache.getTask(rsp.getId())) == null) {
            fileTask = new FileTask(rsp);
            ClientCache.addTask(rsp.getId(), fileTask);
            fileTask.addListner(new FileTaskListener());
        }

        StatusEnum status = fileTask.appendFileData(ctx,fileData, rsp,  writeProgress);

        if (status == StatusEnum.COMPLETED) {
            log.info("file({}) transfer complete.", rsp.getFilepath());
            handleNext(ctx);
        }
    }

    private static byte[] parseFileData(ByteBuf msg) {
        byte[] fileData = null;
        // decode chunk data and set to rsp
        int chunkSize = msg.readableBytes();
        if (chunkSize == 0) {
            log.error("chunk data is 0.");
            return new byte[0];
        }
        fileData = new byte[chunkSize];
        msg.readBytes(fileData);
        return fileData;
    }

    public static void handleNext(ChannelHandlerContext ctx) {
        BFileInfo nextFile = CtxUtil.reqNextFile(ctx);
        log.info("@@@@@@@@@@@@@@@@ request next file : {} @@@@@@@@@@", JSON.toJSONString(nextFile));
        // all files downloaded
        if (nextFile == null) {
            ClientCache.cleanAll();
            log.info("all files received.");

            ctx.write(Unpooled.wrappedBuffer(ConstUtil.sender_req_prefix.getBytes(CharsetUtil.UTF_8)));
            SenderMsg.Rsp senderMsgRsp = SenderMsg.Rsp.newBuilder()
                    .setCmd(BFileCmd.RSP_COMPLETED)
                    .build();
            ctx.write(Unpooled.wrappedBuffer(senderMsgRsp.toByteArray()));
            ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

        }
    }



}
