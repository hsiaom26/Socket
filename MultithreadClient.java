import java.io.*;
import java.net.*;

/**
 * Multithreaded Echo Client
 * Connects to MultithreadServer and sends messages, receives echoed responses
 */
public class MultithreadClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            System.out.println("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("Enter messages (type 'quit' to exit):\n");

            // Set up input and output streams
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter serverWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Read from console
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            // Create a separate thread to read responses from server
            // Three different ways to implement the response reader thread are shown below,
            // you can choose one of them by uncommenting it and commenting out the others.

            // 1. method reference version of response reader thread
            /*
            private static class ResponseReader implements Runnable {
                private BufferedReader serverReader;

                public ResponseReader(BufferedReader serverReader) {
                    this.serverReader = serverReader;
                }

                @Override
                public void run() {
                    try {
                        String response;
                        while ((response = serverReader.readLine()) != null) {
                            System.out.println("Echo from server: " + response);
                        }
                        System.out.println("\nServer closed the connection.");
                    } catch (IOException e) {
                        System.err.println("Error reading from server: " + e.getMessage());
                    }
                }
            }
            Thread responseThread = new Thread(new ResponseReader(serverReader));
            */

            // 2. anonymous inner class version
            /* 
            Thread responseThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String response;
                        while ((response = serverReader.readLine()) != null) {
                            System.out.println("Echo from server: " + response);
                        }
                        System.out.println("\nServer closed the connection.");
                    } catch (IOException e) {
                        System.err.println("Error reading from server: " + e.getMessage());
                    }
                }
            });
            */

            // 3. lambda version of response reader thread
            
            Thread responseThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = serverReader.readLine()) != null) {
                        System.out.println("Echo from server: " + response);
                    }
                    System.out.println("\nServer closed the connection.");
                } catch (IOException e) {
                    System.err.println("Error reading from server: " + e.getMessage());
                }
            });
            
            
            // Set as daemon so it will exit when main thread exits
            // If the server closes the connection, this thread will also exit.
            // Otherswise, it will keep running until the user types 'quit' in the main thread.
            responseThread.setDaemon(true);
            responseThread.start();

            // Main thread: read from console and send to server
            String userInput;
            while ((userInput = consoleReader.readLine()) != null) {
                if (userInput.equalsIgnoreCase("quit")) {
                    System.out.println("Closing connection...");
                    break;
                }

                if (!userInput.isEmpty()) {
                    serverWriter.println(userInput);
                    System.out.println("Sent: " + userInput);
                }
            }

        } catch (ConnectException e) {
            System.err.println("Cannot connect to server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.err.println("Make sure the server is running.");
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
