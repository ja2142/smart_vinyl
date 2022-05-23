package tk.letsplaybol.smart_vinyl.util;

import org.apache.commons.codec.binary.Base32;

// back and forth conversion to store arbitrary string in ResourceLocation
// (which only allows lowercase alphanumeric and some limited punctuation)
public class ResourceLocationCoder {
    public static String stringToLocation(String s) {
        return new Base32().encodeAsString(
                Utf8Coder.utf8encode(s))
                .toLowerCase()
                .replace("=", "");
    }

    public static String locationToString(String locationBase32) {
        // the very idea of basex padding is ridiculous
        while (locationBase32.length() % 8 != 0) {
            locationBase32 += "=";
        }
        return Utf8Coder.utf8decode(new Base32().decode(locationBase32.toLowerCase()));
    }
}
