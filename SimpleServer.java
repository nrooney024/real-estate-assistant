// Import necessary libraries
import com.sun.net.httpserver.*;
import java.io.*;
// For the GET request
import java.net.*;
import java.nio.charset.StandardCharsets;

// Define the main class
public class SimpleServer {
    // Define the main method
    public static void main(String[] args) throws IOException {
        // Define the port number
        int port = 8080;
        // Create a new HTTP server on the specified port
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Print a message to the console indicating the server has started
        System.out.println("Server started on port: " + port);

        // Define a new context for the server
        server.createContext("/receive-data", new HttpHandler() {

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

                    // Create a new InputStreamReader for the request body
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    // Create a new BufferedReader for the InputStreamReader
                    BufferedReader br = new BufferedReader(isr);

                    // Read the first line of the request body
                    String query = br.readLine();

                    // Print the received data to the console
                    System.out.println("Received: " + query);

                    // Extract the string from the query
                    String extractedString = query.substring(query.indexOf("[") + 2, query.lastIndexOf("\""));
                    System.out.println("Extracted String: " + extractedString);
                    
                    // Encode the string
                    String encodedString = URLEncoder.encode(extractedString, "UTF-8");
                    System.out.println("Encoded String: " + encodedString);

                    // Define the response string
                    String response = "Echo: " + query;

                    // Calculate the length of the response string in bytes, not characters
                    int responseLength = response.getBytes(StandardCharsets.UTF_8).length;

                    System.out.println("Response Length Chkpnt 1: " + responseLength);

                    // Send the response headers
                    exchange.sendResponseHeaders(200, responseLength);

                    // Get the response body as an OutputStream
                    OutputStream os = exchange.getResponseBody();

                    System.out.println("Response Length Chkpnt 2: " + responseLength);
                    
                    // Write the response string to the OutputStream
                    os.write(response.getBytes());
                    
                    // Close the OutputStream
                    os.close();


                    // Pulling API_KEY from .env file
                    String apiKey = System.getenv("API_KEY");
                    if (apiKey == null) {
                        System.err.println("API key not found in environment variables");
                        System.exit(1);
                    }

                    // Send GET request to convert address to coordinates
                    // String address = query; // Replace with the actual address from the POST request
                    String urlString = "https://api.opencagedata.com/geocode/v1/json?q=" + encodedString + "&key=" + apiKey;

                    // Print "This is the URL: " + urlString to the console
                    System.out.println("This is the URL: " + urlString);

                    try {
                        // Create a new URL object from the URL string
                        URI uri = new URI(urlString);
                        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                        connection.setRequestMethod("GET");

                        // Read the response from the GET request
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        reader.close();

                        // Print the response to the console
                        System.out.println("GET Response: " + result.toString());

                        // Extract the latitude coordinates from the GET response
                        String jsonResponse = result.toString();

                        // Extract the relevant portion of the string
                        String resultsSection = jsonResponse.substring(jsonResponse.indexOf("\"results\":"));

                        // Find the geometry section
                        String geometrySection = resultsSection.substring(resultsSection.indexOf("\"geometry\":"));

                        // Find the lat and lng values
                        String latStr = geometrySection.substring(geometrySection.indexOf("\"lat\":") + 6, geometrySection.indexOf(","));
                        String lngStr = geometrySection.substring(geometrySection.indexOf("\"lng\":") + 6, geometrySection.indexOf("}"));

                        // Convert string to double
                        double latitude = Double.parseDouble(latStr);
                        double longitude = Double.parseDouble(lngStr);

                        // Print out the latitude and longitude
                        System.out.println("Latitude: " + latitude + ", Longitude: " + longitude);
                        
                        // Create the POST request body
                        String postBody = "[out:json];\n" +
                                "(\n" +
                                "  node[\"shop\"=\"supermarket\"](around:5000," + latitude + "," + longitude + ");\n" +
                                "  way[\"shop\"=\"supermarket\"](around:5000," + latitude + "," + longitude + ");\n" +
                                "  relation[\"shop\"=\"supermarket\"](around:5000," + latitude + "," + longitude + ");\n" +
                                ");\n" +
                                "out center;";


                        // Print the POST request body to the console
                        System.out.println("POST Request Body: " + postBody);

                        // Create a new URL object for the POST request
                        URI postUri = new URI("https://overpass-api.de/api/interpreter");
                        HttpURLConnection postConnection = (HttpURLConnection) postUri.toURL().openConnection();
                        postConnection.setRequestMethod("POST");
                        postConnection.setRequestProperty("Content-Type", "text/plain");
                        postConnection.setDoOutput(true);

                        // Write the POST request body to the connection
                        OutputStream postOutputStream = postConnection.getOutputStream();
                        postOutputStream.write(postBody.getBytes());
                        postOutputStream.flush();
                        postOutputStream.close();

                        // Read the response from the POST request
                        BufferedReader postReader = new BufferedReader(new InputStreamReader(postConnection.getInputStream()));
                        StringBuilder postResult = new StringBuilder();
                        String postLine;
                        while ((postLine = postReader.readLine()) != null) {
                            postResult.append(postLine);
                        }
                        postReader.close();

                        // Print the response to the console
                        System.out.println("POST Response: " + postResult.toString());

                    } catch (URISyntaxException e) {
                        System.out.println("Invalid URL");
                        e.printStackTrace();
                    }

                } else {
                    // If the request method is not OPTIONS or POST, send a 405 Method Not Allowed response
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });



        // Define a new context for the server
        // TODO: Pull this handler out of this scope.
        server.createContext("/get-supermarkets", new HttpHandler() {

        // Define the handle method for incoming requests
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            
            System.out.println("GET Checkpoint 1");

            // Check if the request method is OPTIONS
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            System.out.println("GET Checkpoint 2");

            // Check if the request method is GET
            if ("GET".equals(exchange.getRequestMethod())) {
                // Set the CORS headers for the request
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                System.out.println("GET Checkpoint 3");

                // Define the response string
                String response = "stuff"; //postResult.toString();

                // Calculate the length of the response string in bytes, not characters
                int contentLength = response.getBytes(StandardCharsets.UTF_8).length;

                System.out.println("Response Length Chkpnt 1: " + contentLength);

                // Send the response headers
                exchange.sendResponseHeaders(200, contentLength);
                // Get the response body as an OutputStream
                OutputStream os = exchange.getResponseBody();
                // Write the response string to the OutputStream

                System.out.println("Response Length Chkpnt 2: " + response.length());

                os.write(response.getBytes());
                // Close the OutputStream
                os.close();
            } else {
                // If the request method is not GET, send a 405 Method Not Allowed response
                exchange.sendResponseHeaders(405, -1);
            
            }
        }
        });

        // Start the server
        server.start();
    }
}