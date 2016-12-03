package dns;

import dns.db.DBEntry;
import dns.db.DNSDatabase;
import dns.protocol.DNSRequest;
import dns.protocol.DNSResponse;

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

	private final int DNS_MALFORMED = -1;
	private final int DNS_REQUEST = 0;
	private final int DNS_RESPONSE = 1;

	private int port;
	private DNSDatabase db;
	private DatagramSocket datagramSocket;

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
		datagramSocket = new DatagramSocket(this.port);
		byte[] receiveData;
		System.out.println("DNS datagram socket opened successfully, listening on port " + this.port);

		while(true) {
			receiveData = new byte[PACKET_DATA_LENGTH];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				datagramSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}

			this.handleReceivedPacket(receivePacket);

		}
	}

	private void handleReceivedPacket(DatagramPacket receivePacket) {
		System.out.print("Packet received containing data -> ");
		String[] data = this.parsedDataFrom(receivePacket.getData());
		try {
			switch (this.determinedReceivedPacketTypeFrom(data)) {
				case DNS_REQUEST:
					System.out.println(" -> recognized as dns request, handling...");
					this.handleRequest(new DNSRequest(data, receivePacket.getAddress(), receivePacket.getPort()));
					break;
				case DNS_RESPONSE:
					System.out.println(" -> recognized as dns response, handling...");
					this.handleResponse(new DNSResponse(data));
					break;
				default:
					System.out.println("Malformed DNS recognized, doing nothing.");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private String[] parsedDataFrom(byte[] dataArray) {
		String trimmedString = new String(dataArray).trim();
		System.out.print(trimmedString);
		return trimmedString.split(" ");
	}

	private int determinedReceivedPacketTypeFrom(String[] data) {
		if(data == null || data.length < 2) {
			return DNS_MALFORMED;
		}

		switch (data[0]) {
			case "request":
				return DNS_REQUEST;
			case "response":
				return DNS_RESPONSE;
			default:
				return DNS_MALFORMED;
		}
	}

	private void handleRequest(DNSRequest request) {
		DBEntry dnsRecord = db.findEntry(request.getUrl());
		if(dnsRecord != null) {
			this.respondToRequest(request, dnsRecord);
			return;
		} else {
		}
	}

	private void respondToRequest(DNSRequest request, DBEntry withEntry) {
		DNSResponse dnsResponse = new DNSResponse(withEntry);
		byte[] dnsResponseData = dnsResponse.packetFormattedResponse();
		DatagramPacket responsePacket = new DatagramPacket(
				dnsResponseData,
				dnsResponseData.length,
				request.getSenderIP(),
				request.getSenderPort());
		try {
			this.datagramSocket.send(responsePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleResponse(DNSResponse response) {
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
