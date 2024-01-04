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

                    // Send the response headers
                    exchange.sendResponseHeaders(200, response.length());
                    // Get the response body as an OutputStream
                    OutputStream os = exchange.getResponseBody();
                    // Write the response string to the OutputStream
                    os.write(response.getBytes());
                    // Close the OutputStream
                    os.close();

                    // Send GET request to convert address to coordinates
                    // String address = query; // Replace with the actual address from the POST request
                    String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedString;

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
                        int latStartIndex = result.indexOf("\"lat\":") + 6;
                        int latEndIndex = result.indexOf(",", latStartIndex);
                        String latString = result.substring(latStartIndex, latEndIndex);
                        latString = latString.replace("\"", ""); // Remove quotation marks
                        System.out.println("Latitude String: " + latString);

                        double lat = 0.0;
                        try {
                            lat = Double.parseDouble(latString);
                            System.out.println("Latitude: " + lat);
                        } catch (NumberFormatException e) {
                            System.out.println("Error parsing latitude string to double:");
                            e.printStackTrace();
                        }

                        // Extract the longitude coordinates from the GET response
                        int lonStartIndex = result.indexOf("\"lon\":") + 6;
                        int lonEndIndex = result.indexOf(",", lonStartIndex);
                        String lonString = result.substring(lonStartIndex, lonEndIndex);
                        int commaIndex = lonString.indexOf(",");
                        if (commaIndex != -1) {
                            lonString = lonString.substring(0, commaIndex);
                        }
                        lonString = lonString.replace("\"", ""); // Remove quotation marks
                        System.out.println("Longitude String: " + lonString);

                        double lon = 0.0;
                        try {
                            lon = Double.parseDouble(lonString);
                            System.out.println("Longitude: " + lon);
                        } catch (NumberFormatException e) {
                            System.out.println("Error parsing longitude string to double:");
                            e.printStackTrace();
                        }
                        
                        // Create the POST request body
                        String postBody = "[out:json];\n" +
                                "(\n" +
                                "  node[\"shop\"=\"supermarket\"](around:5000," + lat + "," + lon + ");\n" +
                                "  way[\"shop\"=\"supermarket\"](around:5000," + lat + "," + lon + ");\n" +
                                "  relation[\"shop\"=\"supermarket\"](around:5000," + lat + "," + lon + ");\n" +
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

                        // Define a new context for the server
                        server.createContext("/get-supermarkets", new HttpHandler() {
                        // Define the handle method for incoming requests
                        @Override
                        public void handle(HttpExchange exchange) throws IOException {
                            // Check if the request method is GET
                            if ("GET".equals(exchange.getRequestMethod())) {
                                // Set the CORS headers for the request
                                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET");
                                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                                // Define the response string
                                String response = postResult.toString();

                                // Calculate the length of the response string in bytes, not characters
                                int contentLength = response.getBytes(StandardCharsets.UTF_8).length;

                                // Send the response headers
                                exchange.sendResponseHeaders(200, contentLength);
                                // Get the response body as an OutputStream
                                OutputStream os = exchange.getResponseBody();
                                // Write the response string to the OutputStream
                                os.write(response.getBytes());
                                // Close the OutputStream
                                os.close();
                            } else {
                                // If the request method is not GET, send a 405 Method Not Allowed response
                                exchange.sendResponseHeaders(405, -1);
                            }
                        }
                        });

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

        // Start the server
        server.start();
    }
}