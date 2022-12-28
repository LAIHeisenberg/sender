package com.laiyz.client.base;

import com.laiyz.client.cmd.*;
import com.laiyz.comm.BFileCmd;

import java.util.HashMap;
import java.util.Map;

public class ClientCmdRegister {

    private static final Map<String, CmdHandler> cmdHandlerMap = new HashMap<>();

    public static void init() {
        register(BFileCmd.RSP_FILE, new FileRspHandler());
        register(BFileCmd.RSP_PULL, new FilePullHandler());

    }

    public static void register(String cmd, CmdHandler cmdHandler) {
        cmdHandlerMap.put(cmd, cmdHandler);
    }

    public static CmdHandler getHandler(String cmd) {
        return cmdHandlerMap.get(cmd);
    }

}
