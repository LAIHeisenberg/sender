package com.laiyz.client.base;

import com.alibaba.fastjson.JSON;
import com.laiyz.client.task.impl.FileTask;
import com.laiyz.client.task.impl.FileTaskListener;
import com.laiyz.comm.BFileInfo;
import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.CtxUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * used by FileRspHandler & ChunkedReadHandler
 */
@Slf4j
public class FileRspHelper {

    public static void handleFileData(ChannelHandlerContext ctx, BFileMsg.BFileRsp rsp, ByteBuf msg) {
        byte[] fileData = parseFileData(msg);
        FileTask fileTask = null;
        if ((fileTask = ClientCache.getTask(rsp.getId())) == null) {
            fileTask = new FileTask(rsp);
            ClientCache.addTask(rsp.getId(), fileTask);
            fileTask.addListner(new FileTaskListener());
        }
        // append part of file data to storage buffer(sbuf) of task
        StatusEnum status = fileTask.appendFileData(fileData);
        // all file data received, try to start next file download
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
            log.info("all files received, can stop client now(ChunkedWriteHandler need to wait).");
//            System.exit(0);
        }
    }



}
