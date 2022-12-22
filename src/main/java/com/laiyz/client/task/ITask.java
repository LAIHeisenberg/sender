package com.laiyz.client.task;

import com.laiyz.comm.StatusEnum;

public interface ITask {
    public StatusEnum appendFileData(byte[] fileData);
}
