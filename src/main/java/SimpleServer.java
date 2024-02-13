package src.main.java;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*; // Importing the JSON handling library
import java.util.Arrays; // Importing Arrays utility class for sorting



// Define the main class
public class SimpleServer {
    // Define the main method
    public static void main(String[] args) throws IOException {
        // get the runtype argument from the command line
        if(args.length < 1) {
            serverMain(args);
        } else {
            String runtype = args[0];
            // Check if the runtype is "client"
            if ("test".equals(runtype)) {
                // Run the client main method
                testMain();
            } else {
                // Print an error message to the console
                System.err.println("Invalid runtype argument");
                // Exit the program with an error code
                System.exit(1);
            }
        }
    }

    public static void testMain() {
        System.out.println("Running tests..");

        // Test 1: Test call to /fetch-address-data handler with 'nice' input
        // handler('{"content":"304 W 147th St, # 88, New York, NY 10039"}');

    }

    private static void handler(HttpExchange exchange) {

        

        // Initialize a JSON object to store the response
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
    
        
        // CONVERTING /FETCH-ADDRESS-DATA RESPONSE INTO READABLE FORMAT ------------------------------------------------------


        // Get the InputStream from the exchange
        InputStream is = exchange.getRequestBody();
    
        // Use InputStreamReader and BufferedReader to read from the InputStream
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        // Use a StringBuilder to collect the lines
        StringBuilder bodyBuilder = new StringBuilder();
        String lineOfBody;
        
        try{
            while ((lineOfBody = br.readLine()) != null) {
                bodyBuilder.append(lineOfBody);
            }
        } catch (IOException e) {
            e.printStackTrace(); // For now, just print the stack trace to the console
        }

            
        // Convert the StringBuilder to a String
        String body = bodyBuilder.toString();
        // Print the received data to the console
        System.out.println("Received: " + body);
        // Updating responseJSON with a successful received-address-status response
        if (!body.isEmpty()) {
            jsonBuilder.append("\"received-address\": ").append(body).append(",");
        }else{
            jsonBuilder.append("\"received-address\": ").append("\"Error reading /fetch-address-data request body\"").append(",");
        }

        // EXTRACTING ADDRESS FROM /FETCH-ADDRESS-DATA RESPONSE AND CONVERTING TO UTF-8 ------------------------------------------------------

        // Extract the string from the query
        String extractedString = body.substring(body.indexOf("[") + 2, body.lastIndexOf("\""));
        System.out.println("Extracted String: " + extractedString);
        // Encode the string
        String encodedString;
        try {
            encodedString = URLEncoder.encode(extractedString, "UTF-8");
            System.out.println("Encoded String: " + encodedString);
        } catch (UnsupportedEncodingException e) {
            // Handle the exception, maybe log it, and/or rethrow as a RuntimeException
            e.printStackTrace();
            encodedString = "";
        }
        
        
        
        
        // SENDING ADDRESS TO OPENCAGE TO GET LATITUDE AND LONGITUDE ------------------------------------------------------

        // Pulling API_KEY from enovironment variables
        String opencageApiKey = System.getenv("API_KEY");
        if (opencageApiKey == null) {
            System.err.println("API key not found in environment variables");
            System.exit(1);
        }
        // Send GET request to convert address to coordinates
        String opencageUrlString = "https://api.opencagedata.com/geocode/v1/json?q=" + encodedString + "&key=" + opencageApiKey;
        // Print opencage GET URL
        System.out.println("This is the OpenCage URL: " + opencageUrlString);
        // Initializing latitude and longitude
        double latitude = 0.0;
        double longitude = 0.0;

        try {
            URI openCageUri = new URI(opencageUrlString);
            HttpURLConnection connection = null;
            try{
                connection = (HttpURLConnection) openCageUri.toURL().openConnection();
            } catch (MalformedURLException e) {
                e.printStackTrace(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            

            try {
                connection.setRequestMethod("GET");
            } catch (ProtocolException e){
                e.printStackTrace();
            }


            BufferedReader opencageReader = null;

            // Read the response from the GET request
            try {
                opencageReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } catch (IOException e){
                e.printStackTrace();
            }

            StringBuilder opencageResult = new StringBuilder();
            String line;
            
            try{
                while ((line = opencageReader.readLine()) != null) {
                    opencageResult.append(line);
                }
                opencageReader.close();
            } catch (IOException e){
                e.printStackTrace();
            }
            
            

            // Updating Opencage status in response JSON
            if (!opencageResult.isEmpty()) {
                jsonBuilder.append("\"opencagedata-get-status\": ").append("\"Success\"").append(",");
            } else {
                jsonBuilder.append("\"opencagedata-status\": ").append("\"Failed\"").append(",");
            }

            // Print the response to the console
            System.out.println("Opencage GET Response: " + opencageResult.toString());

            // Extract the latitude coordinates from the GET response
            String opencageJsonResponse = opencageResult.toString();

            // Extract the relevant portion of the string
            String opencageResultsSection = opencageJsonResponse.substring(opencageJsonResponse.indexOf("\"results\":"));

            // Find the geometry section
            String opencageGeometrySection = opencageResultsSection.substring(opencageResultsSection.indexOf("\"geometry\":"));

            // Find the lat and lng values
            String latStr = opencageGeometrySection.substring(opencageGeometrySection.indexOf("\"lat\":") + 6, opencageGeometrySection.indexOf(","));
            String lngStr = opencageGeometrySection.substring(opencageGeometrySection.indexOf("\"lng\":") + 6, opencageGeometrySection.indexOf("}"));

            // Convert latitude and longitude strings to doubles
            try {
                latitude = Double.parseDouble(latStr);
                longitude = Double.parseDouble(lngStr);
            } catch (NumberFormatException e) {
                System.out.println("Error parsing latitude or longitude: " + e.getMessage());
                latitude = 0.0;
                longitude = 0.0;
            }

            // Print out the latitude and longitude
            System.out.println("Latitude: " + latitude + ", Longitude: " + longitude);

            // Updating Opencage latitude in response JSON
            if (latitude != 0.0) {
                jsonBuilder.append("\"opencagedata-latitude\": ").append(Double.toString(latitude)).append(",");
            } else {
                jsonBuilder.append("\"opencagedata-latitude\": ").append("\"Failed\"").append(",");
            }

            // Updating Opencage longitude in response JSON
            if (longitude != 0.0) {
                jsonBuilder.append("\"opencagedata-longitude\": ").append(Double.toString(longitude)).append(",");
            } else {
                jsonBuilder.append("\"opencagedata-longitude\": ").append("Failed").append(",");
            }

           
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


        // SENDING THE LATITUDE AND LONGITUDE COORDINATES TO OVERPASS, TO RECEIVE THE SUPERMARKET DATA ------------------------------------------------------

        try{
            // Create the POST request body
            String overpassPostBody = "[out:json];\n" +
                "(\n" +
                "  node[\"shop\"=\"supermarket\"](around:5000," + latitude + "," + longitude + ");\n" +
                "  way[\"shop\"=\"supermarket\"](around:5000," + latitude + "," + longitude + ");\n" +
                "  relation[\"shop\"=\"supermarket\"](around:5000," + latitude + "," + longitude + ");\n" +
                ");\n" +
                "out center;";


            // Print the POST request body to the console
            System.out.println("OverPass POST Request Body: " + overpassPostBody);

            URI overpassUri= null;

            try {
                // Create a new URL object for the POST request
                overpassUri = new URI("https://overpass-api.de/api/interpreter");    
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            HttpURLConnection overpassConnection = (HttpURLConnection) overpassUri.toURL().openConnection();
            overpassConnection.setRequestMethod("POST");
            overpassConnection.setRequestProperty("Content-Type", "text/plain");
            overpassConnection.setDoOutput(true);

            // Write the POST request body to the connection
            OutputStream overpassOutputStream = overpassConnection.getOutputStream();
            overpassOutputStream.write(overpassPostBody.getBytes()); 
            overpassOutputStream.flush();
            overpassOutputStream.close();

            // Read the response from the POST request
            BufferedReader overpassReader = new BufferedReader(new InputStreamReader(overpassConnection.getInputStream()));
            StringBuilder overpassResult = new StringBuilder();
            String overpassLine;
            
            try {
                while ((overpassLine = overpassReader.readLine()) != null) {
                    overpassResult.append(overpassLine);
                }
                overpassReader.close();
            } catch (IOException e){
                e.printStackTrace();
            }
            
          
            

            // Print the response to the console
            System.out.println("OverPass POST Response: " + overpassResult.toString());

            if (!overpassResult.isEmpty()) {
                jsonBuilder.append("\"overpass-response\": ").append(overpassResult.toString()).append(",");
            } else {
                jsonBuilder.append("\"overpass-response\": ").append("\"Null\"").append(",");
            }


            // EXTRACT THREE CLOSEST SUPERMARKETS FROM JSON ------------------------------------------------------

            System.out.println("Beginning to gather three closest supermarkets...");

            JSONObject obj = new JSONObject(overpassResult.toString()); // Parsing the JSON string into a JSON object
            JSONArray elements = obj.getJSONArray("elements"); // Extracting the array of elements (supermarkets)

            Supermarket[] supermarkets = new Supermarket[elements.length()]; // Creating an array to hold supermarket objects

            System.out.println("Supermarkets loop starting...");
            System.out.println("Elements length: " + elements.length());

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
                    
                    double distance = haversine(latitude, longitude, lat, lon); // Calculating the distance from the input coordinates
                    // System.out.println("distance in loop: " + distance);
                    String name = element.getJSONObject("tags").optString("name", "Unknown"); // Extracting the name, defaulting to "Unknown" if not found
                    // System.out.println("name in loop: " + name);
                    supermarkets[i] = new Supermarket(name, lat, lon, distance); // Creating a new Supermarket object and adding it to the array
                    System.out.println("i is equal to: " + i);
                    System.out.println("supermarkets[i] in loop: " + supermarkets[i]);
                } catch (Exception e) {
                    System.out.println("Skipped this iteration: " + i);
                    JSONObject element = elements.getJSONObject(i); 
                    System.out.println("Skipped element: " + element);
                    e.printStackTrace();
                }
                
            }


            System.out.println("\n\n\n\n");

            System.out.println("Supermarkets: " + supermarkets);

            Arrays.sort(supermarkets); // Sorting the supermarkets array based on the distance


            // Breaking out the closest supermarkets into their own array

            Supermarket[] closestSupermarkets; // Declare the closestSupermarkets array
            // Initialize closestSupermarkets with the smaller of 3 or supermarkets.length
            closestSupermarkets = new Supermarket[Math.min(10, supermarkets.length)];
            System.out.println("Closest supermarket loop...");
            // Output the top 3 supermarkets, or less if there are not enough supermarkets
            for (int i = 0; i < closestSupermarkets.length; i++) {
                System.out.println("i in loop equals: " + i);
                closestSupermarkets[i] = supermarkets[i]; // Add supermarket to closestSupermarkets
                System.out.println(supermarkets[i]); // Printing the supermarket details
            }

        
            JSONArray closestSupermarketsJSON = new JSONArray(); // Initialize a new JSONArray
            // Loop through the closest supermarkets and add them to the JSONArray
            for (Supermarket supermarket : closestSupermarkets) {
                if (supermarket != null) {
                    JSONObject supermarketJSON = new JSONObject(); // Create a new JSONObject
                    supermarketJSON.put("name", supermarket.name); // Add name
                    supermarketJSON.put("lat", supermarket.lat); // Add latitude
                    supermarketJSON.put("lon", supermarket.lon); // Add longitude
                    supermarketJSON.put("distance-km", supermarket.distance); // Add distance

                    closestSupermarketsJSON.put(supermarketJSON); // Add the supermarket JSONObject to the JSONArray
                }
            }

            // Add the JSONArray to your JSON response
            if (closestSupermarketsJSON.length() > 0) {
                jsonBuilder.append("\"closest-supermarkets\": ").append(closestSupermarketsJSON.toString()).append(",");
            } else {
                jsonBuilder.append("\"closest-supermarkets\": ").append("\"Null\"").append(",");
            }


            // RESPOND WITH JSON ------------------------------------------------------

            // Finalizing JSON repsonse
            if (jsonBuilder.toString().endsWith(",")) {
                jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
            } 
            jsonBuilder.append("}");
            String jsonString = jsonBuilder.toString();

            // Create response headers
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            

            try{
                byte[] responseBytes = jsonString.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
            } catch (IOException e){
                e.printStackTrace();
            }

            // Send response through output stream
            OutputStream os = exchange.getResponseBody();
            
            try{
                os.write(jsonString.getBytes());
                os.close();
            } catch (IOException e){
                e.printStackTrace();
            }
            
            

        } catch (IOException e) {
            // If the request method is not GET, send a 405 Method Not Allowed response
            // exchange.sendResponseHeaders(405, -1);
            e.printStackTrace();
        }
    };  

    // Method for finding the distance between two sets of coordinates
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // convert to kilometers
    }


    static class Supermarket implements Comparable<Supermarket> {
        String name;
        double lat;
        double lon;
        double distance;

        public Supermarket(String name, double lat, double lon, double distance) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
        }

        @Override
        public int compareTo(Supermarket other) {
            return Double.compare(this.distance, other.distance);
        }

        @Override
        public String toString() {
            return "Name: " + this.name + ", Lat: " + this.lat + ", Lon: " + this.lon + ", Distance: " + this.distance + " km";
        }
    }



    public static void serverMain(String[] args) throws IOException {
        // Define the port number
        int port = 8080;
        // Create a new HTTP server on the specified port
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Print a message to the console indicating the server has started
        System.out.println("Server started on port: " + port);

        // Define a new context for the server
        server.createContext("/fetch-address-data", new HttpHandler() {

            // Define the handle method for incoming requests
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                // Check if the request method is OPTIONS
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    // Set the CORS headers for preflight requests
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    // Set the CORS headers for actual requests
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                    handler(exchange);           

                } else {
                    // If the request method is not OPTIONS or POST, send a 405 Method Not Allowed response
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // Start the server
        server.start();
    }
}