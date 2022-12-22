package com.laiyz.client.base;

import com.laiyz.client.cmd.CmdHandler;
import com.laiyz.client.cmd.DirRspHandler;
import com.laiyz.client.cmd.FileRspHandler;
import com.laiyz.client.cmd.ListRspHandler;
import com.laiyz.comm.BFileCmd;

import java.util.HashMap;
import java.util.Map;

public class ClientCmdRegister {

    private static final Map<String, CmdHandler> cmdHandlerMap = new HashMap<>();

    public static void init() {
        register(BFileCmd.RSP_FILE, new FileRspHandler());
        register(BFileCmd.RSP_DIR, new DirRspHandler());
        register(BFileCmd.RSP_LIST, new ListRspHandler());

    }

    public static void register(String cmd, CmdHandler cmdHandler) {
        cmdHandlerMap.put(cmd, cmdHandler);
    }

    public static CmdHandler getHandler(String cmd) {
        return cmdHandlerMap.get(cmd);
    }

}
