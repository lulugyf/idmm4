package com.sitech.bcc.des;

/*
 * Created by baimin on 2017/5/2 0002.
 *
 * @Description:实现用0填充不足位的类
 * @Project_Name:
 * @Modified by:  on date:
 * @Company:Si-tech
 */
public class PaddingProcessor {

    private final static int BLOCK_SIZE = 8;

    /*
     * @Author:baimin
     * @Description：由于c语言版本的加解密对原始数据进行了用0填充，所以本方法实现对不足八位的数据进行用0填充
     * @param:
     * @Date:2017/5/2 0002
     */
    public static byte[] addPadding(byte[] in, int blockSize) {
        int length = in.length;
        if (length % blockSize == 0) {
            return in;
        } else {
            byte[] out;
            int blockCount = length / blockSize;
            out = new byte[(blockCount+1) * blockSize];
            System.arraycopy(in, 0, out, 0, length);
            return out;
        }

    }

    /*
     * @Author:baimin
     * @Description:对数据进行去0操作，可以不进行使用。
     * @param:
     * @Date:2017/5/4 0004
     */
    public static byte[] removePadding(byte[] in, int blockSize) {
        int outLen = in.length;
        for (int i = BLOCK_SIZE + 1; i > 0&&outLen>0; i--) {

            if (in[--outLen] == (byte) 0x00) {
                break;
            }
        }
        byte[] out = new byte[outLen];
        System.arraycopy(in, 0, out, 0, outLen);
        return out;
    }
}
