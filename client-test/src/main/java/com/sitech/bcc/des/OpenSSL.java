package com.sitech.bcc.des;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * Created by baimin on 2017/5/2 0002.
 *
 * @Description:模拟c语言实现DES算法的openSSL类
 * @Project_Name:
 * @Modified by:  on date:
 * @Company:Si-tech
 */
public class OpenSSL {

    /*
     * @Author:baimin
     * @Description:模拟c语言中des_string_to_key方法的实现
     * @param:
     * @Date:2017/5/2 0002
     */
    public static byte[] des_string_to_key(String keyString) throws Exception {
        final byte[] str = keyString.getBytes();

        byte[] key = new byte[8];
        for (int index = 0; index < str.length; index++) {
            int j = str[index] & 0xff;
            if ((index % 16) < 8)
                key[index % 8] ^= (j << 1);
            else {
                j = ((j << 4) & 0xf0) | ((j >>> 4) & 0x0f);
                j = ((j << 2) & 0xcc) | ((j >>> 2) & 0x33);
                j = ((j << 1) & 0xaa) | ((j >>> 1) & 0x55);
                key[7 - (index % 8)] ^= j;
            }
        }

        des_set_odd_parity(key);

        key = des_cbc_cksum(str, key, key);

        des_set_odd_parity(key);

        return key;
    }

    public static byte[] des_cbc_cksum(byte[] data, byte[] keyBytes, byte[] ivec) throws Exception {
        final byte[] paddedData = new byte[((data.length + 7) / 8) * 8];
        for (int index = 0; index < data.length; index++) {
            paddedData[index] = data[index];
        }

        final SecretKey skey = new SecretKeySpec(keyBytes, "DES");
        final IvParameterSpec iv = new IvParameterSpec((byte[]) ivec.clone());
        Cipher cipher_ = Cipher.getInstance("DES/CBC/NoPadding");
        cipher_.init(Cipher.ENCRYPT_MODE, skey, iv);
        final byte[] rkey = cipher_.doFinal(paddedData);

        final byte[] checksum = new byte[8];
        for (int index = 0, j = rkey.length - 8; index < 8; index++, j++) {
            checksum[index] = rkey[j];
        }

        return checksum;
    }

    public static void des_set_odd_parity(byte[] data) {
        for (int i = 0; i < 8; i++)
            data[i] = (byte) odd_parity[data[i] & 0xff];

    }

    private static final int[] odd_parity = {

            1, 1, 2, 2, 4, 4, 7, 7, 8, 8, 11, 11, 13, 13, 14, 14,

            16, 16, 19, 19, 21, 21, 22, 22, 25, 25, 26, 26, 28, 28, 31, 31,

            32, 32, 35, 35, 37, 37, 38, 38, 41, 41, 42, 42, 44, 44, 47, 47,

            49, 49, 50, 50, 52, 52, 55, 55, 56, 56, 59, 59, 61, 61, 62, 62,

            64, 64, 67, 67, 69, 69, 70, 70, 73, 73, 74, 74, 76, 76, 79, 79,

            81, 81, 82, 82, 84, 84, 87, 87, 88, 88, 91, 91, 93, 93, 94, 94,

            97, 97, 98, 98, 100, 100, 103, 103, 104, 104, 107, 107, 109, 109, 110, 110,

            112, 112, 115, 115, 117, 117, 118, 118, 121, 121, 122, 122, 124, 124, 127, 127,

            128, 128, 131, 131, 133, 133, 134, 134, 137, 137, 138, 138, 140, 140, 143, 143,

            145, 145, 146, 146, 148, 148, 151, 151, 152, 152, 155, 155, 157, 157, 158, 158,

            161, 161, 162, 162, 164, 164, 167, 167, 168, 168, 171, 171, 173, 173, 174, 174,

            176, 176, 179, 179, 181, 181, 182, 182, 185, 185, 186, 186, 188, 188, 191, 191,

            193, 193, 194, 194, 196, 196, 199, 199, 200, 200, 203, 203, 205, 205, 206, 206,

            208, 208, 211, 211, 213, 213, 214, 214, 217, 217, 218, 218, 220, 220, 223, 223,

            224, 224, 227, 227, 229, 229, 230, 230, 233, 233, 234, 234, 236, 236, 239, 239,

            241, 241, 242, 242, 244, 244, 247, 247, 248, 248, 251, 251, 253, 253, 254, 254

    };

}
