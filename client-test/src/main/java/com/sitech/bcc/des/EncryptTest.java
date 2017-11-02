package com.sitech.bcc.des;

/*
 * Created by baimin on 2017/5/4 0004.
 *
 * @Description:加解密测试类
 * @Project_Name:
 * @Modified by:  on date:
 * @Company:Si-tech
 */
public class EncryptTest {

    public static void main(String[] args) throws Exception {

        String str = "测试-*+#@";
        int k = 0;
        long startTime=System.currentTimeMillis();   //获取开始时间
        for (int i = 0; i < 3000000; i++) {
            String encryptStr = EncryptionAndDecryption.strEncrypt(str);
            System.out.println("加密结果**********：" + encryptStr);
            String decryptStr = EncryptionAndDecryption.strDecrypt(encryptStr);
            System.out.println("解密结果**********：" + decryptStr);
            k++;
        }
        System.out.println(k);
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
    }
}
