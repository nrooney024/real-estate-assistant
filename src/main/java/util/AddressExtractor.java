package src.main.java.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AddressExtractor {

    public static String extractAndEncode(String body) {
        // Assuming the address extraction logic remains the same
        String extractedString = body.substring(body.indexOf("[") + 2, body.lastIndexOf("\""));
        try {
            return URLEncoder.encode(extractedString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }
}
