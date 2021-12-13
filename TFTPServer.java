import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class TFTPServer {

	private static final int BUFFERSIZE = 512;

	private static final short RRQ = 1;
	private static final short WRQ = 2;
	private static final short DATA = 3;
	private static final short ACK = 4;
	private static final short ERRO = 5;

	public static String mode;

	public static void main(String[] args) {

		try {
			TFTPServer tftpServer = new TFTPServer();
			tftpServer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void start() throws SocketException {

		byte[] buffer = new byte[BUFFERSIZE];

		/* Create socket */
		DatagramSocket serverSocket = new DatagramSocket(null);

		/* Create local bind point, set at IP 127.0.0.1 and port 6969 */
		serverSocket.bind(new InetSocketAddress("127.0.0.1", 6969));

		System.out.printf("Listening at port %d for new requests\n", 6969);

		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			// receive packet from client
			try {
				serverSocket.receive(packet);
			} catch(IOException e) {
				e.printStackTrace();
				break;
			}

			final InetSocketAddress clientSocketAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());

			// parse the file header, retrieve opcode and file name.
			final ByteBuffer bufwrap = ByteBuffer.wrap(buffer);
			final short opcode = bufwrap.getShort(0);
			final String fileName = get(bufwrap, 2, (byte)0);
			final String mode = get(bufwrap, 2 + fileName.length() + 1, (byte) 0);
	
			System.out.printf("Receive connection from %s:%d\n", packet.getAddress(), packet.getPort());
			System.out.printf("opcode=%h, mode=%s, file_name=%s\n", opcode, mode, fileName);
			if (mode.compareTo("octet") == 0) {

				// create new thread to handle connection
				new Thread() {
						public void run() {
							try {
								DatagramSocket clientUDPSocket = new DatagramSocket(null);
								int randomPortNum = ThreadLocalRandom.current().nextInt(49152, 65535 + 1);
								clientUDPSocket.bind(new InetSocketAddress("127.0.0.1", randomPortNum));
								clientUDPSocket.connect(clientSocketAddress);
								switch(opcode) {
									case RRQ:
										download(clientUDPSocket, fileName.toString(), RRQ);
									break;
									case WRQ:
										upload(clientUDPSocket, fileName.toString(), WRQ);
									break;
								}
								clientUDPSocket.close();
							} catch (SocketException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
				}.start();
			} else {
				continue;
			}
		}
		serverSocket.close();
	}

	private String get(ByteBuffer buf, int current, byte del) {
		StringBuffer sb = new StringBuffer();
		while (buf.get(current) != del) {
			sb.append((char) buf.get(current));
			current++;
		}
		return sb.toString();
	}

	private boolean isAck(ByteBuffer buf, short blocknum) {
		short op = buf.getShort();
		short block = buf.getShort(2);
		return (op == (short) ACK && block == blocknum);
	}

	private void download(DatagramSocket sendSocket, String fileName, int opcode) {
		File file = new File(fileName);
		byte[] buffer = new byte[BUFFERSIZE];
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("File not found.");
			sendError(sendSocket, (short) 1, "File not found.");
			return;
		}

		short blockNum = 1;
		int length;

		while (true) {

			try {
				length = in.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			if (length == -1) {
				length = 0;
			}

			DatagramPacket packet = toData(blockNum, buffer, length);
			try {
				sendSocket.send(packet);

				byte[] recvbuf = new byte[BUFFERSIZE];
				DatagramPacket recv = new DatagramPacket(recvbuf, BUFFERSIZE);
				sendSocket.receive(recv);
				ByteBuffer recvbytebuf = ByteBuffer.wrap(recvbuf);
				if (!isAck(recvbytebuf, blockNum)) {
					System.err.println("Error transferring file.");																				
					break;
				}
	
				blockNum += 1;
				if (length < 512) {
					in.close();
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

	private void upload(DatagramSocket sendSocket, String fileName, int opcode) {
		File file = new File(fileName);
		if (file.exists()) {
			System.out.println("File already exists.");
			sendError(sendSocket, (short) 6, "File already exists.");
			return;
		} else {
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				sendError(sendSocket, (short) 1, "File not found.");
				return;
			}

			short blockNum;
			try {

				sendSocket.send(toAck((short) 0));

				while (true) {
					byte[] recvbuf = new byte[BUFFERSIZE + 4];
					DatagramPacket recv = new DatagramPacket(recvbuf, BUFFERSIZE + 4);
					try {
						sendSocket.receive(recv);
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					ByteBuffer recvbytebuffer = ByteBuffer.wrap(recvbuf);
					if (recvbytebuffer.getShort() == (short) 3) {
						blockNum = recvbytebuffer.getShort();
						output.write(recvbuf, 4, recv.getLength() - 4);
						sendSocket.send(toAck(blockNum));
						if (recv.getLength() - 4 < 512) {
							output.close();
							break;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * sendError
	 * 
	 * Sends an error packet to sendSocket
	 * 
	 * @param sendSocket
	 * @param errorCode
	 * @param errMsg
	 */
	private void sendError(DatagramSocket sendSocket, short errorCode, String errMsg) {

		ByteBuffer wrap = ByteBuffer.allocate(BUFFERSIZE);
		wrap.putShort(ERRO);
		wrap.putShort(errorCode);
		wrap.put(errMsg.getBytes());
		wrap.put((byte) 0);

		DatagramPacket receivePacket = new DatagramPacket(wrap.array(), wrap.array().length);
		try {
			sendSocket.send(receivePacket);
		} catch (IOException e) {
			System.err.println("Problem sending error packet.");
			e.printStackTrace();
		}
	}

	/**
	 * ackPacket
	 * 
	 * Constructs an ACK packet for the given block number.
	 * 
	 * @param block the current block number
	 * @return ackPacket
	 */
	private DatagramPacket toAck(short block) {

		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putShort(ACK);
		buffer.putShort(block);

		return new DatagramPacket(buffer.array(), 4);
	}

	/**
	 * dataPacket
	 * 
	 * Constructs an DATA packet
	 * 
	 * @param block  current block number
	 * @param data   data to be sent
	 * @param length length of data
	 * @return DatagramPacket to be sent
	 */
	private DatagramPacket toData(short block, byte[] data, int length) {

		ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE + 4);
		buffer.putShort(DATA);
		buffer.putShort(block);
		buffer.put(data, 0, length);

		return new DatagramPacket(buffer.array(), 4 + length);
	}
}
