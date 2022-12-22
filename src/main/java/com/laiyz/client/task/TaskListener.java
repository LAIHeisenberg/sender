package com.laiyz.client.task;

import com.laiyz.proto.BFileMsg;

public interface TaskListener {
    public void onCompleted(BFileMsg.BFileRsp rsp);
}
