package com.sitech.crmpd.idmm.util.ch;

/**
 * Created by guanyf on 4/24/2017.
 */
public class StrHashFunction implements HashFunction{

    @Override
    public final int hash(Object key) {
        return hashmurmur(key);
    }

    private final int hash1(Object key) {
        final String s = key.toString();
        final int strlen = s.length();
        int hash = 7;
        for (int i = strlen-1; i >=0; i--) {
//        for (int i = 0; i < strlen; i++) {
            hash = hash*31 + s.charAt(i);
        }
        return hash;
    }

    private final int hashmurmur(Object key) {
        return MurmurHash.hash32(key.toString());
    }
}
