import java.io.*;
import java.net.*;

// Class definition for SimpleServer
public class SimpleServer {
    // The main method - entry point of the application
    public static void main(String[] args) throws IOException {
        // Define the port number on which the server will listen
        int port = 8080;
        // Create a ServerSocket that listens on the specified port
        ServerSocket serverSocket = new ServerSocket(port);
        // Print to the console that the server has started
        System.out.println("Server started on port: " + port);

        // Infinite loop to keep the server running
        while (true) {
            try (
                // Accept an incoming connection from a client
                Socket socket = serverSocket.accept();
                // Create a PrintWriter to send data to the client
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // Create a BufferedReader to read data from the client
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                
                String inputLine;
                // Read lines from the client until "null" is received (indicating end of stream)
                while ((inputLine = in.readLine()) != null) {
                    // Print the received line to the console
                    System.out.println("Received: " + inputLine);
                    // Send an echo response back to the client
                    out.println("Echo: " + inputLine);
                    // If the client sends "bye", exit the loop and wait for the next connection
                    if ("bye".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                }
            } catch (IOException e) {
                // Print an error message if an exception occurs in the try block
                System.out.println("Exception caught when trying to listen on port " + port + " or listening for a connection");
                System.out.println(e.getMessage());
            }
        }
    }
}
