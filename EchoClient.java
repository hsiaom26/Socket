package Echo;

import java.io.*;
import java.net.Socket;

/* create a EchoClient class*/
public class EchoClient {

	public static void main(String[] args) {
		
		try {
			/* socket parameters */
			String serverHostName = "127.0.0.1";
			int serverPortNumber = 9527;
			
			System.out.println("Client started.");
			
			/* Creates a Socket and connects it to a specified port number at a specified IP address. */
			Socket socket = new Socket(serverHostName, serverPortNumber);
			
			/* Prepare a BufferedReader that connects to a InputStreamReader using system default input (System.in). */
			/* So that your input will be stored in a buffer for latter use. */
			/* Then, you can input a message in your terminal. We will send this string to the server via the socket. */
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Enter a string");
			String str = userInput.readLine();
						
			/* socket.getOutputStream() is a stream object that we interact with while sending messages to the server. */
			/* We use a Printwriter object to easily access the stream object. */
			/* The second parameter of PrintWriter is 'autoFlush'. */
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			
			/* Send a string to the server via the socket. */
			out.println(str);
			
			/* If there is any message sent back from the server, we use a BufferedReader to catach it. */
			/* The data will be sent from socket.getInputStream(). */
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			/* Show the message sent from the server. */
			System.out.println(in.readLine());

		} 
		/* Exception handeling. */
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}