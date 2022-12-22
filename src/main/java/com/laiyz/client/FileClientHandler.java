package com.laiyz.client;

import com.laiyz.client.base.RspDispatcher;
import com.laiyz.comm.BFileCmd;
import com.laiyz.proto.BFileMsg;
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
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class FileClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    String serverRelativeFile = ""; //Config.serverDir;
    /**
     * Creates a client-side handler.
     */
    public FileClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // req file list
//        BFileMsg.BFileReq req = BFileUtil.buildReq(BFileCmd.REQ_LIST, serverRelativeFile);
//        BFileMsg.BFileReq uploadReq = BFileUtil.buildReq(BFileCmd.REQ_UPLOAD, "filePath");

//        ctx.write(uploadReq);

//        ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

        long reqTs = System.currentTimeMillis();
        // TODO filepath should be relative path of server.dir send by client,
        // so the first request from client should be CMD_LIST to get server.dir file list
        String filepath = "/home/laiyz/下载/pisces/freemind-bin-max-1.0.1.zip";
        String serverpath = filepath;

        sendFile(ctx, serverpath, reqTs);
        log.debug("------> server done sent file: {}", serverpath);
//        System.exit(0);
    }

    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        log.debug("client recv total readableBytes: {}", msg.readableBytes());
//        RspDispatcher.dispatch(ctx, msg);
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
     * @param serverpath -  server file full path
     * @param reqTs      - timestamp of client request this file
     */
    private void sendFile(ChannelHandlerContext ctx, String serverpath, long reqTs) {
        log.debug(">>>>>>>>>> sending file/dir: {}", serverpath);
//        if (Files.isDirectory(Paths.get(serverpath))) {
//            doSendDir(ctx, serverpath, reqTs);
//            return;
//        }
        // file checksume
        String checksum = BFileUtil.checksum(new File(serverpath));

        // file content size of server file(which path is filepath)
        File serverFile = new File(serverpath);

        long filelen = serverFile.length();
        log.debug("filelen: {}, write BFileRsp to client......", filelen);


        long startTime = System.currentTimeMillis();
        // SSL enabled - cannot use zero-copy file transfer.
        if (ctx.pipeline().get(SslHandler.class) != null) {
            // send BFileRsp header info
            ByteBuf rspBuf = BFileUtil.buildRspFile(serverpath, filelen, checksum, reqTs);
            ctx.write(rspBuf);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));

            // send ChunkedFile data
            try {
                ctx.writeAndFlush(new ChunkedFile(serverFile));
            } catch (IOException e) {
                log.error("write and flush chunked file error.", e);
            }
        } else { // zero-copy FileRegion mode

            long pos = 0;
            int chunkCounter = 0;
            int chunkSize = 0;
            while ((filelen - pos) > 0) {
                /**
                 * Standard Rsp format like:
                 * +--------------------------------------------------------+
                 * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info |
                 * +--------------------------------------------------------+
                 * <p>
                 */
                ByteBuf rspBuf = BFileUtil.buildRspFile(serverpath, filelen, checksum, reqTs);
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
                 */
                chunkSize = (int) Math.min(ConstUtil.DEFAULT_CHUNK_SIZE, (filelen - pos));
                log.debug("current pos: {}, will write {} bytes to channel.", pos, chunkSize);

                // SSL not enabled - can use zero-copy file transfer.
//                ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
                // DefaultFileRegion need to pass new File(filepath) other than raf.getChannel(),
                // because every time new DefaultFileRegion, open a new raf
                ctx.write(new DefaultFileRegion(serverFile, pos, chunkSize));

                ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
                log.debug("output file: {}", serverpath);
                chunkCounter++;
                pos += chunkSize;
                log.info("=============== wrote the {} chunk, wrote len: {}, progress: {}/{} =============", chunkCounter, (rspInfoLen + chunkSize), pos, filelen);
            }
        }
        log.info("write file({}) to channel cost time: {} sec.", serverpath, (System.currentTimeMillis() - startTime) / 1000);
//        log.info("transferred files: {}/{}", );
    }


}
