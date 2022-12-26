package com.laiyz.client;

public class ClientStarter {
    public static void main(String[] args) {
        String filePath = "/home/laiyz/下载/mysql.tar";
//        String filePath = "/Users/laiyz/Downloads/Moana.2016.R6.1080p.WEB-DL.x264.AAC-ADPHD.mkv/Moana.2016.R6.1080p.WEB-DL.x264.AAC-ADPHD.mkv";
        FileClient client = new FileClient();
        client.startup(filePath);
    }
}
