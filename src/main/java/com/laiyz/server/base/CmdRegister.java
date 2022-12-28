package com.laiyz.server.base;

import com.laiyz.comm.BFileCmd;
import com.laiyz.server.cmd.CmdHandler;
import com.laiyz.server.cmd.FileReqHandler;

import java.util.HashMap;
import java.util.Map;

public class CmdRegister {

    private static final Map<String, CmdHandler> cmdHandlerMap = new HashMap<>();

    public static void init() {
        register(BFileCmd.REQ_FILE, new FileReqHandler());

    }

    public static void register(String cmd, CmdHandler cmdHandler) {
        cmdHandlerMap.put(cmd, cmdHandler);
    }

    public static CmdHandler getHandler(String cmd) {
        return cmdHandlerMap.get(cmd);
    }

}
