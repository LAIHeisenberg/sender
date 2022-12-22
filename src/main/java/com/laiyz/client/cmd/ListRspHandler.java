package com.laiyz.client.cmd;

import com.alibaba.fastjson.JSON;
import com.laiyz.client.base.ClientCache;
import com.laiyz.client.base.FileRspHelper;
import com.laiyz.comm.BFileCombo;
import com.laiyz.comm.BFileInfo;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class ListRspHandler extends CmdHandler {

    @Override
    public void handle(ChannelHandlerContext ctx, BFileMsg.BFileRsp rsp, ByteBuf msg) {
        String rspData = rsp.getRspData();
        log.debug("client recv fileTree: \n / \n{}", rspData);


        BFileCombo combo = JSON.parseObject(rspData, BFileCombo.class);
        log.debug("BFile combo: {}", combo);

        List<BFileInfo> serverFileList = combo.getInfoList();
        log.debug("server fileList: {}", JSON.toJSONString(serverFileList));

        List<BFileInfo> clientFileList = BFileUtil.findClientFiles(Config.clientDir());
        log.debug("client exists fileList: {}", JSON.toJSONString(clientFileList));

        List<BFileInfo> needTransferFileList = serverFileList.stream().filter(e -> !clientFileList.contains(e)).collect(Collectors.toList());
//        Set<BFileInfo> needTransferFileList = Sets.difference(Sets.newHashSet(clientFileList), Sets.newHashSet(serverFileList));
        log.info("needTransferFileList : {}", needTransferFileList);
        if (needTransferFileList.size() == 0) {
            log.info("client dir has all file requested, no more file need transferred.");
            return;
        }
        ClientCache.init(combo, needTransferFileList);
        FileRspHelper.handleNext(ctx);
    }

}
