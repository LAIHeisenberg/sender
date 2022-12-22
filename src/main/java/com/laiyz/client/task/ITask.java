package com.laiyz.client.task;

import com.laiyz.comm.StatusEnum;
import com.laiyz.proto.BFileMsg;

public interface ITask {
    public StatusEnum appendFileData(byte[] fileData, BFileMsg.BFileRsp rsp);
}
