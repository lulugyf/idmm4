package com.sitech.bcc.des;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Date;

/*
 * Created by baimin on 2017/5/2 0002.
 *
 * @Description:DES CBC模式加解密
 * @Project_Name:
 * @Modified by:  on date:0.
 * @Company:Si-tech
 */
public class EncryptionAndDecryption {

    static String strDefaultKey = "AmCckeyvaL09";
    static byte[] iv1 = {0, 0, 0, 0, 0,  0, 0, 0};
    static Cipher encryptCipher = null;
    static Cipher decryptCipher = null;
    static final int DES_PADDING_SIZE = 8;

    /*
     * @Author:baimin
     * @Description:生成密钥
     * @param:
     * @Date:
     */
    private static SecretKey keyGenerator(String keyStr) throws Exception {
        OpenSSL openssl = new OpenSSL();
        byte input[] = openssl.des_string_to_key(keyStr);
        DESKeySpec desKey = new DESKeySpec(input);
        // 创建一个密匙工厂，然后用它把DESKeySpec转换成密钥
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey securekey = keyFactory.generateSecret(desKey);
        return securekey;
    }

    /*
     * @Author:baimin
     * @Description:静态代码块：1、生成iv向量；2、初始化Cipher对象
     * @param:
     * @Date:2017/5/4 0004
     */
    static {

        try {

            Key deskey = keyGenerator(strDefaultKey);
            IvParameterSpec iv = new IvParameterSpec(iv1);

            encryptCipher = Cipher.getInstance("DES/CBC/NoPadding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, deskey, iv);// 初始化Cipher对象，设置为加密模式
            decryptCipher = Cipher.getInstance("DES/CBC/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, deskey, iv);// 初始化Cipher对象，设置为解密模式

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * @Author:baimin
     * @Description:Bcd转Asc
     * @param:
     * @Date:2017/5/2 0002
     */
    public static String BcdToAsc(int[] src, int srcLen) throws UnsupportedEncodingException {
        char[] result = new char[srcLen * 2];
        for (int i = 0; i < srcLen; i++) {
            result[i * 2] = (char) ('A' + (src[i] >> 4));
            result[i * 2 + 1] = (char) ('A' + (src[i] & 0x0F));
        }
        return String.valueOf(result);
    }

    /*
     * @Author:baimin
     * @Description:Asc转Bcd
     * @param:
     * @Date:2017/5/3 0003
     */
    public static byte[] AscToBcd(char[] src, int srcLen) {

        byte[] result = new byte[srcLen / 2];
        for (int i = 0; i < srcLen; i+=2) {
            result[i / 2] = (byte) (((src[i] - 'A') << 4) + (src[i + 1] - 'A'));
        }
        return result;
    }

    /*
     * @Author:baimin
     * @Description:字符串加密入口
     * @param:
     * @Date:2017/5/2 0002
     */
    public static String strEncrypt(String str) throws Exception {

        String ver = "01";
        String result = "";
        if ("".equals(str)) {
            result = ver;
        }else {

            //调用加密方法
            String p = encrypt(str, str.getBytes("GBK").length);
            result = ver + p;
        }

        return result;
    }

    /*
     * @Author:baimin
     * @Description:DES加密
     * @param:
     * @Date:2017/5/2 0002
     */
    public static String encrypt(String str, int inLen) throws Exception {

        int l = 0;
        if (inLen % 8 > 0) {
            l = inLen + (8 - inLen % 8);
        }else {
            l = inLen;
        }
        //进行数据填充
        byte[] strIn = PaddingProcessor.addPadding(str.getBytes("GBK"), DES_PADDING_SIZE);

        //进行加密操作
        byte[] result = encryptCipher.doFinal(strIn);

        //对加密产生的负数结果进行正数转换
        int[] temp = new int[result.length];
        for (int i = 0; i < result.length; i++) {
            temp[i] = result[i] & 0x000000FF;
        }
        //将bcd转为ascii
        return BcdToAsc(temp, l);
    }

    /*
     * @Author:baimin
     * @Description:字符串解密入口
     * @param:
     * @Date:2017/5/3 0003
     */
    public static String strDecrypt(String str) throws Exception {

        String result = "";
        if ("".equals(str)) {
            return result;
        }else if ("".equals(str.substring(2, str.length()))) {
            return result;
        }else {
            String p = decrypt(str.substring(2, str.length()), str.length() - 2);
            result = result + p;
            return result;
        }
    }

    /*
     * @Author:baimin
     * @Description:DES解密
     * @param:
     * @Date:2017/5/3 0003
     */
    public static String decrypt(String str, int inLen) throws Exception {

        int l = 0;
        if (inLen % 16 > 0) {
            return null;
        }else {
            l = inLen;
        }

        byte[] temp = AscToBcd(str.toCharArray(), l);

        //进行解密操作
        byte[] result = decryptCipher.doFinal(temp);

        //---------------------------对得到的数据去0--------------------------------
        int k = 0;
        for (int i = 0; i < result.length; i++) {
            if (0 == result[i]) {
                k++;
            }
        }
        byte[] newResult = new byte[result.length - k];
        for (int i = 0; i < result.length; i++) {
            if (0 != result[i]) {
                newResult[i] = result[i];
            }
        }
        //---------------------------对得到的数据去0--------------------------------

        String strResult = new String(newResult, "GBK");
        return strResult;
    }

    /*
     * 客户账户密码加解密
     * @Author:wangjxc
     */

    /****************************************客户账户密码加解密*******************************************/
    private final static String[] asc_val = {"A","z","C","s","E","k","G","r","I","y",
            "K","u","M","q","O","n","Q","l","S","f",
            "U","h","W","c","Y","a","Z","b","X","d",
            "e","T","g","V","i","j","F","p","m","P",
            "o","N","R","H","D","t","L","v","w","B",
            "J","x"};

    private final static char[] chr_val = {'A','z','C','s','E','k','G','r','I','y',
            'K','u','M','q','O','n','Q','l','S','f',
            'U','h','W','c','Y','a','Z','b','X','d',
            'e','T','g','V','i','j','F','p','m','P',
            'o','N','R','H','D','t','L','v','w','B',
            'J','x'};

    //加密
    public static String en_cipher(String src){
        StringBuffer sb = new StringBuffer(6);
        long wTime = new Date().getTime();
        int val,i,m;
        val = Integer.parseInt(src);
        val= (int) ((wTime%2146)*1000000+val);
        System.out.println("The val is : "+ val);
        for(i=0;i<6;i++)
        {
            m=val%52;
            val=val/52;
            System.out.println("The val1 is : "+ val);
            sb.append(asc_val[m]);
        }

        return sb.toString();
    }

    //解密
    public static String de_cipher(String src){
        char[] c = src.toCharArray();
        int val,i,m;
        for(val=0,i=5;i>=0;i--)
        {
            for(m=0; m < 52 && chr_val[m]!=c[i]; m++);
            val=val*52+m;
            System.out.println("The val is : "+ val);
        }
        String dst = String.format("%06d", val%1000000);
        System.out.println("The dst is : "+ dst);
        return dst;
    }
    /****************************************客户账户密码加解密*******************************************/
}