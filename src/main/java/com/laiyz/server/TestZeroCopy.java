package com.laiyz.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import sun.nio.ch.FileChannelImpl;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class TestZeroCopy {

    private static void compositeZeroCopy(){
        // 创建两个小的ByteBuf缓冲区，并往两个缓冲区中插入数据
        ByteBuf b1 = ByteBufAllocator.DEFAULT.buffer(5);
        ByteBuf b2 = ByteBufAllocator.DEFAULT.buffer(5);
        byte[] data1 = {'a','b','c','d','e','z'};
        byte[] data2 = {'n','m','x','y','z'};
        b1.writeBytes(data1);
        b2.writeBytes(data2);

        // 创建一个合并缓冲区的CompositeByteBuf对象
        CompositeByteBuf buffer = ByteBufAllocator.DEFAULT.compositeDirectBuffer();
        // 将前面两个小的缓冲区，合并成一个大的缓冲区
        buffer.addComponent(true,b1);
        buffer.addComponent(true, b2);

//        b1.clear();
//        b1.retain();
//        b1.release();

//        ByteBuf b11 = b1.duplicate();
//        buffer.addComponent(true, b11);
        buffer.clear();
        buffer.release();
        buffer = ByteBufAllocator.DEFAULT.compositeDirectBuffer();

        byte[] data3 = {'o','p','q','r','s'};
        ByteBuf b3 = ByteBufAllocator.DEFAULT.buffer(5);
        b3.writeBytes(data3);
        buffer.addComponent(true, b3);
//        buffer.addComponent(true, b1);
        System.out.println(ByteBufUtil.prettyHexDump(buffer,0,buffer.readableBytes()));
//        System.out.println(ByteBufUtil.prettyHexDump(b1,0,buffer.readableBytes()));

    }

    public static void main(String[] args) {
        compositeZeroCopy();
    }


}
