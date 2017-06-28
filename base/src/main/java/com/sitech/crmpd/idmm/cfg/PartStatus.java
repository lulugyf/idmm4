package com.sitech.crmpd.idmm.cfg;

import java.util.HashMap;
import java.util.Map;

/**
 * 分区状态
 */
public enum PartStatus {
    READY(1),
    JOIN(2),
    LEAVE(3),
    SHUT(4),
    UNDEFINE(-1)
    ;

    private int code;
    private static final Map<Integer, PartStatus> intToTypeMap
            = new HashMap<>();

    static {
        for (PartStatus type : PartStatus.values()) {
            intToTypeMap.put(type.code, type);
        }
    }
    public static PartStatus valueOf(int i) {
        PartStatus type = intToTypeMap.get(i);
        if (type == null)
            return UNDEFINE;
        return type;
    }

    private PartStatus(int i) {
        code = i;
    }
    public int code() {
        return code;
    }
}
