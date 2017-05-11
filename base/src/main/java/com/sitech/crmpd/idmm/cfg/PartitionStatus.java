package com.sitech.crmpd.idmm.cfg;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gyf on 4/30/2017.
 */
public enum PartitionStatus {
    READY(1),
    JOINING(2),
    LEAVING(3),
    SHUT(4),
    UNDEFINE(-1)
    ;

    private int code;
    private static final Map<Integer, PartitionStatus> intToTypeMap
            = new HashMap<Integer, PartitionStatus>();
    static {
        for (PartitionStatus type : PartitionStatus.values()) {
            intToTypeMap.put(type.code, type);
        }
    }
    public static PartitionStatus fromInt(int i) {
        PartitionStatus type = intToTypeMap.get(Integer.valueOf(i));
        if (type == null)
            return PartitionStatus.UNDEFINE;
        return type;
    }

    private PartitionStatus(int i) {
        code = i;
    }
    public int code() {
        return code;
    }
//    public static PartitionStatus valueOf(int code) { return PartitionStatus.(code); }

}
