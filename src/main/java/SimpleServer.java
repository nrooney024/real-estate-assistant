package src.main.java;

import src.main.java.util.RequestHandler;
import src.main.java.util.AddressExtractor;
import src.main.java.util.HaversineCalculator;
import src.main.java.api.OverPass;
import src.main.java.api.OpenCageGeocoder;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

import java.util.ArrayList;
import java.util.Arrays;

import javax.management.openmbean.TabularType; 



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

    private static void handler(HttpExchange exchange) throws IOException {
        // Initializing jsonBuilder to be sent in response
        StringBuilder jsonBuilder = new StringBuilder("{");

        // Extracting address from GET request

        // Initializing encodedString
        String encodedString = "";
        try {
            String body = RequestHandler.parseBody(exchange.getRequestBody());
            System.out.println("\nReceived: " + body);
    
            if (!body.isEmpty()) {
                jsonBuilder.append("\"received-address\": ").append(body).append(",");
                encodedString = AddressExtractor.extractAndEncode(body);
                System.out.println("\nEncoded String: " + encodedString);
                
            } else {
                jsonBuilder.append("\"received-address\": ").append("\"Error reading /fetch-address-data request body\"").append(",");
            }
        } catch (IOException e) {
            System.err.println("Error reading request body: " + e.getMessage()); 
            throw e;
        }
        

        // Sending address to OpenCage in return for latitude and longitude coordinates.

        // Pulling OpenCage API key
        String opencageApiKey = System.getenv("API_KEY");
        if (opencageApiKey == null) {
            System.err.println("API key not found in environment variables");
            System.exit(1);
        }

        // Using OpenCageGeocoder class to get latitude and longitude coordinates of the address.
        OpenCageGeocoder geocoder = new OpenCageGeocoder(opencageApiKey);
        double[] coordinates = geocoder.getCoordinates(encodedString); // Fetch coordinates

        // Check if OpenCage successfully pulled both coordinates
        if (coordinates != null && coordinates.length == 2) {
            
            // Add the coordinates to the JSON builder
            double latitude = coordinates[0];
            double longitude = coordinates[1];
            geocoder.appendCoordinatesToJson(latitude, longitude, jsonBuilder);

            // Listing out OverPass requests by searchTermType and searchTerm pairings
            String[][] placeTypes = {
                {"shop", "supermarket"},
                {"leisure", "fitness_centre"},
                {"amenity", "cafe"},
                {"amenity", "school"},
                {"leisure", "park"},
                {"amenity", "bank"}
            };
            
            // Loop over each searchTermType and searchTerm, and create an OverPass call
            for (String[] placeType : placeTypes) {
                String category = placeType[0];
                String type = placeType[1];
                // Create and execute OverPass call for the current place type
                OverPass overPassCall = new OverPass(exchange, jsonBuilder, category, type, latitude, longitude);
                overPassCall.executeWorkflow();
            }
            
            // Send response
            response(exchange, jsonBuilder);

        }else {
            // Handle the case where coordinates couldn't be fetched
            System.err.println("Failed to fetch coordinates for address: " + encodedString);
        }

    };

    // This method sends a response to the /FETCH-ADDRESS-DATA GET request
    public static void response(HttpExchange exchange, StringBuilder jsonBuilder) {
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
        
        // Send response headers
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
            public void handle(HttpExchange exchange) {
                try {
                    // Check if the request method is OPTIONS or POST and call the handler accordingly
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
            
                        handler(exchange); // This is where IOException may be thrown
                    } else {
                        // If the request method is not OPTIONS or POST, send a 405 Method Not Allowed response
                        exchange.sendResponseHeaders(405, -1);
                    }
                } catch (IOException e) {
                    // Log the error
                    e.printStackTrace(); 
                    // Respond with an HTTP error code
                    try {
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
                        // Close the exchange to complete the response
                        exchange.getResponseBody().close();
                    } catch (IOException ioException) {
                        // Handle the case where the exchange cannot be closed
                        ioException.printStackTrace();
                    }
                }
            }
        });

        // Start the server
        server.start();
    }
}