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

import com.laiyz.client.base.ClientCache;
import com.laiyz.client.task.impl.FileTask;
import com.laiyz.client.task.impl.FileTaskListener;
import com.laiyz.comm.BFileCmd;
import com.laiyz.proto.BFileMsg;
import com.laiyz.proto.SenderMsg;
import com.laiyz.server.base.ReqDispatcher;
import com.laiyz.util.BFileUtil;
import com.laiyz.util.ConstUtil;
import com.laiyz.util.FileUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
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

/**
 * cannot used ObjectEncoder/ObjectDecoder, because FileRegion write bytes to socket channel directly,
 * and no api to set chunk to a object,
 *
 * the solution just send every chunk with BFileResponse prefix, and end with delimiter "__BBSTONE_BFILE_END__"
 *
 */
@Slf4j
public class FileServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        ctx.writeAndFlush("HELLO: Type the path of the file to retrieve.\n");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int len = msg.readableBytes();
        msg.markReaderIndex();
        byte[] prefixBytes = new byte[ConstUtil.sender_req_prefix_len];
        msg.readBytes(prefixBytes);
        if (ConstUtil.sender_req_prefix.equals(new String(prefixBytes, CharsetUtil.UTF_8))){
            byte[] reqBytes = new byte[len - ConstUtil.sender_req_prefix_len];
            msg.readBytes(reqBytes);
            SenderMsg.Req req = SenderMsg.Req.parseFrom(reqBytes);
            SenderMsg.Rsp rsp;
            switch (req.getCmd()){
                case BFileCmd.REQ_UPLOAD :
                    long cacheDataPosistion = BFileUtil.getTmpFileLength(req.getFilepath());
                    if (cacheDataPosistion > 0l){
                        FileTask fileTask;
                        if ((fileTask = ClientCache.getTask(req.getId())) != null) {
                            fileTask.resetRecvSize(cacheDataPosistion);
                        }
                    }
                    ctx.write(Unpooled.wrappedBuffer(ConstUtil.sender_req_prefix.getBytes(CharsetUtil.UTF_8)));
                    rsp = SenderMsg.Rsp.newBuilder()
                            .setId(req.getId())
                            .setFilepath(req.getFilepath())
                            .setAccessFilePosition(cacheDataPosistion)
                            .setCmd(BFileCmd.RSP_UPLOAD).build();
                    ctx.write(Unpooled.wrappedBuffer(rsp.toByteArray()));
                    ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
                    break;
                case BFileCmd.REQ_PULL:
                    String filePath = req.getFilepath();
                    long fileLength = BFileUtil.getFileLength(filePath);
                    if (fileLength > 0){
//                        FileUtil.send(ctx,filePath,req.getAccessFilePosition(),req.getTs());
                    }else {
                        ctx.write(Unpooled.wrappedBuffer(ConstUtil.sender_req_prefix.getBytes(CharsetUtil.UTF_8)));
                        rsp = SenderMsg.Rsp.newBuilder()
                                .setId(req.getId())
                                .setFilepath(req.getFilepath())
                                .setCmd(BFileCmd.RSP_FILE_NOT_FIND).build();
                        ctx.write(Unpooled.wrappedBuffer(rsp.toByteArray()));
                        ctx.writeAndFlush(Unpooled.wrappedBuffer(ConstUtil.delimiter.getBytes(CharsetUtil.UTF_8)));
                    }
                    break;
            }
        }else {
            msg.resetReaderIndex();

//            byte[] data = new byte[msg.readableBytes()];
//            msg.readBytes(data);
//            ctx.fireChannelRead(msg);
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



}

