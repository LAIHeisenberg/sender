package com.laiyz.client;

public class ClientStarter {
    public static void main(String[] args) {
//        String filePath = "/Users/laiyz/Downloads/黄金三镖客BD中英双字.电影天堂.www.dy2018.com.mp4";
//        String filePath = "/home/laiyz/projects/sender/target";
//        String filePath = "/Users/laiyz/Workspace/git_repo/sender/target/sender-1.0-SNAPSHOT.jar";
//        FileClient client = new FileClient();
//        client.startup(filePath);

        FilePullClient filePullClient = new FilePullClient();
        filePullClient.startup("/root/深入理解高并发编程（第1版）.pdf", "/home/laiyz/ddump.sql");
    }
}
