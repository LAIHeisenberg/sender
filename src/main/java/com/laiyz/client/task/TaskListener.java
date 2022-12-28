package com.laiyz.client.task;

import com.laiyz.proto.SenderMsg;

public interface TaskListener {
    public void onCompleted(SenderMsg.Rsp rsp);
}
