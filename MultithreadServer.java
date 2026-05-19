import java.io.*;
import java.net.*;

/**
 * Multithreaded Echo Server
 * Accepts multiple client connections and echoes back their messages
 */
public class MultithreadServer {
    private static final int PORT = 8000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Multithreaded Echo Server started on port " + PORT);
            System.out.println("Waiting for client connections...\n");

            // Accept client connections in a loop
            while (true) {
                // Accept a new client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread to handle this client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Inner class to handle individual client connections
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Set up input and output streams
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                String message;
                System.out.println("Handler started for: " + socket.getInetAddress().getHostAddress());

                // Read messages from client and echo them back
                while ((message = reader.readLine()) != null) {
                    // readLine() is blocking, so it will wait until a message is received, 
                    // or return null if the client disconnects
                    System.out.println("Received from " + socket.getInetAddress().getHostAddress() + ": " + message);
                    
                    // Echo the message back to the client
                    writer.println(message);
                    System.out.println("Echoed to " + socket.getInetAddress().getHostAddress() + ": " + message);
                }

                System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());

            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                try {
                    if (reader != null) reader.close();
                    if (writer != null) writer.close();
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    // Error Handling
                    System.err.println("Error closing resources: " + e.getMessage());
                }
            }
        }
    }
}
