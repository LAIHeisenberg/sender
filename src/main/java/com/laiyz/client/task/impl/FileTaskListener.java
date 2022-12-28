package com.laiyz.client.task.impl;

import com.laiyz.client.base.ClientCache;
import com.laiyz.client.task.TaskListener;
import com.laiyz.proto.BFileMsg;
import com.laiyz.proto.SenderMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FileTaskListener implements TaskListener {

    /**
     * reset recvFileKey to null,
     * clean ClientCache RspInfo with the recvFileKey
     * clean running task with the recvFileKey
     *
     * @param rsp - file response info(BFileRsp)
     */
    @Override
    public void onCompleted(SenderMsg.Rsp rsp) {
        String recvFileKey = ClientCache.currRecvFileKey();
        if (StringUtils.isNotBlank(recvFileKey)) {
            ClientCache.removeRspInfo(recvFileKey);
        }
        ClientCache.resetRecvFileKey();
        ClientCache.removeTask(rsp.getId());
    }
}
