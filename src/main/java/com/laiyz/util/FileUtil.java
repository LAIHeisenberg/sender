package com.laiyz.util;

import com.laiyz.client.base.ClientCache;
import com.laiyz.client.task.TaskListener;
import com.laiyz.client.task.impl.FileTask;
import com.laiyz.client.task.impl.FileTaskListener;
import com.laiyz.comm.BFileCmd;
import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.SenderMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;


@Slf4j

public class FileUtil {
/*

    public static void send(ChannelHandlerContext ctx, String filePath, long pos, long reqTs) {
        log.debug(">>>>>>>>>> sending file/dir: {}", filePath);
        File file = new File(filePath);
        if (Files.isDirectory(Paths.get(filePath))) {
            return;
        }
        // file checksume
        String checksum = BFileUtil.checksum(file);

        long filelen = file.length();

        long startTime = System.currentTimeMillis();
        // SSL enabled - cannot use zero-copy file transfer.

        long chunkSize;
        while ((filelen - pos) > 0) {
            */
/**
             * Standard Rsp format like:
             * +--------------------------------------------------------+
             * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info |
             * +--------------------------------------------------------+
             * <p>
             *//*


            */
/**
             * Non-standard format:
             * appending send following data to channel directly(not assemble to full data format because
             * FileRegion not support extract data (TBD)
             *
             * +------------------------+
             * | chunk_data | delimiter |
             * +------------------------+
             **//*

            chunkSize = Math.min(ConstUtil.DEFAULT_CHUNK_SIZE, (filelen - pos));
            log.debug("current pos: {}, will write {} bytes to channel.", pos, chunkSize);

            ByteBuf rspBuf = BFileUtil.buildPullFile(filePath, "", filelen, checksum, chunkSize,  reqTs);
            ctx.write(rspBuf);

            ctx.write(new DefaultFileRegion(file, pos, chunkSize));
            ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

//            currSendSize += chunkSize;
            pos += chunkSize;
        }

        log.info("write file({}) to channel cost time: {} sec.", filePath, (System.currentTimeMillis() - startTime) / 1000);
    }




    public static void receive(ChannelHandlerContext ctx, SenderMsg.Rsp rsp, ByteBuf msg, boolean writeProgress ) {
//        byte[] fileData = parseFileData(msg);
        FileTask fileTask = null;
        if ((fileTask = ClientCache.getTask(rsp.getId())) == null) {
            fileTask = new FileTask(rsp);
            ClientCache.addTask(rsp.getId(), fileTask);
            fileTask.addListner(new FileTaskListener());
        }

        StatusEnum status = fileTask.appendFileData(ctx, msg, rsp,  writeProgress);

        if (status == StatusEnum.COMPLETED) {
            log.info("file({}) transfer complete.", rsp.getFilepath());
            handleNext(ctx);
        }
    }


    public StatusEnum appendFileData(ChannelHandlerContext ctx,  ByteBuf msg, SenderMsg.Rsp rsp, boolean writeProgress) {
        counter++;
        int len = msg.readableBytes();
        if (len <= 0) {
            log.warn("recv file data is 0.");
            return StatusEnum.NO_DATA;
        }

        recvSize += len;
        currRecvSize += len;
//        msg.retain(1);

        boolean saveOK;
        long saveSize = 0l;

        // sbuf full or all file data received
//        if (compositeByteBuf.capacity() >= SBUF_SIZE || recvSize == fileSize) {
        saveSize = saveToDisk(msg);
        // do save file data to disk
        saveOK = saveSize > 0;
        if (!saveOK) {
            log.warn("save file data error, recvSize: {}, len: ", recvSize, len);
            return StatusEnum.ERR_SAVE_DATA;
        }
        log.info("saveOK, saveSize: {}, recvSize: {}, len: {}", saveSize, recvSize, len);


//        }
        // saveOK, and all bytes received
        if (recvSize == fileSize && recvSize == saveSize) {
            closeFos();
            // check file integrity
            String checkSum = BFileUtil.checksum(new File(this.tempFullPath));
            log.debug("server checksum: {}, client checksum: {}, isEq: {}", rsp.getChecksum(), checkSum, (rsp.getChecksum().equals(checkSum)));
            log.debug("filepath: {}", rsp.getFilepath());
            log.debug("clientFullPath: {}, tempFullPath: {}", clientFullPath, tempFullPath);
            if (rsp.getChecksum().equals(checkSum)) {
                BFileUtil.renameCliTempFile(new File(this.tempFullPath), clientFullPath);
                log.debug("temp file rename OK.");
            }
            long endTime = Instant.now().getEpochSecond();
            log.debug("============ endTime: {}==========", endTime);

            long costTime = (endTime - rsp.getReqTs());
            log.info(">>>>>>>>>>>>>>> file transfer cost time: {} sec. <<<<<<<<<<<<<<<<<", costTime);
            for (TaskListener listener : listener) {
                listener.onCompleted(rsp);
            }

            return StatusEnum.COMPLETED;
        }
        log.info("recvSize: {}, saveSize: {}, fileSize: {}", recvSize, saveSize, fileSize);
        // all file data recv, but some save to disk fail
        if (recvSize == fileSize && recvSize == saveSize) {
            closeFos();
            return StatusEnum.ERR_SAVE_DATA;
        }
        // unknown
        return StatusEnum.CONTINUE;
    }
*/

}
