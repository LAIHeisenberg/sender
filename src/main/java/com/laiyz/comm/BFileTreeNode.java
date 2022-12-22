package com.laiyz.comm;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BFileTreeNode {

    @JSONField(ordinal = 1)
    private String absolutePath;

    @JSONField(ordinal = 2)
    private String name;

    // D - directory, F-file
    @JSONField(ordinal = 3)
    private String fileCat;

    private List<BFileTreeNode> children = new ArrayList<>();

    public BFileTreeNode(String absolutePath, String name, String fileCat) {
        this.absolutePath = absolutePath;
        this.name = name;
        this.fileCat = fileCat;
    }
}

