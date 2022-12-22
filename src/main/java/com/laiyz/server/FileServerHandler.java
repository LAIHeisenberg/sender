/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.laiyz.server;

import com.laiyz.proto.BFileMsg;
import com.laiyz.server.base.ReqDispatcher;
import com.laiyz.util.ConstUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.RandomAccessFile;

/**
 * cannot used ObjectEncoder/ObjectDecoder, because FileRegion write bytes to socket channel directly,
 * and no api to set chunk to a object,
 *
 * the solution just send every chunk with BFileResponse prefix, and end with delimiter "__BBSTONE_BFILE_END__"
 *
 */
@Slf4j
public class FileServerHandler extends SimpleChannelInboundHandler<BFileMsg.BFileReq> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        ctx.writeAndFlush("HELLO: Type the path of the file to retrieve.\n");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, BFileMsg.BFileReq msg) throws Exception {
        if (msg != null) {
            ReqDispatcher.dispatch(ctx, msg);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();

        if (ctx.channel().isActive()) {
            ctx.writeAndFlush("ERR: " +
                    cause.getClass().getSimpleName() + ": " +
                    cause.getMessage() + '\n').addListener(ChannelFutureListener.CLOSE);
        }
    }


    private void writeChunk(ChannelHandlerContext ctx, RandomAccessFile raf, int pos, int length) {
        ctx.write(new DefaultFileRegion(raf.getChannel(), pos, length));
        ctx.writeAndFlush(ConstUtil.delimiter);
    }
}

