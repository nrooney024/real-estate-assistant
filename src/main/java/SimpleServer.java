package src.main.java;

import src.main.java.util.HaversineCalculator;
import src.main.java.api.OverPass;
import src.main.java.api.OpenCageGeocoder;

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
        System.out.println("\nRunning tests..");

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
        System.out.println("\nReceived: " + body);
        // Updating responseJSON with a successful received-address-status response
        if (!body.isEmpty()) {
            jsonBuilder.append("\"received-address\": ").append(body).append(",");
        }else{
            jsonBuilder.append("\"received-address\": ").append("\"Error reading /fetch-address-data request body\"").append(",");
        }

        // EXTRACTING ADDRESS FROM /FETCH-ADDRESS-DATA RESPONSE AND CONVERTING TO UTF-8 ------------------------------------------------------

        // Extract the string from the query
        String extractedString = body.substring(body.indexOf("[") + 2, body.lastIndexOf("\""));
        System.out.println("\nExtracted String: " + extractedString);
        // Encode the string
        String encodedString;
        try {
            encodedString = URLEncoder.encode(extractedString, "UTF-8");
            System.out.println("\nEncoded String: " + encodedString);
        } catch (UnsupportedEncodingException e) {
            // Handle the exception, maybe log it, and/or rethrow as a RuntimeException
            e.printStackTrace();
            encodedString = "";
        }
        
        
        
        
        // SENDING ADDRESS TO OPENCAGE TO GET LATITUDE AND LONGITUDE ------------------------------------------------------

        // Assuming you've extracted the address and have it in `encodedString`
        String opencageApiKey = System.getenv("API_KEY");
        if (opencageApiKey == null) {
            System.err.println("API key not found in environment variables");
            System.exit(1);
        }

        OpenCageGeocoder geocoder = new OpenCageGeocoder(opencageApiKey);
        double[] coordinates = geocoder.getCoordinates(encodedString); // Fetch coordinates

        // If coordinates are valid, append them to jsonBuilder and proceed
        if (coordinates != null && coordinates.length == 2) {
            double latitude = coordinates[0];
            double longitude = coordinates[1];
            geocoder.appendCoordinatesToJson(latitude, longitude, jsonBuilder);
            
            // Asking overpass for closest supermarkets
            OverPass myOverPassCallSupermarkets = new OverPass(exchange,jsonBuilder,"shop","supermarket",latitude,longitude);
            myOverPassCallSupermarkets.postRequest();
            myOverPassCallSupermarkets.readPostResponse();
            myOverPassCallSupermarkets.sortByDistance();
            myOverPassCallSupermarkets.extractClosestEstablishments();
            myOverPassCallSupermarkets.convertToJson();

        
            // Asking overpass for closest gyms
            OverPass myOverPassCallGyms = new OverPass(exchange,jsonBuilder,"leisure","fitness_centre",latitude,longitude);
            myOverPassCallGyms.postRequest();
            myOverPassCallGyms.readPostResponse();
            myOverPassCallGyms.sortByDistance();
            myOverPassCallGyms.extractClosestEstablishments();
            myOverPassCallGyms.convertToJson();
            
            // Asking overpass for closest cafes
            OverPass myOverPassCallCafes = new OverPass(exchange,jsonBuilder,"amenity","cafe",latitude,longitude);
            myOverPassCallCafes.postRequest();
            myOverPassCallCafes.readPostResponse();
            myOverPassCallCafes.sortByDistance();
            myOverPassCallCafes.extractClosestEstablishments();
            myOverPassCallCafes.convertToJson();


            // Asking overpass for closest schools
            OverPass myOverPassCallSchools = new OverPass(exchange,jsonBuilder,"amenity","school",latitude,longitude);
            myOverPassCallSchools.postRequest();
            myOverPassCallSchools.readPostResponse();
            myOverPassCallSchools.sortByDistance();
            myOverPassCallSchools.extractClosestEstablishments();
            myOverPassCallSchools.convertToJson();

            // Asking overpass for closest parks
            OverPass myOverPassCallParks = new OverPass(exchange,jsonBuilder,"leisure","park",latitude,longitude);
            myOverPassCallParks.postRequest();
            myOverPassCallParks.readPostResponse();
            myOverPassCallParks.sortByDistance();
            myOverPassCallParks.extractClosestEstablishments();
            myOverPassCallParks.convertToJson();

            // Asking overpass for closest banks
            OverPass myOverPassCallBanks = new OverPass(exchange,jsonBuilder,"amenity","bank",latitude,longitude);
            myOverPassCallBanks.postRequest();
            myOverPassCallBanks.readPostResponse();
            myOverPassCallBanks.sortByDistance();
            myOverPassCallBanks.extractClosestEstablishments();
            myOverPassCallBanks.convertToJson();
            myOverPassCallBanks.response();
        }else {
            // Handle the case where coordinates couldn't be fetched
            System.err.println("Failed to fetch coordinates for address: " + encodedString);
        }

    };



    public static void serverMain(String[] args) throws IOException {
        // Define the port number
        int port = 8080;
        // Create a new HTTP server on the specified port
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Print a message to the console indicating the server has started
        System.out.println("\nServer started on port: " + port);

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