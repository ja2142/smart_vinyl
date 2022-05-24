package tk.letsplaybol.smartvinyl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tk.letsplaybol.smart_vinyl.util.ResourceLocationCoder;

public class TestResourceLocationCoder {

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "123",
            "Darude - Sandstorm",
            "qwertyuiopasdfghjklzxcvbnm,./p[]l;'\1234567890-=" })
    void encodeDecodeIdentity(String orig) {
        String encoded = ResourceLocationCoder.stringToLocation(orig);
        String decoded = ResourceLocationCoder.locationToString(encoded);

        System.out.println(encoded);
        System.out.println(decoded);

        assertEquals(orig, decoded);
    }
}
