package src.main.java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RequestParser {
    
    public static String parseBody(InputStream inputStream) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            bodyBuilder.append(line);
        }
        return bodyBuilder.toString();
    }
}
