import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Sender {
	
	private DatagramSocket socket;
	private DatagramPacket packetToSend;
	private DatagramPacket packetToReceive;
	private FileInputStream fileIn;
	private byte seqNum = 0;
	private int offset = 1;
	private byte[] sendBuf = new byte[125];
	private byte[] recBuf = new byte[125];

	public Sender(InetAddress ip, int receiverPort, int ackPort, String fileName, int timeout) {
	
		try {
			this.socket = new DatagramSocket(ackPort);
			this.socket.setSoTimeout(timeout);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.packetToSend = new DatagramPacket(sendBuf, offset, sendBuf.length, ip, receiverPort);
		this.packetToReceive = new DatagramPacket(recBuf, offset, recBuf.length);
		
		// Check if the file exists
		try {
			this.fileIn = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startTransfer() {
		int read;
		try {
			while(( read = fileIn.read(this.sendBuf, this.offset, this.sendBuf.length - this.offset) ) != -1) {
				makePacket(sendBuf,read+1);
				sendPacket();
			}
		} catch (EOFException e1) {
			e1.printStackTrace();
			//Fail out and close
			//EOF reached close ok.
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//Fail out
		}
		
		// Grab the file and change into Byte Stream
		// Init the sequence number
		// Send and start timeout
	}
	
	private void makePacket(byte[] data, int dataLength) {
		data[0] = this.seqNum;
		this.packetToSend.setData(data, offset, dataLength);
		this.seqNum ^= 1;
	}
	
	private void sendPacket() {
		try {
			socket.send(this.packetToSend);
			this.socket.receive(packetToReceive);
			byte recSeqNum = recBuf[(packetToReceive.getOffset()-1)];
			if (recSeqNum != seqNum) {
				sendPacket();
			}
			
		} catch (SocketTimeoutException e) {
			sendPacket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String[] args) throws Exception{
		//Get command line arguments
		if(args.length != 5) {
			System.out.println("Invalid Number of args");
			return;
		}
		
		InetAddress ip = null;
		try {
			ip = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e1) {
			System.out.println(e1.getMessage());
			return;
		}
		int receiverPort = Integer.parseInt(args[1]);
		int ackPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		int timeout = Integer.parseInt(args[4]);
		
		Sender mySender = new Sender(ip, receiverPort, ackPort, fileName, timeout);
		mySender.startTransfer();
		
	}
	
}
