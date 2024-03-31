package com.myapp.api;

import com.myapp.util.HaversineCalculator; 
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;


 // Class for managing Overpass connection
 public class OverPass {

    private String overpassPostBody;
    private String searchTermType;
    private StringBuilder overpassResponse;
    private Double latitude;
    private Double longitude;
    private HttpURLConnection overpassConnection;
    private StringBuilder jsonBuilder;
    private Establishment[] establishments;
    private Establishment[] closestEstablishments;
    private HttpExchange exchange;
    private String searchTerm;

    public OverPass(HttpExchange exchange, StringBuilder jsonBuilder, String searchTermType, String searchTerm, Double latitude, Double longitude) {
        this.searchTermType = searchTermType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.jsonBuilder = jsonBuilder;
        this.exchange = exchange;
        this.searchTerm = searchTerm;
    }

    // Each location being pulled from OverPass will be considered an "establishment"
    static class Establishment implements Comparable<Establishment> {
        String name;
        double lat;
        double lon;
        double distance;

        public Establishment(String name, double lat, double lon, double distance) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
        }

        @Override
        public int compareTo(Establishment other) {
            return Double.compare(this.distance, other.distance);
        }

        @Override
        public String toString() {
            return "Name: " + this.name + ", Lat: " + this.lat + ", Lon: " + this.lon + ", Distance: " + this.distance + " km";
        }
    }



    // Methods

    // Executes the complete workflow for processing overpass data.
    public void executeWorkflow() {
        postRequest();
        readPostResponse();
        sortByDistance();
        extractClosestEstablishments();
        convertToJson();
    }


    // This method sends a post request.
    public void postRequest() {
        
        System.out.println("\nStarting postRequest...");

        // Initializing post request body
        overpassPostBody = "[out:json];\n" +
        "(\n" +
        "  node[\"" + searchTermType + "\"=\"" + searchTerm + "\"](around:5000," + latitude + "," + longitude + ");\n" +
        "  way[\"" + searchTermType + "\"=\"" + searchTerm + "\"](around:5000," + latitude + "," + longitude + ");\n" +
        "  relation[\"" + searchTermType + "\"=\"" + searchTerm + "\"](around:5000," + latitude + "," + longitude + ");\n" +
        ");\n" +
        "out center;";

        System.out.println("\nOverPass POST Request Body: " + overpassPostBody);

        URI overpassUri = null;

        try {
            // Create a new URL object for the POST request
            overpassUri = new URI("https://overpass-api.de/api/interpreter");
            overpassConnection = (HttpURLConnection) overpassUri.toURL().openConnection();
            overpassConnection.setRequestMethod("POST");
            overpassConnection.setRequestProperty("Content-Type", "text/plain");
            overpassConnection.setDoOutput(true);

            // Write the POST request body to the connection
            try (OutputStream overpassOutputStream = overpassConnection.getOutputStream()) {
                overpassOutputStream.write(overpassPostBody.getBytes(StandardCharsets.UTF_8));
                overpassOutputStream.flush();
            } catch (IOException e) {
                System.err.println("Error writing to the output stream: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI syntax: " + e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL: " + e.getMessage());
            e.printStackTrace();
        } catch (ProtocolException e) {
            System.err.println("Protocol exception: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        }


    }


    // This method reads the post response.
    public void readPostResponse() {
        
        System.out.println("\nStarting readPostResponse...");

        if (overpassConnection != null) {
           try{
                // Read the response from the POST request
                BufferedReader overpassReader = new BufferedReader(new InputStreamReader(overpassConnection.getInputStream()));
                overpassResponse = new StringBuilder();
                String overpassLine;
           
                while ((overpassLine = overpassReader.readLine()) != null) {
                    overpassResponse.append(overpassLine);
                }
                overpassReader.close();
                
                System.out.println("OverPass POST Response: " + overpassResponse.toString());

            } catch (IOException e){
                e.printStackTrace();
            }
            
            // Appending the response to the jsonBuilder
            if (!overpassResponse.isEmpty()) {
                jsonBuilder.append("\"overpass-response for" + searchTermType + ": "+ searchTerm + "\": ").append(overpassResponse.toString()).append(",");
            } else {
                jsonBuilder.append("\"overpass-response for" + searchTermType + ": "+ searchTerm + "\": ").append("\"Null\"").append(",");
            }
        }else{
            System.out.println("OverPass Connection is not initialized.");
        }

    }


    // This method sorts the extracted establishments by distance.
    public void sortByDistance() {
        // Parsing the JSON string into a JSON object
        JSONObject obj = new JSONObject(overpassResponse.toString()); 
        // Extracting the array of elements (supermarkets)
        JSONArray elements = obj.getJSONArray("elements"); 
        // Creating an array to hold supermarket objects
        establishments = new Establishment[elements.length()]; 

        System.out.println("\nEstablishments loop starting...");
        System.out.println("\nElements length: " + elements.length());

        // Looping through each element in the JSON array
        for (int i = 0; i < elements.length(); i++) {
            try {
                // Getting the individual supermarket JSON object
                JSONObject element = elements.getJSONObject(i); 
                
                double lat;
                double lon;

                
                if (element.has("lat") && element.has("lon")) {
                    // Extracting the latitude and longitude
                    lat = element.getDouble("lat"); 
                    lon = element.getDouble("lon"); 
                } else if (element.has("center")) {
                    // Check inside the center object if lat and lon are not directly present
                    JSONObject center = element.getJSONObject("center");
                    // Default to 0.0 if lat is not present
                    lat = center.optDouble("lat", 0.0); 
                    // Default to 0.0 if lon is not present
                    lon = center.optDouble("lon", 0.0); 
                } else {
                    // Default to 0.0 if neither direct nor center lat/lon are present
                    lat = 0.0;
                    lon = 0.0;
                }
                // Calculating the distance from the input coordinates
                double distance = HaversineCalculator.haversine(latitude, longitude, lat, lon); 
                // Extracting the name, defaulting to "Unknown" if not found
                String name = element.getJSONObject("tags").optString("name", "Unknown"); 
                // Creating a new Supermarket object and adding it to the array
                establishments[i] = new Establishment(name, lat, lon, distance); 
                System.out.println("\ni is equal to: " + i);
                System.out.println("\nestablishments[i] in loop: " + establishments[i]);
            } catch (Exception e) {
                System.out.println("\nSkipped this iteration: " + i);
                JSONObject element = elements.getJSONObject(i); 
                System.out.println("\nSkipped element: " + element);
                e.printStackTrace();
            }
            
        }
        
        Arrays.sort(establishments); // Sorting the supermarkets array based on the distance

    }


    // // This method extracts the closest establishments from the post response.
    public void extractClosestEstablishments() {
        
        // Initialize closestSupermarkets with the smaller of 3 or supermarkets.length
        closestEstablishments = new Establishment[Math.min(10, establishments.length)];
        System.out.println("\nClosest supermarket loop...");
        // Output the top 3 supermarkets, or less if there are not enough supermarkets
        for (int i = 0; i < closestEstablishments.length; i++) {
            System.out.println("i in loop equals: " + i);
            closestEstablishments[i] = establishments[i]; // Add supermarket to closestSupermarkets
            System.out.println(establishments[i]); // Printing the supermarket details
        }

    }


    // This method converts the sorted establishments to JSON format.
    public void convertToJson() {
        System.out.println("\nconvertToJson is running..." );
        // Initialize a new JSONArray
        JSONArray closestEstablishmentsJSON = new JSONArray(); 
        System.out.println("closestEstablishments within convertToJson: " + closestEstablishments);
        // Loop through the closest establishments and add them to the JSONArray
        for (Establishment establishment : closestEstablishments) {
            System.out.println("\nAbout to start loop through closestEstablishments...");
            if (establishment != null) {
                System.out.println("Establishment in loop: " + establishment);
                // Create a new JSONObject
                JSONObject establishmentJSON = new JSONObject(); 
                // Add name
                establishmentJSON.put("name", establishment.name); 
                // Add latitude
                establishmentJSON.put("lat", establishment.lat); 
                // Add longitude
                establishmentJSON.put("lon", establishment.lon); 
                // Add distance
                establishmentJSON.put("distance", establishment.distance); 
                // Add the supermarket JSONObject to the JSONArray
                closestEstablishmentsJSON.put(establishmentJSON); 
            }
        }

        // Add the JSONArray to your JSON response
        if (closestEstablishmentsJSON.length() > 0) {
            jsonBuilder.append("\"closest-").append(searchTerm).append("\": ").append(closestEstablishmentsJSON.toString()).append(",");
            System.out.println("jsonBuilder: " + jsonBuilder);
        } else {
            jsonBuilder.append("\"closest-").append(searchTerm).append("\": ").append("\"Null\"").append(",");
            System.out.println("closestEstablishmentsJSON is empty...");
        }

    }


}