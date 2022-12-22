package com.laiyz.client;

public class ClientStarter {
    public static void main(String[] args) {
//        String filePath = "/home/laiyz/下载/mysql.tar";
        String filePath = "/home/laiyz/桌面/556.jpg";
        FileClient client = new FileClient();
        client.startup(filePath);
    }
}
