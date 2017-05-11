package com.sitech.crmpd.idmm.ble.mem;


import java.util.List;

/**
 * Created by guanyf on 2016/4/26.
 */
public interface LoadCallback {
    public void finishLoading(List<MsgIndex> arr);
}
