package tk.letsplaybol.smart_vinyl.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Utf8Coder {
    private static final Charset utf8 = Charset.forName("UTF-8");

    public static byte[] utf8encode(String str){
        return utf8.encode(str).array();
    }

    public static String utf8decode(byte[] bytes){
        return utf8.decode(ByteBuffer.wrap(bytes)).toString();
    }
}
