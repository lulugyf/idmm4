package com.sitech.crmpd.idmm.client.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Encryptor {
    public static String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
//            System.out.println("encrypted string: "                 + Base64.encodeBase64String(encrypted));
            System.out.println("length of output:"+encrypted.length);
            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }


    public static String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    static final IvParameterSpec iv = new IvParameterSpec("vandomxnitVecto1".getBytes());
    static final SecretKeySpec skeySpec = new SecretKeySpec("xar12j45uab1234c".getBytes(), "AES");

    public final static byte[] encrypt(byte[] in) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            return cipher.doFinal(in);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public final static byte[] decrypt(byte[] in) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            return cipher.doFinal(in);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    // https://github.com/kokke/tiny-AES128-C
    public static void main(String[] args) {
        String key = "Bar12345Bar12345"; // 128 bit key
        String initVector = "RandomInitVector"; // 16 bytes IV

        String en = encrypt(key, initVector, "Hello Worl   12345678901234567890123456789011111111116");
        System.out.println("en "+en);

        System.out.println(decrypt(key, initVector, en
                ));

//        String oo = "YjIxKpU0oPBiPWsTO4awMQ==";
//        System.out.println(decrypt(key, initVector, oo ));

    }
}