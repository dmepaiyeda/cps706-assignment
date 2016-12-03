package dns;

import dns.db.DBEntry;
import dns.db.DNSDatabase;
import sun.net.spi.nameservice.dns.DNSNameService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * The DNS server for this application.
 * Uses a UDP socket to listen for requests, and responds with the requested information.
 *
 * Requests are made in the form [URL] Ex. request hisCinema.com
 * Responses are made in the form [TYPE] [URL] Ex. respond A hisCinema.com
 *
 * Records are stored in the db text file in the form [url] [type] [entry]
 * The full URL is stored for simplicity.
 * So for the record video.hisCinema.com, the authoritative DNS server would store the line:
 *
 * video.hisCinema.com	A	1.2.3.4
 *
 * OR
 *
 * video.hisCinema.com 	CNAME	hercdn.com
 *
 * This DNS server does recursive querying, so if a request is made, and a CNAME record is returned,
 * then it will go ahead and resolved that url as well, and so on.
 */
public class DNS {

	private static final int PACKET_DATA_LENGTH = 1500;

	private int port;
	private DNSDatabase db;

	/**
	 * @param port The port to listen for requests from
	 * @param filename The name of the database file
	 * @throws FileNotFoundException If there was a problem with the given filename
	 */
	public DNS(int port, String filename) throws FileNotFoundException {
		this.port = port;
		this.db = this.populatedDatabaseFromFile(filename);
	}

	/**
	 * @param filename the name of the database file
	 * @return The populated DNSDatabase
	 */
	private DNSDatabase populatedDatabaseFromFile(String filename) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(filename));
		DNSDatabase db = new DNSDatabase();
		while(sc.hasNext()) {
			String line = sc.nextLine();
			StringTokenizer stringTokenizer = new StringTokenizer(line);
			db.addEntry(
				stringTokenizer.nextToken().toLowerCase(),
				stringTokenizer.nextToken().toLowerCase(),
				stringTokenizer.nextToken().toLowerCase());
		}
		return db;
	}

	public void openConnection() throws SocketException {
		DatagramSocket datagramSocket = new DatagramSocket(this.port);
		byte[] receiveData;
		byte[] sendData = new byte[PACKET_DATA_LENGTH];
		System.out.println("DNS datagram socket opened successfully, listening on port " + this.port);

		while(true) {
			receiveData = new byte[PACKET_DATA_LENGTH];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				datagramSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String trimmedRequestString = new String(receivePacket.getData()).trim();
			InetAddress senderIP = receivePacket.getAddress();
			int senderPort = receivePacket.getPort();
			System.out.printf("Received from %s:%d data: %s \n", senderIP, senderPort, trimmedRequestString);

			String responseString = db.findEntry(trimmedRequestString).toDNSResponse();
			resolveURL(trimmedRequestString, datagramSocket);

			DatagramPacket sendPacket = new DatagramPacket(
				responseString.getBytes(),
				responseString.getBytes().length,
				senderIP,
				senderPort);
			try {
				datagramSocket.send(sendPacket);
				System.out.printf("Send to %s:%d data: %s\n", senderIP, senderPort, responseString);
			} catch (IOException e) {
				System.out.println("Error sending packet: " + e.getMessage());
			}
		}
	}

	public String resolveURL(String url, DatagramSocket socket) {
		DBEntry dbEntry = this.db.findEntry(url);
		if(dbEntry != null) { //the url is not in the database
		}
		getHostNameFromString(url);
		return null;
	}

	public String getHostNameFromString(String hostname) {
		System.out.print(hostname + " -> ");
		String[] parts = hostname.split("\\.");
		System.out.println(Arrays.toString(parts));
		if(parts.length > 2) {
			return parts[parts.length-2] + "." + parts[parts.length-1];
		} else if(parts.length == 2) {
			return parts[parts.length-2] + "." + parts[parts.length-1];
		} else {
			return "none none";
		}
	}

}
