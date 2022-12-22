package com.laiyz.comm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BFileCombo {
    private List<BFileInfo> infoList;
    private BFileTreeNode treeNode;
}
