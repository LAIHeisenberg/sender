package com.laiyz.client.task.impl;

import com.laiyz.client.task.ITask;
import com.laiyz.client.task.TaskListener;
import com.laiyz.comm.BFileCmd;
import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.SenderMsg;
import com.laiyz.util.BFileUtil;
import com.laiyz.util.ConstUtil;
import com.laiyz.util.ThreadPoolUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NewFileTask implements ITask {

    private List<TaskListener> listener = new ArrayList<>();

    private String checksum = null;
    private long fileSize = 0; // total bytes
    private long recvSize = 0; // recv bytes(still in sbuf)
    private long currRecvSize = 0;

    private FileOutputStream fos = null;

    private SenderMsg.Rsp rsp = null;


    // 8k * 8 = 64k
//    private final int SBUF_SIZE = 8192 * 8;
    // 4M
    private final int SBUF_SIZE = 4 * 1024 * 1024;

    private String clientFullPath = null;
    private String tempFullPath = null;


    private long startTime = System.currentTimeMillis();
    // 80 means 80%, 0 means 0%
    private int progress = 0;

    // recv times
    private int counter = 0;

    NewFileTask() {
    }

    public NewFileTask(SenderMsg.Rsp rsp) {
        init(rsp);
    }

    public void addListner(TaskListener listner) {
        listener.add(listner);
    }

    private void init(SenderMsg.Rsp rsp) {
        try {
            this.rsp = rsp;
            this.fileSize = rsp.getFileSize();
            this.checksum = rsp.getChecksum();

            this.clientFullPath = BFileUtil.getClientFullPathWithCheck(rsp.getFilepath());

            this.tempFullPath = BFileUtil.getClientTempFileFullPath(clientFullPath);


            fos = new FileOutputStream(new File(this.tempFullPath), true);
            this.recvSize = BFileUtil.getTmpFileLength(rsp.getFilepath());

        } catch (FileNotFoundException e) {
            log.error(String.format("File Not Found. clientFullPath: %s, tempFullPath: %s", this.clientFullPath, this.tempFullPath), e);
        }
    }

    public void resetRecvSize(long recvSize){
        this.recvSize = recvSize;
        this.currRecvSize = 0;
    }

    @Override
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

        if (writeProgress){
            ThreadPoolUtil.submitTask(() -> {
                ctx.write(Unpooled.wrappedBuffer(ConstUtil.sender_req_prefix.getBytes(CharsetUtil.UTF_8)));
                SenderMsg.Rsp senderMsgRsp = SenderMsg.Rsp.newBuilder()
                        .setId(rsp.getId())
                        .setCmd(BFileCmd.RSP_UPLOAD_PROGRESS)
                        .setFileSize(rsp.getFileSize())
                        .setReqTs(rsp.getReqTs())
                        .setRecvSize(recvSize)
                        .setCurrRecvSize(currRecvSize)
                        .build();
                ctx.write(Unpooled.wrappedBuffer(senderMsgRsp.toByteArray()));
                ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
            });
        }

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

    private long saveToDisk(ByteBuf byteBuf){
        try {
            byteBuf.readBytes(fos.getChannel(),0, byteBuf.readableBytes());
            return fos.getChannel().size();
        }catch (Exception e){
            log.error("wrote data to disk error.", e);
            return 0;
        }
    }

    private void closeFos() {
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String calcAvgSpeed(long recvSize, long reqTs){

        reqTs = reqTs / 1000;
        long currTs = System.currentTimeMillis() / 1000;
        double xKb = recvSize * 1d / Math.max((currTs-reqTs),0.5) / 1024;
        if (xKb < 1024){
            DecimalFormat df = new DecimalFormat("####.00");
            return df.format(xKb)+"KB/S";
        }else {
            double xmb = xKb / 1024;
            DecimalFormat df = new DecimalFormat("#0.00");
            return df.format(xmb)+"MB/S";
        }

    }

}
