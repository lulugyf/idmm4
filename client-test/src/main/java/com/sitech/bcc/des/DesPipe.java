package com.sitech.bcc.des;

import java.io.IOException;
import java.io.InputStream;

public class DesPipe {
    public static String des(String src, String mode, String key, String type) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(new String[]{"e:/tmp/1/d.exe", src, mode, key, type});
            p.waitFor();
            InputStream pin = p.getInputStream();
            byte buf[] = new byte[pin.available()];
            pin.read(buf);
            pin.close();
            p.destroy();
            return new String(buf).trim();
        }catch(IOException ex){
            ex.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String out = des("1234567890", "0", "NETTV2015", "DES_PKCS5PADDING");
        System.out.println(out);
        out = des("915772B99F4127D54422BA2308667D94", "1", "NETTV2015", "DES_PKCS5PADDING");
        System.out.println(out);
    }
}
