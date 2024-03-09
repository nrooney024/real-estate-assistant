package src.main.java.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONObject;

public class OpenCageGeocoder {

    private final String apiKey;

    public OpenCageGeocoder(String apiKey) {
        this.apiKey = apiKey;
    }

    // Method to fetch coordinates from OpenCage API
    public double[] getCoordinates(String address) {
        double[] coordinates = new double[2]; // Default to an array of size 2 for lat, lng
        try {
            String encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
            String requestUrl = "https://api.opencagedata.com/geocode/v1/json?q=" + encodedAddress + "&key=" + this.apiKey;

            URI uri = new URI(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            if(jsonResponse.getJSONArray("results").length() > 0) {
                JSONObject results = jsonResponse.getJSONArray("results").getJSONObject(0);
                JSONObject geometry = results.getJSONObject("geometry");

                coordinates[0] = geometry.getDouble("lat"); // Latitude
                coordinates[1] = geometry.getDouble("lng"); // Longitude
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Error fetching coordinates for address: " + address);
            System.err.println(e.getMessage());
            // Handle error appropriately, could set coordinates to a specific error value
            coordinates[0] = Double.NaN;
            coordinates[1] = Double.NaN;
        }
        return coordinates;
    }

    // Method to append coordinates to a StringBuilder (jsonBuilder)
    public void appendCoordinatesToJson(double latitude, double longitude, StringBuilder jsonBuilder) {
        // Check for valid coordinates before appending
        if(!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            jsonBuilder.append("\"opencagedata-latitude\": ").append(latitude).append(",");
            jsonBuilder.append("\"opencagedata-longitude\": ").append(longitude).append(",");
        } else {
            jsonBuilder.append("\"opencagedata-error\": \"Unable to fetch coordinates\",");
        }
    }
}
