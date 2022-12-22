package com.laiyz.comm;

public interface BFileCmd {

    /** client ack server's response **/
    public static final int RSP_ACK = 3;

    // server register request command
    /** client send file download request */
    public static final String REQ_FILE = "REQ_FILE";
    public static final String REQ_LIST = "REQ_LIST";
    public static final String REQ_UPLOAD = "REQ_UPLOAD";

    // client register rsp commands
    /** server response file bytes(with BFileRsp header info) */
    public static final String RSP_FILE = "RSP_FILE";
    public static final String RSP_DIR = "RSP_DIR";
    public static final String RSP_LIST = "RSP_LIST";
}
