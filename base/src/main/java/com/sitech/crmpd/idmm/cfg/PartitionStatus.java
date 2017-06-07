package com.sitech.crmpd.idmm.cfg;

import java.util.HashMap;
import java.util.Map;

/**
 * 分区状态
 */
public enum PartitionStatus {
    READY(1),
    JOIN(2),
    LEAVE(3),
    SHUT(4),
    UNDEFINE(-1)
    ;

    private int code;
    private static final Map<Integer, PartitionStatus> intToTypeMap
            = new HashMap<>();

    static {
        for (PartitionStatus type : PartitionStatus.values()) {
            intToTypeMap.put(type.code, type);
        }
    }
    public static PartitionStatus valueOf(int i) {
        PartitionStatus type = intToTypeMap.get(i);
        if (type == null)
            return UNDEFINE;
        return type;
    }

    private PartitionStatus(int i) {
        code = i;
    }
    public int code() {
        return code;
    }
}
