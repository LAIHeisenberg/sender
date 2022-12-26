package com.laiyz.server;

import com.alibaba.fastjson.JSON;
import com.laiyz.client.base.ClientCache;
import com.laiyz.client.base.FileRspHelper;
import com.laiyz.client.base.RspDispatcher;
import com.laiyz.client.task.impl.FileTask;
import com.laiyz.client.task.impl.FileTaskListener;
import com.laiyz.comm.BFileCmd;
import com.laiyz.comm.BFileInfo;
import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import com.laiyz.util.ConstUtil;
import com.laiyz.util.CtxUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileServerHandler2 extends SimpleChannelInboundHandler<ByteBuf> {

    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        RspDispatcher.dispatch(ctx, msg);
    }

}
