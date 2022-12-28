package com.laiyz.client.cmd;

import com.laiyz.client.base.ClientCache;
import com.laiyz.client.base.FileRspHelper;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
import com.laiyz.proto.SenderMsg;
import com.laiyz.util.BFileUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.time.Instant;

@Slf4j
public class FilePullHandler extends CmdHandler {

    public void handle(ChannelHandlerContext ctx, SenderMsg.Rsp rsp, ByteBuf msg) {
        log.info("transferring file({})...", rsp.getFilepath());
        FileRspHelper.handleFileData(ctx, rsp, msg, false);
        long recvSize = rsp.getRecvSize();
        long fileSize = rsp.getFileSize();
        String result = String.format("receive file data, progress: %s/%s(%s%%) avg speed:%s remaining time: %s", recvSize, fileSize, Math.floor((recvSize*1d/fileSize*1d) * 10000)/100, calcAvgSpeed(rsp.getCurrRecvSize(), rsp.getReqTs()), calcTimeRemaining(fileSize-recvSize,rsp.getCurrRecvSize(),rsp.getReqTs()));
        System.out.print("\r"+result);

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
