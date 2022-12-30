package com.laiyz.client.task;

import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.SenderMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface ITask {
    public StatusEnum appendFileData(ChannelHandlerContext ctx, ByteBuf msg, SenderMsg.Rsp rsp, boolean writeProgress);
}
