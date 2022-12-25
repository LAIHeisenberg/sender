package com.laiyz.client.task;

import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.BFileMsg;
import io.netty.channel.ChannelHandlerContext;

public interface ITask {
    public StatusEnum appendFileData(ChannelHandlerContext ctx, byte[] fileData, BFileMsg.BFileRsp rsp);
}
