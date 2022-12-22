package com.laiyz.comm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Objects;

@Data
public class BFileInfo {
    //
    @JSONField(ordinal = 1)
    private String filepath;

    // D - directory, F-file
    @JSONField(ordinal = 2)
    private String fileCat;

    // file fingerprint
    @JSONField(ordinal = 3)
    private String checksum;
    // bytes
    @JSONField(ordinal = 4)
    private long fileSize;

    public BFileInfo() {
    }

    public BFileInfo(String filepath, String fileCat, String checksum, long fileSize) {
        this.filepath = filepath;
        this.fileCat = fileCat;
        this.checksum = checksum;
        this.fileSize = fileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BFileInfo)) return false;
        BFileInfo fileInfo = (BFileInfo) o;
        return fileSize == fileInfo.fileSize &&
                filepath.equals(fileInfo.filepath) &&
                fileCat.equals(fileInfo.fileCat) &&
                checksum.equals(fileInfo.checksum);
    }

    @Override
    public int hashCode() {
        return 17 + Objects.hash(filepath, fileCat, checksum, fileSize);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
