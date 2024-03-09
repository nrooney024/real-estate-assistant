package src.main.java.api;

import src.main.java.util.HaversineCalculator; 
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

    // Properties
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

    // Constructor
    public OverPass(HttpExchange exchange, StringBuilder jsonBuilder, String searchTermType, String searchTerm, Double latitude, Double longitude) {
        // Initialize your properties here if necessary
        this.searchTermType = searchTermType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.jsonBuilder = jsonBuilder;
        this.exchange = exchange;
        this.searchTerm = searchTerm;
    }



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

    // This method sends a post request.
    // You will need to add your specific implementation details.
    public void postRequest() {
        
        System.out.println("\nStarting postRequest...");
        // Placeholder for post request implementation
        overpassPostBody = "[out:json];\n" +
        "(\n" +
        "  node[\"" + searchTermType + "\"=\"" + searchTerm + "\"](around:5000," + latitude + "," + longitude + ");\n" +
        "  way[\"" + searchTermType + "\"=\"" + searchTerm + "\"](around:5000," + latitude + "," + longitude + ");\n" +
        "  relation[\"" + searchTermType + "\"=\"" + searchTerm + "\"](around:5000," + latitude + "," + longitude + ");\n" +
        ");\n" +
        "out center;";

        // Print the POST request body to the console
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
    // Add your specific code to process the response.
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
                
                // Print the response to the console
                System.out.println("OverPass POST Response: " + overpassResponse.toString());

            } catch (IOException e){
                e.printStackTrace();
            }
            
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
    // Fill in with your sorting logic.
    public void sortByDistance() {
        // Placeholder for sorting by distance implementation
        JSONObject obj = new JSONObject(overpassResponse.toString()); // Parsing the JSON string into a JSON object
        JSONArray elements = obj.getJSONArray("elements"); // Extracting the array of elements (supermarkets)

        establishments = new Establishment[elements.length()]; // Creating an array to hold supermarket objects

        System.out.println("\nEstablishments loop starting...");
        System.out.println("\nElements length: " + elements.length());

        // Looping through each element in the JSON array
        for (int i = 0; i < elements.length(); i++) {
            try {
                JSONObject element = elements.getJSONObject(i); // Getting the individual supermarket JSON object
                
                double lat;
                double lon;

                
                if (element.has("lat") && element.has("lon")) {
                    // Directly use lat and lon if present
                    lat = element.getDouble("lat"); // Extracting the latitude
                    lon = element.getDouble("lon"); // Extracting the longitude
                } else if (element.has("center")) {
                    // Check inside the center object if lat and lon are not directly present
                    JSONObject center = element.getJSONObject("center");
                    lat = center.optDouble("lat", 0.0); // Default to 0.0 if lat is not present
                    lon = center.optDouble("lon", 0.0); // Default to 0.0 if lon is not present
                } else {
                    // Default to 0.0 if neither direct nor center lat/lon are present
                    lat = 0.0;
                    lon = 0.0;
                }
                
                double distance = HaversineCalculator.haversine(latitude, longitude, lat, lon); // Calculating the distance from the input coordinates
                // System.out.println("distance in loop: " + distance);
                String name = element.getJSONObject("tags").optString("name", "Unknown"); // Extracting the name, defaulting to "Unknown" if not found
                // System.out.println("name in loop: " + name);
                establishments[i] = new Establishment(name, lat, lon, distance); // Creating a new Supermarket object and adding it to the array
                System.out.println("\ni is equal to: " + i);
                System.out.println("\nestablishments[i] in loop: " + establishments[i]);
            } catch (Exception e) {
                System.out.println("\nSkipped this iteration: " + i);
                JSONObject element = elements.getJSONObject(i); 
                System.out.println("\nSkipped element: " + element);
                e.printStackTrace();
            }
            
        }

        // Print statements for the Establishments before the sort
        // System.out.println("\n\nEstablishments: " + establishments);
        // for (Establishment est : establishments) {
        //     System.out.println(est);
        // }
        

        Arrays.sort(establishments); // Sorting the supermarkets array based on the distance

        // Print statements for the Establishments after the sort
        // System.out.println("\n\nEstablishments after sort method: " + establishments);
        // for (Establishment est : establishments) {
        //     System.out.println(est);
        // }
        

    }


    // // This method extracts the closest establishments from the post response.
    // // Implement your logic to extract and process the establishments.
    public void extractClosestEstablishments() {
        // Placeholder for extracting closest establishments implementation
        // Breaking out the closest supermarkets into their own array

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


    // // This method converts the sorted establishments to JSON format.
    // // Implement your JSON conversion logic here.
    public void convertToJson() {
        System.out.println("\nconvertToJson is running..." );
        // Placeholder for converting to JSON implementation
        JSONArray closestEstablishmentsJSON = new JSONArray(); // Initialize a new JSONArray
        // Loop through the closest supermarkets and add them to the JSONArray
        
        System.out.println("closestEstablishments within convertToJson: " + closestEstablishments);
        for (Establishment establishment : closestEstablishments) {
            System.out.println("\nAbout to start loop through closestEstablishments...");
            if (establishment != null) {
                System.out.println("Establishment in loop: " + establishment);
                JSONObject establishmentJSON = new JSONObject(); // Create a new JSONObject
                establishmentJSON.put("name", establishment.name); // Add name
                establishmentJSON.put("lat", establishment.lat); // Add latitude
                establishmentJSON.put("lon", establishment.lon); // Add longitude
                establishmentJSON.put("distance-km", establishment.distance); // Add distance

                closestEstablishmentsJSON.put(establishmentJSON); // Add the supermarket JSONObject to the JSONArray
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

    public void response() {
        System.out.println("\nresponse starting...");
        // Finalizing JSON repsonse
        if (jsonBuilder.toString().endsWith(",")) {
            jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
        } 
        jsonBuilder.append("}");
        String jsonString = jsonBuilder.toString();

        System.out.println("\n\njsonString: " + jsonString);

        // Create response headers
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        

        try{
            byte[] responseBytes = jsonString.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            System.out.println("\nSent successful response headers");
        } catch (IOException e){
            e.printStackTrace();
        }

        // Send response through output stream
        OutputStream os = exchange.getResponseBody();
        
        try{
            os.write(jsonString.getBytes());
            os.close();
            System.out.println("\nSent jsonString as response...");
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // // Getters and Setters for the properties
    // public String getOverpassPostBody() {
    //     return overpassPostBody;
    // }

    // public void setOverpassPostBody(String overpassPostBody) {
    //     this.overpassPostBody = overpassPostBody;
    // }

    // public String getSearchTerm() {
    //     return searchTerm;
    // }

    // public void setSearchTerm(String searchTerm) {
    //     this.searchTerm = searchTerm;
    // }

    // public String getPostResponse() {
    //     return postResponse;
    // }

    // public void setPostResponse(String postResponse) {
    //     this.postResponse = postResponse;
    // }
}