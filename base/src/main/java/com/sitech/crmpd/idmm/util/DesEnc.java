package com.sitech.crmpd.idmm.util;

/**
 * Created by guanyf on 5/12/2017.
 */
public class DesEnc {
    private static final String charset = "GBK";
    private static final String key = "qwertyuiopasdfghjklzxcvbnm";

    static final char permute1[] = {
        217,120,249,196, 25,221,181,237, 40,233,253,121, 74,160,216,157,
                198,126, 55,131, 43,118, 83,142, 98, 76,100,136, 68,139,251,162,
                23,154, 89,245,135,179, 79, 19, 97, 69,109,141,  9,129,125, 50,
                189,143, 64,235,134,183,123, 11,240,149, 33, 34, 92,107, 78,130,
                84,214,101,147,206, 96,178, 28,115, 86,192, 20,167,140,241,220,
                18,117,202, 31, 59,190,228,209, 66, 61,212, 48,163, 60,182, 38,
                111,191, 14,218, 70,105,  7, 87, 39,242, 29,155,188,148, 67,  3,
                248, 17,199,246,144,239, 62,231,  6,195,213, 47,200,102, 30,215,
                8,232,234,222,128, 82,238,247,132,170,114,172, 53, 77,106, 42,
                150, 26,210,113, 90, 21, 73,116, 75,159,208, 94,  4, 24,164,236,
                194,224, 65,110, 15, 81,203,204, 36,145,175, 80,161,244,112, 57,
                153,124, 58,133, 35,184,180,122,252,  2, 54, 91, 37, 85,151, 49,
                45, 93,250,152,227,138,146,174,  5,223, 41, 16,103,108,186,201,
                211,  0,230,207,225,158,168, 44, 99, 22,  1, 63, 88,226,137,169,
                13, 56, 52, 27,171, 51,255,176,187, 72, 12, 95,185,177,205, 46,
                197,243,219, 71,229,165,156,119, 10,166, 32,104,254,127,193,173
    };
    private static byte permute[] = new byte[256];
    static {
        for(int i=0; i<permute.length; i++){
            permute[i] = (byte)(permute1[i] & 0xff);
        }
    }

    public static int toword(byte [] bt )
    {
        int ch1 = bt[1] & 0xff;
        int ch2 = bt[0] & 0xff;

        return (ch1 << 8) + (ch2 << 0);
    }

    public static int[]  rc2_keyschedule(String key) throws Exception
    {

        byte keyBytes []  = key.getBytes(charset);
        int len = keyBytes.length;
        int bits = 1024;
        int i = 0 ;

        if(!(len > 0 && len <= 128))
        {
            return null;
        }

        byte xkey[] =  new byte [128];

        System.arraycopy(keyBytes,0,xkey,0,len);


        char x ;

        if (len < 128) {
            i = 0;
            x = (char)xkey[len-1];
            do {
                x = (char)permute[ ((int)x + xkey[i++])&255 ];
                xkey[len++] = (byte)x;

            } while (len < 128);
        }


        len = (bits+7) >>> 3;
        i = 128-len;

        x = (char)permute[xkey[i] & (255 >>> (7 & -bits))];
        xkey[i] = (byte)x;

        while ((i--) > 0 ) {
            x = (char)permute[ x ^ xkey[i+len] ];
            xkey[i] = (byte)x;
        }
//        printbin(xkey);


        int ixkey[] = new int[64];
        int index = 0 ;
        for( int k = 0 ; k < xkey.length   ;k+=2 ){
            ixkey[index++] = ((xkey[k+1] & 0xff) << 8) + (xkey[k] & 0xff);
        }

        return ixkey;
    }

    static void rc2_encrypt(int xkey[],
				 byte plain[], byte cipher[] ) {
        int x76, x54, x32, x10, i;

        x76 = ((plain[7]&0xff) << 8) + (plain[6]&0xff);
        x54 = ((plain[5]&0xff) << 8) + (plain[4]&0xff);
        x32 = ((plain[3]&0xff) << 8) + (plain[2]&0xff);
        x10 = ((plain[1]&0xff) << 8) + (plain[0]&0xff);

        for (i = 0; i < 16; i++) {
            x10 += (x32 & ~x76) + (x54 & x76) + xkey[4*i+0]; x10 &= 0xffff;
            x10 = (x10 << 1) + (x10 >> 15 & 1); x10 &= 0xffff;

            x32 += (x54 & ~x10) + (x76 & x10) + xkey[4*i+1]; x32 &= 0xffff;
            x32 = (x32 << 2) + (x32 >> 14 & 3); x32 &= 0xffff;

            x54 += (x76 & ~x32) + (x10 & x32) + xkey[4*i+2]; x54 &= 0xffff;
            x54 = (x54 << 3) + (x54 >> 13 & 7); x54 &= 0xffff;

            x76 += (x10 & ~x54) + (x32 & x54) + xkey[4*i+3]; x76 &= 0xffff;
            x76 = (x76 << 5) + (x76 >> 11 & 31); x76 &= 0xffff;

            if (i == 4 || i == 10) {
                x10 += xkey[x76 & 63] & 0xffff;
                x32 += xkey[x10 & 63] & 0xffff;
                x54 += xkey[x32 & 63] & 0xffff;
                x76 += xkey[x54 & 63] & 0xffff;
            }
        }

        cipher[0] = (byte)(x10 & 0xff);
        cipher[1] = (byte)((x10 & 0xffff) >> 8);
        cipher[2] = (byte)(x32 & 0xff);
        cipher[3] = (byte)((x32 & 0xffff) >> 8);
        cipher[4] = (byte)(x54 & 0xff);
        cipher[5] = (byte)((x54 & 0xffff) >> 8);
        cipher[6] = (byte)(x76 & 0xff);
        cipher[7] = (byte)((x76 & 0xffff) >> 8);
    }

    static byte rc2_encrypt_chr(int xkey[],
					 byte plain)
    {
        byte src;
        int i;
        src = plain;

        for(i=0;i<64;i++)
        {
            src += ( (xkey[i] & 0xffff) >> 8 & 0xff);
            src ^= ( xkey[i] & 0xff );
            src = (byte)(((src << 5) & 224) + ((src >> 3) & 31));

            if((i%16) == 4 || (i%16) == 5 | (i%16) == 14)
            {
                src ^= ( ( (xkey[i] &0xffff) >> 8) & 0xff );
                src -= ( xkey[i] & 0xff);
            }
        }

        return src;
    }

    static int crypt_rc2_array(byte[] buf, String key) throws Exception
    {
        byte[] in = new byte[8];
        byte[] out = new byte[8];

        int i,j,k;
        int m;

        int[] xkey = rc2_keyschedule(key);

        j = buf.length / 8;
        for (i=0;i<j;i++)
        {
            System.arraycopy(buf, i*8, in, 0, 8);
            rc2_encrypt(xkey,in,out);
            System.arraycopy(out, 0, buf, i*8, 8);
        }

        for(m=8*j; m<buf.length; m++)
        {
            buf[m] = rc2_encrypt_chr(xkey, buf[m]);
        }
        return 0;
    }

    static String encrypt2(String srcbuf) throws Exception
    {
        int len;
        int i;

        byte[] buf = srcbuf.getBytes("GBK");

        crypt_rc2_array(buf, key);

        return toBCD(buf);
    }
    private final static char[] bcd_table = {'0', '1', '2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static String toBCD(byte[] in){
        StringBuffer sb = new StringBuffer();
        for(byte bcd: in) {
            sb.append(bcd_table[(bcd & 0xf0) >> 4]);
            sb.append(bcd_table[bcd & 0x0f]);
        }
        return sb.toString();
    }

    private static byte[] fromBCD(String s) {
        byte[] r = new byte[s.length()/2];
        char[] c = s.toCharArray();
        for(int i=0; i<r.length; i++){
            int b1 = c[i*2];
            if(b1 > '9') b1 = b1 - 'a'+10;
            else b1 -= '0';
//            System.out.printf("b1 %c %x\n", c[i*2], b1<<4);
            int b2 = c[i*2+1];
            if(b2 > '9') b2 = b2 - 'a'+10;
            else b2 -= '0';
//            System.out.printf("b2 %c %x\n", c[i*2+1], (b1<<4) | b2);
            r[i] = (byte)((b1 << 4) | b2 );
        }
        return r;
    }

    static void rc2_decrypt( int[] xkey, byte[] plain, byte[] cipher)
    {
        int x76, x54, x32, x10, i;

        x76 = ((cipher[7]&0xff) << 8) + (cipher[6]&0xff);
        x54 = ((cipher[5]&0xff) << 8) + (cipher[4]&0xff);
        x32 = ((cipher[3]&0xff) << 8) + (cipher[2]&0xff);
        x10 = ((cipher[1]&0xff) << 8) + (cipher[0]&0xff);

        i = 15;
        do
        {
            x76 &= 65535;
            x76 = (x76 << 11) + (x76 >> 5); x76 &= 0xffff;
            x76 -= (x10 & ~x54) + (x32 & x54) + xkey[4*i+3]; x76 &= 0xffff;

            x54 &= 65535;
            x54 = (x54 << 13) + (x54 >> 3); x54 &= 0xffff;
            x54 -= (x76 & ~x32) + (x10 & x32) + xkey[4*i+2];  x54 &= 0xffff;

            x32 &= 65535;
            x32 = (x32 << 14) + (x32 >> 2); x32 &= 0xffff;
            x32 -= (x54 & ~x10) + (x76 & x10) + xkey[4*i+1]; x32 &= 0xffff;

            x10 &= 65535;
            x10 = (x10 << 15) + (x10 >> 1); x10 &= 0xffff;
            x10 -= (x32 & ~x76) + (x54 & x76) + xkey[4*i+0]; x10 &= 0xffff;

            if (i == 5 || i == 11)
            {
                x76 -= xkey[x54 & 63] & 0xffff;
                x54 -= xkey[x32 & 63] & 0xffff;
                x32 -= xkey[x10 & 63] & 0xffff;
                x10 -= xkey[x76 & 63] & 0xffff;
            }
        } while (i-- > 0);

        plain[0] = (byte) (x10 & 0xff);
        plain[1] = (byte)((x10&0xffff) >> 8);
        plain[2] = (byte) (x32 & 0xff);
        plain[3] = (byte)((x32&0xffff) >> 8);
        plain[4] = (byte) (x54 & 0xff);
        plain[5] = (byte)((x54&0xffff) >> 8);
        plain[6] = (byte) (x76 & 0xff);
        plain[7] = (byte)((x76&0xffff) >> 8);

        return;
    }

    static byte rc2_decrypt_chr(int  xkey[], byte src)
    {
        int i;
        for(i=63;i>=0;i--)
        {
            if((i%16) == 4 || (i%16) == 5 | (i%16) == 14)
            {
                src += (xkey[i] & 255);
                src ^= ( xkey[i] >>> 8 & 255 );
            }

            src = (byte)((src << 3 & 248) + (src >>> 5 & 7));
            src ^= ( xkey[i] & 255 );
            src -= ( xkey[i] >> 8 & 255);
        }
        return src;
    }

    static int decrypt_rc2_array(byte[] buf, String key) throws Exception
    {
        byte[] in = new byte[8];
        byte[] out = new byte[8];
        int i,j;
        int m;
        int[] xkey = rc2_keyschedule(key);
        j = buf.length/8;
        for (i=0;i<j;i++)
        {
            System.arraycopy(buf, i*8, in, 0, 8);
            rc2_decrypt(xkey,out,in);
            System.arraycopy(out, 0, buf, i*8, 8);
        }

        for(m=j*8; m<buf.length; m++)
            buf[m] = rc2_decrypt_chr(xkey, buf[m]);

        return 0;
    }

    static String decrypt2(String s) throws  Exception{
        byte[] buf = fromBCD(s);
        decrypt_rc2_array(buf, key);
        return new String(buf, "GBK");
    }

    private static void printbin(byte s[]) {
        for(int i=0; i<s.length; i++)
            System.out.printf("%02X ", s[i]);
        System.out.println();
    }
    private static void printbin(int[] s, String tag){
        System.out.printf("[%s] ", tag);
        for(int i: s) {
            System.out.printf("%04X ", i);
        }
        System.out.println();
    }



    public static void main(String[] args) throws Exception{
//        int xkey[] = rc2_keyschedule(key) ;
//
//        byte x = (byte)0xf1;
//        byte y = (byte)0x27;
//        System.out.printf("%04x %04x\n", x<<8 + y, ((x&0xff) << 8) + (y&0xff));

//        String x = "abcd111111111116675674688";
        String x = "abcd";
        x = encrypt2(x);
        System.out.println("cipher:"+x);

//        String a = "283959e9515b21afaf3e2bfa216f232a9c5715c3a168ff5754";
        x = decrypt2(x);
        System.out.println("plain:"+x);
    }
}
