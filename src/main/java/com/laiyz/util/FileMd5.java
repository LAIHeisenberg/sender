package com.laiyz.util;

import java.io.File;

public class FileMd5 {

    public static void main(String[] args){

        String am_pdf = "/home/laiyz/桌面/test_pdf/am.pdf";
        String test_pdf = "/home/laiyz/桌面/test_pdf/test.pdf";

        String checksum1 = BFileUtil.checksum(new File(am_pdf));
        String checksum2 = BFileUtil.checksum(new File(test_pdf));

        System.out.println(checksum1);
        System.out.println(checksum2);

    }

}
