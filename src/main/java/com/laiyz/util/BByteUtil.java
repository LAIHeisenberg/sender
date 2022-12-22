package com.laiyz.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

@Slf4j
public class BByteUtil {

    /** String -> byte[] */
    public static byte[] toBytes(String str) {
        return str.getBytes(CharsetUtil.UTF_8);
    }
    /** String -> byte[] */
    public static byte[] toBytes(String str, Charset charset) {
        return str.getBytes(charset);
    }

    /** byte[] -> String */
    public static String toStr(byte[] bytes) {
        return new String(bytes, CharsetUtil.UTF_8);
    }

    /** ByteBuf -> byte[] */
    public static byte[] toBytes(ByteBuf msg) {
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        return data;
    }
    /** byte[] -> ByteBuf */
    public static ByteBuf toBytesBuf(byte[] bytes) {
        return Unpooled.wrappedBuffer(bytes);
    }

    /** String -> ByteBuf */
    public static ByteBuf toByteBuf(String str) {
        return Unpooled.wrappedBuffer(str.getBytes(CharsetUtil.UTF_8));
    }
    /** String -> ByteBuf */
    public static ByteBuf toByteBuf(String str, Charset charset) {
        return Unpooled.wrappedBuffer(str.getBytes(charset));
    }

    /** string's bytes length */
    public static int byteSize(String str) {
        return str.getBytes(CharsetUtil.UTF_8).length;
    }
    /** string's bytes length */
    public static int byteSize(String str, Charset charset) {
        return str.getBytes(charset).length;
    }

    public static int magicLen() {
        return ConstUtil.magicLen;
    }

    public static String magic() {
        return ConstUtil.magic;
    }



    // Little Endian Byte Order
    public static byte[] toBytesLE(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static byte[] toBytes(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }


    private static ByteBuffer buffer = ByteBuffer.allocate(8);
    // long -> byte[]
    public static byte[] toBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    // byte[] -> long
    public static long toLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }




}
