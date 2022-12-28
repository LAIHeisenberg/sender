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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FileTask implements ITask {

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
    // accumulate SBUF_SIZE before save file data
    private byte[] sbuf = new byte[SBUF_SIZE];
    // sbuf next write position
    private int spos = 0;

    private String clientFullPath = null;
    private String tempFullPath = null;


    private long startTime = System.currentTimeMillis();
    // 80 means 80%, 0 means 0%
    private int progress = 0;

    // recv times
    private int counter = 0;

    FileTask() {
    }

    public FileTask(SenderMsg.Rsp rsp) {
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
//            checkFileExists(this.clientFullPath);

            this.tempFullPath = BFileUtil.getClientTempFileFullPath(clientFullPath);

//            this.tempFullPath = BFileUtil.getClientTempFileFullPath();
//            checkFileExists(this.tempFullPath);

//            this.tempfile = new File(this.tempFullPath);

            fos = new FileOutputStream(new File(this.tempFullPath), true);
            this.recvSize = BFileUtil.getTmpFileLength(rsp.getFilepath());

        } catch (FileNotFoundException e) {
            log.error(String.format("File Not Found. clientFullPath: %s, tempFullPath: %s", this.clientFullPath, this.tempFullPath), e);
        }
    }

    public void resetRecvSize(long recvSize){
        this.recvSize = recvSize;
        this.spos = 0;
        this.currRecvSize = 0;
    }

    @Override
    public StatusEnum appendFileData(ChannelHandlerContext ctx, byte[] fileData, SenderMsg.Rsp rsp, boolean writeProgress) {
        counter++;
        int len = fileData.length;
        if (len <= 0) {
            log.warn("recv file data is 0.");
            return StatusEnum.NO_DATA;
        }

        checkSBufSpace(len);
        // append data to storage buffer(sbuf)
        System.arraycopy(fileData, 0, sbuf, spos, len);
        // increase sbuf next write pos
        spos += len;
        recvSize += len;
        currRecvSize += len;

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
        if (spos >= SBUF_SIZE || recvSize == fileSize) {
            // do save file data to disk
            saveSize = saveToDisk();
            saveOK = saveSize > 0;
            // reset recvSize, sbuf
            if (!saveOK) {
                log.warn("save file data error, recvSize: {}, spos: {}, len: ", recvSize, spos, len);
                return StatusEnum.ERR_SAVE_DATA;
            }
            log.info("saveOK, saveSize: {}, recvSize: {}, spos: {}, len: {}", saveSize, recvSize, spos, len);
            resetSbuf();
        }
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

    /**
     * check if append file data will exceed sbuf capacity,
     * expand sbuf capacity for saving this appending data
     *
     * @param appendLen
     */
    private void checkSBufSpace(int appendLen) {
        if ( (spos + appendLen) >= SBUF_SIZE ) {
            byte[] sbuf2 = new byte[spos + appendLen];
            System.arraycopy(sbuf, 0, sbuf2, 0, (spos+1));
            sbuf = sbuf2;
        }
    }

    private void checkFileExists(String filepath) {
        if (Files.exists(Paths.get(filepath))) {
            try {
                Files.delete(Paths.get(filepath));
            } catch (IOException e) {
                log.error(String.format("delete exist file(%s) error.", filepath), e);
            }
        }
    }

    private long saveToDisk() {
        try {
            // sbuf full data
            if (spos >= SBUF_SIZE) {
                fos.write(sbuf);
            } else { // sbuf not full data
                byte[] wdata = new byte[spos];
                System.arraycopy(sbuf, 0, wdata, 0, spos);
                fos.write(wdata);
            }

            log.debug("wrote data to disk ...");
            return fos.getChannel().size();
        } catch (IOException e) {
            log.error("wrote data to disk error.", e);
            return 0;
        }
    }

    private void resetSbuf() {
        spos = 0;
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
