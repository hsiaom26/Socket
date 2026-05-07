package Echo;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/* create a EchoServer class*/
public class EchoServer {

	/* socket parameters */
	private static final int PORT = 9527;
	private static final String HOST = "127.0.0.1"

	public static void main(String[] args) {
		
		try {
		
			System.out.println("Waiting for clients...");
			
			/* Create a serversocket instance. */
			ServerSocket serverSocket = new ServerSocket();
			
			/* Let the OS bind the created ServerSocket by using user-specified parameters. */
			/* The 1st parameter is the binded IP, and the 2nd parameter is the binded port number. */
			/* In java, there is no need to call listen(). */
			serverSocket.bind(new InetSocketAddress(HOST, PORT));
			System.out.println("Server binded to " + HOST + ":" + PORT);
			
			/* Sever waits for accepting a request from a client. */
			Socket clientSocket = serverSocket.accept();
			System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
			
			/* InputStreamReader gets the input byte stream from the socket, */
			/* and store the data in a BufferReader for latter reading. */
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));			
			/* The program could read a line from the socket buffer. */
			String str = in.readLine();
			
			/* You can implement whatever you want after receiving a message from the client. */
			/* In this example, we echo the message back to the client. */
			if(str != null) {
				/* Show the message sent from the client. */
				System.out.println("Received from client " + clientSocket.getInetAddress().getHostAddress() + ": " + str);
				
				/* In order to send message back to the client,
				/* we setup a Printwriter and connect it to the clientSocket's getOutputStream().
				/* The second parameter of PrintWriter is 'autoFlush'. */
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				/* Now, we send some string back to the client via the socket. */
				System.out.println("Server echoed back to client " + clientSocket.getInetAddress().getHostAddress() + ": " + str);
			}
		} 
		/* Exception handeling. */
		catch (IOException e) {
			System.err.println("I/O error: " + e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println("Server error: " + e.getMessage());
			e.printStackTrace();
		} 
		finally {
			System.out.println("Server is closing...");
		}

	}

}