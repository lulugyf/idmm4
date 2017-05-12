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
            = new HashMap<>();
//    private static final Map<String, PartitionStatus> strToTypeMap
//            = new HashMap<>();
    static {
        for (PartitionStatus type : PartitionStatus.values()) {
            intToTypeMap.put(type.code, type);
//            strToTypeMap.put(type.name(), type);
        }
    }
    public static PartitionStatus valueOf(int i) {
        PartitionStatus type = intToTypeMap.get(i);
        if (type == null)
            return UNDEFINE;
        return type;
    }
//    public static PartitionStatus valueOf(String name){
//        PartitionStatus t = strToTypeMap.get(name);
//        if(t == null)
//            return UNDEFINE;
//        else
//            return t;
//    }

    private PartitionStatus(int i) {
        code = i;
    }
    public int code() {
        return code;
    }
//    public static PartitionStatus valueOf(int code) { return PartitionStatus.(code); }

}
