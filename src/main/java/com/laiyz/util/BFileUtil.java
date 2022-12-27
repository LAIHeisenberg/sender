package com.laiyz.util;

import com.google.protobuf.ByteString;
import com.laiyz.comm.BFileCmd;
import com.laiyz.comm.BFileInfo;
import com.laiyz.comm.BFileTreeNode;
import com.laiyz.config.Config;
import com.laiyz.proto.BFileMsg;
import com.laiyz.proto.SenderMsg;
import com.twmacinta.util.MD5;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;

@Slf4j
public class BFileUtil {

    public static final String LF = "\n";

    private static int level = -1;

    public static boolean isBFileStream(ByteBuf msg) {
        int len = msg.readableBytes();
        log.debug("msg.len: {}", len);

        if (len < ConstUtil.bfile_info_prefix_len)
            return false;
        msg.markReaderIndex();
        byte[] data = new byte[ConstUtil.bfile_info_prefix_len];
        msg.readBytes(data);
        String prefix = BByteUtil.toStr(data);
        if (ConstUtil.bfile_info_prefix.equals(prefix)) {
            return true;
        }

        // stream not start with magic
        msg.resetReaderIndex();
        return false;
    }

    public static boolean isBFileStream(byte[] msg) {
        if (msg == null || msg.length < ConstUtil.magicLen)
            return false;
        byte[] data = new byte[ConstUtil.magicLen];
        System.arraycopy(msg, 0, data, 0, ConstUtil.magicLen);
        String magic = BByteUtil.toStr(data);
        if (ConstUtil.magic.equals(magic)) {
            return true;
        }
        return false;
    }


    public static String list(String filepath) {
        if (Files.notExists(Paths.get(filepath))) {
            log.warn("not found file/directory: {}", filepath);
        }
        StringBuilder sbu = new StringBuilder();
        File file = new File(filepath);
        if (Files.isDirectory(Paths.get(filepath))) {
            level = -1; // reset
            listAll(sbu, new File(filepath));
        } else {
            sbu.append(file.getName());
        }
        return sbu.toString();
    }

    private static void listAll(StringBuilder sbu, File file) {
        level++;
        File[] flist = file.listFiles();
        Arrays.sort(flist);
        for (File subfile : flist) {
            for (int i = 0; i < level; i++) {
                sbu.append("|   ");
            }
            sbu.append("|-- " + subfile.getName()).append(LF);
            if (subfile.isDirectory()) {
                listAll(sbu, subfile);
            }
        }
        level--;
    }


    public static BFileTreeNode findServerFileTree(String filepath) {
        return findFileTree(filepath, getServerDir());
    }

    private static BFileTreeNode findFileTree(String filepath, String basePath) {
        if (Files.notExists(Paths.get(filepath))) {
            log.warn("not found file/directory: {}", filepath);
        }
        File file = new File(filepath);
        BFileTreeNode root = null;
        if (Files.isDirectory(Paths.get(filepath))) {
            root = new BFileTreeNode(file.getAbsolutePath(), file.getName(), ConstUtil.BFILE_CAT_DIR);
            findFileTree(root, file, basePath);
        } else {
            root = new BFileTreeNode(file.getAbsolutePath(), file.getName(), ConstUtil.BFILE_CAT_FILE);
        }
        return root;
    }

    private static void findFileTree(BFileTreeNode parent, File file, String basePath) {
        File[] flist = file.listFiles();
        Arrays.sort(flist);
        for (File subfile : flist) {
            BFileTreeNode node = new BFileTreeNode(subfile.getAbsolutePath(), subfile.getName(), subfile.isDirectory() ? ConstUtil.BFILE_CAT_DIR : ConstUtil.BFILE_CAT_FILE);
            parent.getChildren().add(node);
            if (subfile.isDirectory()) {
                findFileTree(node, subfile, basePath);
            }
        }
    }

    public static List<BFileInfo> findServerFiles(String filepath) {
        return findFiles(filepath, getServerDir());
    }

    public static List<BFileInfo> findClientFiles(String filepath) {
        return findFiles(filepath, getClientDir());
    }


    /**
     * return all sub-files relative to server.dir
     *
     * @param filepath
     * @return
     */
    private static List<BFileInfo> findFiles(String filepath, String basePath) {
        if (Files.notExists(Paths.get(filepath))) {
            log.warn("not found file/directory: {}", filepath);
        }
        List<BFileInfo> fileList = new ArrayList<>();
        File file = new File(filepath);
        if (Files.isDirectory(Paths.get(filepath))) {
            findFile(fileList, file, basePath);
        } else {
            String relativePath = getRelativePath(file.getAbsolutePath(), basePath);
            BFileInfo fileInfo = new BFileInfo(relativePath, ConstUtil.BFILE_CAT_FILE, checksum(file), file.length());
            fileList.add(fileInfo);
        }
        return fileList;
    }

    private static void findFile(List<BFileInfo> fileList, File file, String basePath) {
        File[] flist = file.listFiles();
        Arrays.sort(flist);
        for (File subfile : flist) {
            String relativePath = getRelativePath(subfile.getAbsolutePath(), basePath);
            BFileInfo fileInfo = new BFileInfo(relativePath,
                    subfile.isDirectory() ? ConstUtil.BFILE_CAT_DIR : ConstUtil.BFILE_CAT_FILE,
                    // dir- md5(relativePath), file - md5(file:file_content)
                    subfile.isDirectory() ? checksum(relativePath) : checksum(subfile),
                    subfile.length());
            fileList.add(fileInfo);
            if (subfile.isDirectory()) {
                findFile(fileList, subfile, basePath);
            }
        }
    }
    // --------------------------

    /**
     * file is directory, md5(file_abs_path),
     * <p>
     * file is file, return file fingerprint
     *
     * @param file - dir/file
     * @return
     */
    public static String checksum(File file) {
        return file.isDirectory() ? CipherUtil.md5(file.getAbsolutePath()) : CipherUtil.md5(file);
    }

    /**
     * calculate the string argument's md5,
     * but not the string path relative file's md5
     *
     * @param path - string path
     * @return string argument's md5
     */
    public static String checksum(String path) {
//        if (Files.exists(Paths.get(path))) {
//            return CipherUtil.md5(Paths.get(path).toFile());
//        }
        return CipherUtil.md5(path);
    }


    public static String checksum(byte[] bytes) {
        return CipherUtil.md5(bytes);
    }

    /**
     * @param relativePath - server relative path(base on: server.dir)
     * @param check        - check path or not
     * @return
     */
    public static String getClientFullPathWithCheck(String relativePath, boolean check) {
//        String relativePath = getCanonicalPath(filepath).substring(getServerDir().length());
        String clipath = getClientDir() + getCanonicalRelativePath(relativePath);
        String clientpath = convertToLocalePath(clipath);
        if (check)
            checkPath(clientpath);
        return clientpath;
    }

    /**
     * convert path which a server os path to client os path
     * e.g. server is *nix, client is windows,
     * need to convert server path format to client os platform format
     *
     * @param path
     * @return
     */
    public static String convertToLocalePath(String path) {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            return path.replaceAll(ConstUtil.NIX_FILE_SEPARATOR, Matcher.quoteReplacement(File.separator));
        } else {
            return path.replaceAll(ConstUtil.WIN_FILE_SEPARATOR, File.separator);
        }
    }

    /**
     * @param serverFullPath
     * @return
     */
    public static String getServerRelativePath(String serverFullPath) {
        return getRelativePath(serverFullPath, getServerDir());
//        serverFullPath = getCanonicalPath(serverFullPath);
//        if (Files.notExists(Paths.get(serverFullPath)) || !serverFullPath.startsWith(getServerDir())) {
//            throw new RuntimeException(String.format("@param(filepath: %s) should be a exists server path and started with(%s).", serverFullPath, getServerDir()));
//        }
//        return getCanonicalRelativePath(serverFullPath.substring(getServerDir().length()));
    }

    public static String getClientRelativePath(String clientFullPath) {
        return getRelativePath(clientFullPath, getClientDir());
    }

    private static String getRelativePath(String fullPath, String basePath) {
        fullPath = getCanonicalPath(fullPath);
//        log.info("fullPath: {}, basePath: {}", fullPath, basePath);
        if (Files.notExists(Paths.get(fullPath)) || !fullPath.startsWith(basePath)) {
            throw new RuntimeException(String.format("@param(filepath: %s) should be an exists client path and started with(%s).", fullPath, basePath));
        }
        return getCanonicalRelativePath(fullPath.substring(basePath.length()));
    }

//    public static String getClientFullPath(String serverRelativePath) {
//        return getClientDir() + getCanonicalRelativePath(serverRelativePath);
//    }

    public static String getClientDir() {
        String clientdir = Config.clientDir();
        if (StringUtils.isBlank(clientdir)) {
            throw new RuntimeException("client.dir property cannot be empty.");
        }
        return getCanonicalPath(clientdir);
    }

    public static String getServerDir() {
        String serverdir = Config.serverDir();
        if (StringUtils.isBlank(serverdir)) {
            throw new RuntimeException("server.dir property cannot be empty.");
        }
        return getCanonicalPath(serverdir);
    }

    /**
     * if path not end with File.separator, append to it before return
     *
     * @param path
     * @return
     */
    private static String getCanonicalPath(String path) {
        path = convertToLocalePath(path);
        // append last File.separator for dir
        if (Files.isDirectory(Paths.get(path))) {
            path = (path.lastIndexOf(File.separator) == (path.length() - 1)) ? path : path + File.separator;
        }
        return path;
    }

    /**
     * remove the first File.separator
     *
     * @param path
     * @return
     */
    private static String getCanonicalRelativePath(String path) {
        path = convertToLocalePath(path);
        // remove the first File.separator
        if (path.startsWith(File.separator)) {
            path = path.substring(File.separator.length());
        }
        return path;
    }

    /**
     * @param relativePath
     * @return
     */
    public static String getClientFullPathWithCheck(String relativePath) {
        return getClientFullPathWithCheck(relativePath, Boolean.TRUE);
    }

    private static void checkPath(String clipath) {
        String dirpath = getCanonicalPath(clipath).substring(0, clipath.lastIndexOf(File.separator));
        if (Files.notExists(Paths.get(dirpath))) {
            mkdir(dirpath);
        }
        log.debug("checkPath-> dirpath: {}", dirpath);
    }

    /**
     * create directory
     *
     * @param dirpath -  a directory path
     */
    public static void mkdir(String dirpath) {
        if (Files.notExists(Paths.get(dirpath))) {
            try {
                Files.createDirectories(Paths.get(dirpath));
                log.debug(">>>>>>>>>>>>>> created dir: {}", dirpath);
            } catch (IOException e) {
                log.error("dir create fail. ", e);
            }
        }
    }

    /**
     * @param clientFullPath - client full path
     * @return
     */
    public static String getClientTempFileFullPath(String clientFullPath) {
        if (isDir(clientFullPath)) return clientFullPath;
        return clientFullPath + Config.tempFilePostfix();
    }

    public static long getTmpFileLength(String filePath){
        String clientFullPathWithCheck = BFileUtil.getClientFullPathWithCheck(filePath);
        filePath = BFileUtil.getClientTempFileFullPath(clientFullPathWithCheck);
        try {
            return new File(filePath).length();
        }catch (Exception e){}
        return 0L;
    }

    public static String getTmpCacheFileFullPath(String filePath){
        if (isDir(filePath)) return null;
        StringBuffer sbf = new StringBuffer(filePath);
        int i = filePath.lastIndexOf(File.separator);
        return sbf.insert(i+1,".tmp_cache_").toString();
    }

    public static void main(String[] args){

        String filePath = "/home/laiyz/下载/mysql.tar";
        System.out.println(getTmpCacheFileFullPath(filePath));


    }


    /**
     * @param serverRelativeFile
     * @return
     */
    public static String getPartFileFromRelative(String serverRelativeFile) {
        String clientFullPath = BFileUtil.getClientFullPathWithCheck(serverRelativeFile);
        String tempFile = BFileUtil.getClientTempFileFullPath(clientFullPath);
        return tempFile;
    }

    /**
     * @param serverFileFullPath
     * @return
     */
    public static String getPartFileFromFull(String serverFileFullPath) {
        String serverRelativeFile = getServerRelativePath(serverFileFullPath);
        String clientFullPath = BFileUtil.getClientFullPathWithCheck(serverRelativeFile);
        String tempFile = BFileUtil.getClientTempFileFullPath(clientFullPath);
        return tempFile;
    }


    public static boolean isDir(String filepath) {
        return Files.isDirectory(Paths.get(filepath));
    }

    /**
     * rename temp file to client file
     *
     * @param tmpFile
     * @param clipath
     */
    public static void renameCliTempFile(File tmpFile, String clipath) {
        tmpFile.renameTo(new File(clipath));
    }

    /**
     * build FileRsp
     *
     * @param serverpath
     * @param filesize
     * @param checksum
     * @param reqTs
     * @return
     */
    public static ByteBuf buildRspFile(String serverpath, long filesize, String checksum, long reqTs) {
        return buildRsp(BFileCmd.RSP_FILE, serverpath, filesize, checksum, null, null, reqTs);
    }

    public static ByteBuf buildRspDir(String serverpath, long reqTs) {
        return buildRsp(BFileCmd.RSP_DIR, serverpath, 0, ConstUtil.EMPTY_STR, null, null, reqTs);
    }

    /**
     * build ListRsp
     *
     * @param serverpath
     * @param rspData
     * @param reqTs
     * @return
     */
    public static ByteBuf buildRspList(String serverpath, String rspData, long reqTs) {
        // checksum: rspData 's md5 hash
        String checksum = MD5.asHex(BByteUtil.toBytes(rspData));
        return buildRsp(BFileCmd.RSP_LIST, serverpath, 0, checksum, rspData, null, reqTs);
    }


    /**
     * BFileRsp intro:
     * it contains 2 field for store client send data(rspData/chunkData),
     * usage:
     * rspData - string, can be json string or only some string words
     * fileChunkData - byte[], byte array data
     * <p>
     * <p>
     * Standard Rsp format like:
     * +--------------------------------------------------------------------+
     * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info | delimiter |
     * +--------------------------------------------------------------------+
     * <p>
     * <p>
     * Non-standard format(append byte[] data after Rsp object, because cannot retrieve the data and set to Rsp.chunkData:
     * e.g. FileRsp(cmd: CMD_REQ) data format(only for FileRegion which directly write file data to channel):
     * +---------------------------------------------------------------------------------+
     * | bfile_info_prefix | bfile_info_bytes(int) | bfile_info | chunk_data | delimiter |
     * +---------------------------------------------------------------------------------+
     * <p>
     * bfile_info_prefix:
     * <p>
     * Note: delimiter not encode in upper format, need append delimiter after
     * this method invoked, see "bytes size format" in method body
     *
     * @param cmd
     * @param filePath
     * @param fileSize
     * @param checksum
     * @param rspData
     * @param chunkData
     * @param reqTs
     * @return
     */
    private static ByteBuf buildRsp(String cmd, String filePath, long fileSize, String checksum, String rspData, byte[] chunkData, long reqTs) {
        // BFile info prefix
        byte[] prefix = BByteUtil.toBytes(ConstUtil.bfile_info_prefix);
        // BFile info

        BFileMsg.BFileRsp rsp = BFileMsg.BFileRsp.newBuilder()
                .setId(getReqId(filePath)) // rspId = reqId
                .setCmd(cmd) //BFileCmd.CMD_RSP)
                .setFilepath(filePath) // relative path(not contains client.dir or server.dir)
                .setFileSize(fileSize)
                .setChecksum(checksum) // file fingerprint/ md5(server_dir_abs_path)
                .setRspData(StringUtils.trimToEmpty(rspData))
                .setChunkData(chunkData == null ? ByteString.EMPTY : ByteString.copyFrom(chunkData))
                .setReqTs(reqTs)
                .setRspTs(System.currentTimeMillis())
                .build();
        byte[] rspWithoutFileData = rsp.toByteArray();

        int bfileInfoLen = rspWithoutFileData.length;
        byte[] bifileInfoBytes = BByteUtil.toBytes(bfileInfoLen);

        /**
         * bytes size format:
         * +----------------------------------------------------------------+
         * | bfile_info_prefix_len | bfile_info_bytes(int) | bfile_info_len |
         * +----------------------------------------------------------------+
         *
         */
        // assemble data
        int cpos = 0;
        byte[] data = new byte[prefix.length + Integer.BYTES + rspWithoutFileData.length];
        // bfile_info_prefix
        System.arraycopy(prefix, 0, data, cpos, prefix.length);
        // bfile_info size
        cpos += prefix.length;
        System.arraycopy(bifileInfoBytes, 0, data, cpos, Integer.BYTES);
        // bfile_info
        cpos += Integer.BYTES;
        System.arraycopy(rspWithoutFileData, 0, data, cpos, rspWithoutFileData.length);

        log.debug("data.len: {}, bytes: {}", data.length, data.toString());
        ByteBuf bbuf = Unpooled.directBuffer(data.length);
        bbuf.writeBytes(data);

        return bbuf;
    }


    /**
     * @param cmd
     * @param filepath - file path relative server.dir
     * @return
     */
    public static BFileMsg.BFileReq buildReq(String cmd, String filepath) {
        BFileMsg.BFileReq req = BFileMsg.BFileReq.newBuilder()
                .setId(getReqId(filepath))
                .setCmd(cmd)
                .setFilepath(filepath)
                .setTs(System.currentTimeMillis())
                .build();

        return req;
    }

    public static SenderMsg.Req buildSenderReq(String cmd, String filepath) {
        SenderMsg.Req req = SenderMsg.Req.newBuilder()
                .setId(getReqId(filepath))
                .setCmd(cmd)
                .setFilepath(filepath)
                .setTs(System.currentTimeMillis())
                .build();

        return req;
    }

    /**
     * @param filepath - file path relative server.dir
     */
    public static String getReqId(String filepath) {
        return MD5.asHex(BByteUtil.toBytes(filepath));
    }


}
