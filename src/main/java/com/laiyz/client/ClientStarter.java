package com.laiyz.client;

public class ClientStarter {
    public static void main(String[] args) {
//        String filePath = "/Users/laiyz/Downloads/黄金三镖客BD中英双字.电影天堂.www.dy2018.com.mp4";
        String filePath = "/home/laiyz/projects/sender/target";
//        String filePath = "/Users/laiyz/Downloads/Moana.2016.R6.1080p.WEB-DL.x264.AAC-ADPHD.mkv/Moana.2016.R6.1080p.WEB-DL.x264.AAC-ADPHD.mkv";
//        FileClient client = new FileClient();
//        client.startup(filePath);

        FilePullClient filePullClient = new FilePullClient();
        filePullClient.startup("/mnt/www/video/snb.avi", "/home/laiyz/ddump.sql");
    }
}
