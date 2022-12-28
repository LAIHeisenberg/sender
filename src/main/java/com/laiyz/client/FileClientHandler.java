package com.laiyz.client;

import com.laiyz.client.base.RspDispatcher;
import com.laiyz.comm.BFileCmd;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
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
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;


@Slf4j
public class FileClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private String filePath;
    /**
     * Creates a client-side handler.
     */
    public FileClientHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // req file list

        SenderMsg.Req req = BFileUtil.buildSenderReq(BFileCmd.REQ_UPLOAD, this.filePath);

        ctx.write(Unpooled.wrappedBuffer(ConstUtil.sender_req_prefix.getBytes(CharsetUtil.UTF_8)));
        ctx.write(Unpooled.wrappedBuffer(req.toByteArray()));
        ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

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
                case BFileCmd.RSP_UPLOAD:
                    long accessFilePosition = rsp.getAccessFilePosition();
                    sendFile(ctx, this.filePath, accessFilePosition, Instant.now().getEpochSecond());
                    break;
                case BFileCmd.RSP_UPLOAD_PROGRESS:
                    long fileSize = rsp.getFileSize();
                    long recvSize = rsp.getRecvSize();
                    String result = String.format("send file data, progress: %s/%s(%s%%) avg speed:%s remaining time: %s", recvSize, fileSize, Math.floor((recvSize*1d/fileSize*1d) * 10000)/100, calcAvgSpeed(rsp.getCurrRecvSize(), rsp.getReqTs()), calcTimeRemaining(fileSize-recvSize,rsp.getCurrRecvSize(),rsp.getReqTs()));
                    System.out.print("\r"+result);
//                    log.info("\r send file data, progress: {}/{}({}%) avg speed:{}", recvSize, fileSize, Math.floor((recvSize*1d/fileSize*1d) * 10000)/100, calcAvgSpeed(rsp.getCurrRecvSize(), rsp.getReqTs()));
                    break;
                case BFileCmd.RSP_COMPLETED:
                    System.out.println("\nall files send.");
                    break;
            }
//            log.info(result);
//            if ("all completed".equals(result)){
//                System.exit(0);
//            }
        }

    }



    /**
     * Non-standard format(append byte[] data after Rsp object, because cannot retrieve the data and set to Rsp.chunkData:
     * e.g. FileRsp(cmd: CMD_REQ) data format(only for FileRegion which directly write file data to channel):
     * +---------------------------------------------------------------------------------+
     * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info | chunk_data | delimiter |
     * +---------------------------------------------------------------------------------+
     * <p>
     *
     * @param ctx
     * @param filePath -  file full path
     * @param reqTs      - timestamp of client request this file
     */
    private void sendFile(ChannelHandlerContext ctx, String filePath, long pos, long reqTs) {
        log.debug(">>>>>>>>>> sending file/dir: {}", filePath);
        File file = new File(filePath);
        if (Files.isDirectory(Paths.get(filePath))) {
            sendDir(ctx, file, reqTs);
            return;
        }
        // file checksume
        String checksum = BFileUtil.checksum(file);

        long filelen = file.length();
        log.debug("filelen: {}, write BFileRsp to server......", filelen);

        long startTime = System.currentTimeMillis();
        // SSL enabled - cannot use zero-copy file transfer.
        if (ctx.pipeline().get(SslHandler.class) != null) {
            // send BFileRsp header info
            ByteBuf rspBuf = BFileUtil.buildRspFile(filePath, filelen, checksum, reqTs);
            ctx.write(rspBuf);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

            // send ChunkedFile data
            try {
                ctx.writeAndFlush(new ChunkedFile(file));
            } catch (IOException e) {
                log.error("write and flush chunked file error.", e);
            }
        } else { // zero-copy FileRegion mode

            int chunkCounter = 0;
            int chunkSize;
            while ((filelen - pos) > 0) {
                /**
                 * Standard Rsp format like:
                 * +--------------------------------------------------------+
                 * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info |
                 * +--------------------------------------------------------+
                 * <p>
                 */
                ByteBuf rspBuf = BFileUtil.buildRspFile(filePath, filelen, checksum, reqTs);
                int rspInfoLen = rspBuf.readableBytes();
                ctx.write(rspBuf);
                /**
                 * Non-standard format:
                 * appending send following data to channel directly(not assemble to full data format because
                 * FileRegion not support extract data (TBD)
                 *
                 * +------------------------+
                 * | chunk_data | delimiter |
                 * +------------------------+
                **/
                chunkSize = (int) Math.min(ConstUtil.DEFAULT_CHUNK_SIZE, (filelen - pos));
                log.debug("current pos: {}, will write {} bytes to channel.", pos, chunkSize);

                ctx.write(new DefaultFileRegion(file, pos, chunkSize));
                ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

                chunkCounter++;
                pos += chunkSize;
//                log.info("=============== wrote the {} chunk, wrote len: {}, progress: {}/{} =============", chunkCounter, (rspInfoLen + chunkSize), pos, filelen);
            }
        }
        log.info("write file({}) to channel cost time: {} sec.", filePath, (System.currentTimeMillis() - startTime) / 1000);

    }

    private void sendDir(ChannelHandlerContext ctx, File file, long reqTs) {
        File[] flist = file.listFiles();
//        Arrays.sort(flist);
        for (File subfile : flist) {
            if (subfile.isDirectory()) {
                sendDir(ctx, subfile, reqTs);
            }
            sendFile(ctx, subfile.getAbsolutePath(),0, reqTs);
        }
    }

    private String calcAvgSpeed(long currRecvSize, long reqTs){
        long currTs = Instant.now().getEpochSecond();
        double xKb = currRecvSize * 1d / Math.max((currTs-reqTs),0.5) / 1024;
        if (xKb < 1024){
            DecimalFormat df = new DecimalFormat("####.00");
            return df.format(xKb)+"KB/S";
        }else {
            double xmb = xKb / 1024;
            DecimalFormat df = new DecimalFormat("#0.00");
            return df.format(xmb)+"MB/S";
        }
    }

    private String calcTimeRemaining(long remainSize ,long currRecvSize, long reqTs){

        long currTs = Instant.now().getEpochSecond();
        double speed = currRecvSize * 1d / Math.max((currTs-reqTs),0.5);
        double remainSec = remainSize / speed;

        int hour = (int)remainSec/3600;
        int mins = (int)remainSec % 3600 / 60;
        int sec = (int)remainSec % 3600 % 60;
        StringBuffer sbf = new StringBuffer();
        if (hour > 0){
            sbf.append(hour+" hour ");
        }
        if (mins > 0){
            sbf.append(mins+" min ");
        }
        sbf.append(sec+"sec ");
        sbf.append("/ total sec "+(int)remainSec);
        return sbf.toString();

    }

}
