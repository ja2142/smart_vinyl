package tk.letsplaybol.smart_vinyl.util;

import java.nio.charset.StandardCharsets;

public class Utf8Coder {

    public static byte[] utf8encode(String str){
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String utf8decode(byte[] bytes){
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
