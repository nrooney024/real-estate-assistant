package src.main.java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class RequestHandler {
    
    private String body; 

    public RequestHandler(InputStream inputStream) throws IOException {
        this.body = parseBody(inputStream); // Store the parsed body
    }

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

    public JSONObject buildJsonResponse() {
        JSONObject jsonResponse = new JSONObject();
        if (!this.body.isEmpty()) {
            jsonResponse.put("received-address", this.body);
        } else {
            jsonResponse.put("received-address", "Error reading /fetch-address-data request body");
        }
        return jsonResponse;
    }

    
}
