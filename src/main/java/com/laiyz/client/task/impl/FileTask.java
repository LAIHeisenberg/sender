package com.laiyz.client.task.impl;

import com.laiyz.client.task.ITask;
import com.laiyz.client.task.TaskListener;
import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileTask implements ITask {

    private List<TaskListener> listener = new ArrayList<>();

    private String checksum = null;
    private long fileSize = 0; // total bytes
    private long recvSize = 0; // recv bytes(still in sbuf)
    private long saveSize = 0; // saved bytes(saved to disk)

    private FileOutputStream fos = null;

    private BFileMsg.BFileRsp rsp = null;


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
//    private File tempfile = null;

    private long startTime = System.currentTimeMillis();
    // 80 means 80%, 0 means 0%
    private int progress = 0;

    // recv times
    private int counter = 0;

    FileTask() {
    }

    public FileTask(BFileMsg.BFileRsp rsp) {
        init(rsp);
    }

    public void addListner(TaskListener listner) {
        listener.add(listner);
    }

    private void init(BFileMsg.BFileRsp rsp) {
        try {
            this.rsp = rsp;
            this.fileSize = rsp.getFileSize();
            this.checksum = rsp.getChecksum();

//            this.clientFullPath = BFileUtil.getClientFullPathWithCheck(rsp.getFilepath());
            String filepath = "/home/laiyz/下载/pisces/freemind.zip";
            this.clientFullPath = filepath;
//            checkFileExists(this.clientFullPath);

            this.tempFullPath = BFileUtil.getClientTempFileFullPath(clientFullPath);
//            checkFileExists(this.tempFullPath);

//            this.tempfile = new File(this.tempFullPath);

            fos = new FileOutputStream(new File(this.tempFullPath), true);
        } catch (FileNotFoundException e) {
            log.error(String.format("File Not Found. clientFullPath: %s, tempFullPath: %s", this.clientFullPath, this.tempFullPath), e);
        }
    }

    public StatusEnum appendFileData(byte[] fileData) {
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


        log.info("recv file data len: {}, progress: {}/{}({}%)", len, recvSize, fileSize, Math.floor((recvSize*1d/fileSize*1d) * 10000)/100 );
        boolean saveOK = false;
        // sbuf full or all file data received
        if (spos >= SBUF_SIZE || recvSize == fileSize) {
            // do save file data to disk
            saveOK = saveToDisk();
            // reset recvSize, sbuf
            if (!saveOK) {
                log.warn("save file data error, recvSize: {}, spos: {}, len: ", recvSize, spos, len);
                // TODO save file data fail, retry 3 times,
                //  then throw exception
                //  or write down break point and skip this file, after all file downloaded, report this situation
                return StatusEnum.ERR_SAVE_DATA;
            }
            saveSize += spos;
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
            long endTime = System.currentTimeMillis();
            log.debug("============ endTime: {}==========", endTime);

            long costTime = (endTime - rsp.getReqTs()) / 1000;
            log.info(">>>>>>>>>>>>>>> file transfer cost time: {} sec. <<<<<<<<<<<<<<<<<", costTime);
            for (TaskListener listener : listener) {
                listener.onCompleted(rsp);
            }
//            ClientCache.resetRecvFileKey();
//            ClientCache.removeTask(rsp.getId());
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

    private boolean saveToDisk() {
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
            return true;
        } catch (IOException e) {
            log.error("wrote data to disk error.", e);
            return false;
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

}
