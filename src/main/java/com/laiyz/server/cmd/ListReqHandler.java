package com.laiyz.server.cmd;

import com.alibaba.fastjson.JSON;
import com.laiyz.comm.BFileCombo;
import com.laiyz.comm.BFileInfo;
import com.laiyz.comm.BFileTreeNode;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import com.laiyz.util.ConstUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ListReqHandler implements CmdHandler {
    @Override
    public void handle(ChannelHandlerContext ctx, BFileMsg.BFileReq msg) {
        log.debug("filepath: {}", msg.getFilepath());
        long reqTs = msg.getTs();
        String filepath = msg.getFilepath();
        listDir(ctx, filepath, reqTs);
    }

    /**
     * data format:
     * +---------------------------------------------------------------------------------+
     * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info | chunk_data | delimiter |
     * +---------------------------------------------------------------------------------+
     * <p>
     * bfile_info_prefix:
     *
     */
    private void listDir(ChannelHandlerContext ctx, String filepath, long reqTs) {
        /*
        String fileTree = BFileUtil.list(filepath);
        ctx.write(fileTree);
        log.debug("fileTree: {} {}, {}", BFileUtil.LF, filepath, fileTree);
        ByteBuf rspBuf = BFileUtil.buildRspList(filepath, fileTree, reqTs);
        ctx.write(rspBuf);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
        */

        String serverFullPath = BFileUtil.getServerDir() + filepath;
        List<BFileInfo> fileList = BFileUtil.findServerFiles(serverFullPath);

        BFileTreeNode treeNode = BFileUtil.findServerFileTree(serverFullPath);

        BFileCombo combo = new BFileCombo(fileList, treeNode);

        String filesJson = JSON.toJSONString(combo);

//        String filesJson = JSON.toJSONString(fileList);
        ByteBuf rspBuf = BFileUtil.buildRspList(serverFullPath, filesJson, reqTs);
        ctx.write(rspBuf);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
    }
}
