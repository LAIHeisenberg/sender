package com.laiyz.client.base;


import com.laiyz.client.task.impl.FileTask;
import com.laiyz.comm.BFileCombo;
import com.laiyz.comm.BFileInfo;
import com.laiyz.proto.BFileMsg;
import com.laiyz.util.BFileUtil;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;

/**
 * BFileRsp.id = BFileReq.id = reqId = md5(filepath)
 * filepath is relative to server.dir
 */
public class ClientCache {

    /** client received server filepath list, file data will transferred one by one
     *  key: BFileReq.id=reqId
     **/
    private static Map<String, BFileInfo> serverFiles = new HashMap<>();

    private static BFileCombo combo = null;
    /** current running Tasks, key: BFileReq.id=reqId */
    private static Map<String, FileTask> runningTasks = new HashedMap();

    // ------------- only for ChunkedFile mode start ----------------
    /** ChunkedFile mode -> key: key: BFileReq.id=reqId, value: the BFileRsp info */
    private static Map<String, BFileMsg.BFileRsp> rspInfoMap = new HashMap<>();

    /** ChunkedFile mode, recvFileKey = (BFileReq.id=reqId)) */
    private static volatile String recvFileKey = null;


    public static String currRecvFileKey() {
        return recvFileKey;
    }

    public static void resetRecvFileKey() {
        recvFileKey = null;
    }

    /**
     * get transferred file BFileRsp info
     *
     * @param key
     * @return
     */
    public static BFileMsg.BFileRsp getRspInfo(String key) {
        return rspInfoMap.get(key);
    }

    public static void addRspInfo(String key, BFileMsg.BFileRsp rsp) {
        recvFileKey = key; // update recvFileKey
        rspInfoMap.put(key, rsp);
    }

    public static void removeRspInfo(String key) {
        recvFileKey = null;
        rspInfoMap.remove(key);
    }

    // ------------- only for ChunkedFile mode end ----------------

    /**
     * get next transferred filepath and update recvFileKey
     *
     * @return next transferred filepath
     */
    public static BFileInfo nextFile() {
        Iterator<String> it = serverFiles.keySet().iterator();
        if (it.hasNext()) {
            String key = it.next();
            BFileInfo fileInfo = serverFiles.get(key);
            serverFiles.remove(key);
            return fileInfo;
        }
        return null;
    }

    /**
     * initial transferring file list
     * <k, v> - <md5(filepath), BFileInfo>
     *
     *     filepath - relative to server.dir
     * @param fileList
     */
    public static void init(BFileCombo combo, List<BFileInfo> fileList) {
        serverFiles.clear();
        combo = combo;
        for (BFileInfo file : fileList) {
            serverFiles.put(BFileUtil.getReqId(file.getFilepath()), file);
        }
    }

    /**
     *
     * @param id - md5(server_relative_path)
     * @return
     */
    public static FileTask getTask(String id) {
        return runningTasks.get(id);
    }

    /**
     *
     * @param id - md5(server_relative_path)
     * @param task - File Receive & Storage path
     */
    public static void addTask(String id, FileTask task) {
        runningTasks.put(id, task);
    }

    public static void removeTask(String id) {
        runningTasks.remove(id);
    }

    public static BFileCombo getCombo() {
        return combo;
    }

    public static void cleanAll() {
        recvFileKey = null;
        rspInfoMap = new HashMap<>();
        serverFiles = new HashMap<>();
        runningTasks = new HashMap<>();
    }


}
