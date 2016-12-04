import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * A DNS server. Initial records are set via the constructor.
 * <br />
 * Supported record types:
 * <ul>
 *     <li>A</li>
 *     <li>V</li>
 *     <li>CNAME</li>
 *     <li>NS</li>
 * </ul>
 * Created by Frank on 2016-12-03.
 */
public class Dns {

	// A bit overkill, but ehh
	private static final int PACKET_SIZE = 1026;
	private final static int REQUEST_TIMEOUT = 3000;

	/** Protocol determining if the packet sent was a resonce or a request. */
	private static final byte
		DNS_RESPONSE = 1,
		DNS_REQUEST = 2;

	/** Record types. */
	protected final static String
		DNS_TYPE_V = "V",
		DNS_TYPE_A = "A",
		DNS_TYPE_CNAME = "CNAME",
		DNS_TYPE_NS = "NS",
		DNS_TYPE_NONE = "NONE";

	/** Set port for entire lifetime of the server instance. */
	private final int PORT;

	private HashMap<String, HashMap<String, String>> records = new HashMap<>();
	private HashMap<String, String> requests = new HashMap<>();

	/**
	 * Creates a new DNS server, initializing records from a hash-map.
	 * @param port Port to listen and send on.
	 * @param records Records to initilize with.
	 */
	Dns(int port, HashMap<String, HashMap<String, String>> records) {
		PORT = port;
		this.records = records;
	}

	/**
	 * Creates a new DNS server where the initial values are loaded from a file.
	 * @param port Port to send and recieve on for the entire lifetime of instance.
	 * @param filename Filename of the file to initialize records from. See README.md for format.
	 * @throws FileNotFoundException Throws if the file provided does not exist.
	 */
	Dns(int port, String filename) throws FileNotFoundException {
		PORT = port;
		records = readRecordsFromFile(filename);
		System.out.println("Loaded files: " + records);
	}

	/**
	 * Reads a hashmap representing loaded records from a file.
	 * @param filename Filename to load from.
	 * @return A HashMap populated records.
	 * @throws FileNotFoundException Throws if it cannot find the file specified.
	 */
	private HashMap<String, HashMap<String, String>> readRecordsFromFile(String filename) throws FileNotFoundException {
		HashMap<String, HashMap<String, String>> records = new HashMap<>();
		records.put(DNS_TYPE_V, new HashMap<>());
		records.put(DNS_TYPE_A, new HashMap<>());
		records.put(DNS_TYPE_CNAME, new HashMap<>());
		records.put(DNS_TYPE_NS, new HashMap<>());

		Scanner scanner = new Scanner(new File(filename));
		while (scanner.hasNext()) {
			String
				key = scanner.next(),
				type = scanner.next(),
				value = scanner.next();
			if (records.containsKey(type))
				records.get(type).put(key, value);
		}
		scanner.close();
		return records;
	}

	/**
	 * Starts up the dns server, and keeps it in an endless, blocking loop. During this time, it will handle all inbound requests and responces.
	 */
	void run() {
		DatagramSocket socket;

		try { // Init socket
			socket = new DatagramSocket(PORT);
			System.out.printf("Server started on port: %d\n", PORT);
			System.out.flush();
		} catch (SocketException e) {
			throw new IllegalStateException("ERROR - Could not open socket.");
		}

		final byte[] BUFF = new byte[PACKET_SIZE];
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(BUFF, BUFF.length);
			try {
				socket.receive(receivedPacket);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException("ERROR - Could not receive packet.");
			}

			switch (BUFF[0]) {
				case DNS_REQUEST:
					System.out.printf("\nGot a request for: %s from: %s\n", parseUrl(BUFF), receivedPacket.getAddress().toString());
					handleRequest(parseUrl(BUFF), receivedPacket, socket);
					break;
				case DNS_RESPONSE:
					System.out.printf("\nGot a response for: %s from: %s\n", parseUrl(BUFF), receivedPacket.getAddress().toString());
					handleResponse(parseUrl(BUFF), parseType(BUFF), parseValue(BUFF), socket);
					break;

			}
			Arrays.fill(BUFF, (byte) 0);
		}
	}

	/**
	 * Handles a request recived from a client.
	 * @param requestedUrl The url requested.
	 * @param requestPacket The packet of the request.
	 * @param socket The server socket.
	 */
	private void handleRequest(String requestedUrl, DatagramPacket requestPacket, DatagramSocket socket) {
		String requestIp = requestPacket.getAddress().toString();
		if (requestIp.startsWith("/")) requestIp = requestIp.substring(1);
		processRequest(
			requestedUrl,
			// Here we serialize the request data, so we can easily store it if we need to wait for another response
			// 			format:   originalRequestedUrl:sourceIp:sourcePort
			String.format("%s:%s:%d", requestedUrl, requestIp, requestPacket.getPort()),
			socket
		);
	}

	/**
	 * Recursively resolves and responds to requested urls. If it reaches an NS record, it will save the request data
	 * to be resumed when the NS query has returned.
	 * @param requestedUrl Url currently being looked for.
	 * @param requestRecord Original request information.
	 * @param socket The socket to send responses to requests and send/receive NS queries.
	 */
	private void processRequest(String requestedUrl, String requestRecord, DatagramSocket socket) {
		String[] result = localUrlLookup(requestedUrl);

		// Parse the request record (from the format:   originalRequestedUrl:sourceIp:sourcePort
		final String[] requestRecordTokens = requestRecord.split(":");
		final String
			originalUrlRequest = requestRecordTokens[0],
			requestIp = requestRecordTokens[1];
		final int requestPort = Integer.parseInt(requestRecordTokens[2]);

		if (result == null) {
			// If the url cannot be resolved, send a null responce so the client does not need to timeout
			sendResponse(originalUrlRequest, DNS_TYPE_NONE, DNS_TYPE_NONE, requestIp, requestPort, socket);
			return;
		}

		switch (result[0]) {
			case DNS_TYPE_A:
				System.out.printf("%s -A-> %s\n", requestedUrl, result[1]);
				sendResponse(originalUrlRequest, result[0], result[1], requestIp, requestPort, socket);
				break;
			case DNS_TYPE_V:
			case DNS_TYPE_CNAME:
				System.out.printf("%s -CNAME-> %s\n", requestedUrl, result[1]);
				if (localUrlLookup(result[1]) != null)
					processRequest(result[1], requestRecord, socket);
				else
					sendResponse(originalUrlRequest, result[0], result[1], requestIp, requestPort, socket);
				break;
			case DNS_TYPE_NS:
				System.out.printf("%s -NS-> %s\n", requestedUrl, result[1]);
				String nsIp = result[1];
				int nsPort = PORT;
				if (nsIp.contains(":")) {
					String[] nsTokens = nsIp.split(":");
					nsIp = nsTokens[0];
					nsPort = Integer.parseInt(nsTokens[1]);
				}
				requests.put(requestedUrl, requestRecord);
				System.out.printf("%s -NS-> %s\n", requestedUrl, result[1]);
				sendRequest(requestedUrl, nsIp, nsPort, socket);
				break;
		}
	}

	/**
	 * Handles responses. Resumes resolving for original request if an A record was not found.
	 * @param requestedUrl Url that was requested to be found.
	 * @param responseType Type of the record found.
	 * @param responseValue Value of the record found.
	 * @param socket Socket to send/receive requests/responses on.
	 */
	private void handleResponse(String requestedUrl, String responseType, String responseValue, DatagramSocket socket) {
		// Get the original request that triggered the NS query
		String requestRecord = requests.remove(requestedUrl);
		if (requestRecord == null) return; // Dont bother continuing if there is no record of a request to resolve

		final String[] requestRecordTokens = requestRecord.split(":");
		final String
			originalUrlRequest = requestRecordTokens[0],
			requestIp = requestRecordTokens[1];
		final int requestPort = Integer.parseInt(requestRecordTokens[2]);

		switch (responseType) {
			case DNS_TYPE_A:
				// If its an A record, just send it back to the client right away.
				sendResponse(originalUrlRequest, responseValue, responseValue, requestIp, requestPort, socket);
				break;
			default:
				// For any other kind of record responce, resume resolving
				processRequest(responseValue, requestRecord, socket);
				break;
		}
	}

	/**
	 * Iteratively find the most accurate record type possible for a url.
	 * @param url The url to resolve.
	 * @return An array consisting of {recordType, recordValue}. Null if no record could be found.
	 */
	private String[] localUrlLookup(String url) {
		String ns = getNsDomain(url);
		if (records.get(DNS_TYPE_A).containsKey(url)) {
			return new String[]{DNS_TYPE_A, records.get(DNS_TYPE_A).get(url)};
		} else if (records.get(DNS_TYPE_V).containsKey(url)) {
				return new String[]{DNS_TYPE_V, records.get(DNS_TYPE_V).get(url)};
		} else if (records.get(DNS_TYPE_CNAME).containsKey(url)) {
			return new String[]{DNS_TYPE_CNAME, records.get(DNS_TYPE_CNAME).get(url)};
		} else if (ns != null) {
			return new String[]{DNS_TYPE_NS, ns};
		} else return null;
	}

	/**
	 * Gets the most specific NS record value that applies to the requested url.
	 * @param domain The url to find a NS server for.
	 * @return NameServer value of one can be found. False otherwise.
	 */
	private String getNsDomain(String domain) {
		// If the exact url exists as an NS record, return the value.
		if (records.get(DNS_TYPE_NS).containsKey(domain))
			return records.get(DNS_TYPE_NS).get(domain);

		// If the first word before a '.' can be removed and has a matching NS record, return the record value.
		String root = domain.substring(domain.indexOf('.') + 1);
		if (records.get(DNS_TYPE_NS).containsKey(root))
			return records.get(DNS_TYPE_NS).get(root);
		return null;
	}

	/**
	 * Helper method for sending dns requests in our protocol.
	 * @param url The url to request.
	 * @param destIp The destination ip.
	 * @param destPort The destination port.
	 * @param socket The destination socket.
	 */
	private void sendRequest(String url, String destIp, int destPort, DatagramSocket socket) {
		System.out.printf("Sending request for: %s to: %s\n", url, destIp);
		try {
			socket.send(createRequest(url, destIp, destPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method for sending a dns response.
	 * @param url The requested url.
	 * @param type The found record type.
	 * @param value The found record value.
	 * @param destIp The destination ip (probably to the ip that send the request).
	 * @param destPort The destination port (probably to the same port that sent the request).
	 * @param socket The socket to send the response over.
	 */
	private void sendResponse(String url, String type, String value, String destIp, int destPort, DatagramSocket socket) {
		System.out.printf("Sending response record: (%s, %s, %s) to: %s\n", url, value, type, destIp);
		try {
			socket.send(createResponse(url, type, value, destIp, destPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method for creating a request packet.
	 * @param url Requested url.
	 * @param destIp Destination ip.
	 * @param destPort Destination port.
	 * @return Configured DatagramPacket ready to send through a socket.
	 * @throws UnknownHostException Throws if the destIp is not in a valid format.
	 */
	private static DatagramPacket createRequest(String url, String destIp, int destPort) throws UnknownHostException {
		if (destIp.contains(":")) {
			String[] tokens = destIp.split(":");
			destIp = tokens[0];
			destPort = Integer.parseInt(tokens[1]);
		}
		byte[] buff = String.format("\2%s", url).getBytes();
		return new DatagramPacket(buff, buff.length, InetAddress.getByName(destIp), destPort);
	}

	/**
	 * Helper method for creating a responce packet.
	 * @param url Requested url.
	 * @param type Found record type.
	 * @param value Found record value.
	 * @param destIp Destination ip.
	 * @param destPort Destination port.
	 * @return A configured DatagramPacket ready to send to a socket.
	 * @throws UnknownHostException Throws if an invalid destIp is passed in.
	 */
	private static DatagramPacket createResponse(String url, String type, String value, String destIp, int destPort) throws UnknownHostException {
		if (destIp.contains(":")) {
			String[] tokens = destIp.split(":");
			destIp = tokens[0];
			destPort = Integer.parseInt(tokens[1]);
		}
		byte[] buff = String.format("\1%s %s %s", url, type, value).getBytes();
		if (destIp.startsWith("/")) destIp = destIp.substring(1);
		InetAddress addr = InetAddress.getByName(destIp);
		return new DatagramPacket(buff, buff.length, addr, destPort);
	}

	/**
	 * Helper method to parse the requested url from a packet.
	 * @param data Data to parse.
	 * @return The requested url.
	 */
	private static String parseUrl(byte[] data) {
		int e = findNIndex(data, (byte) ' ', 0);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, 1, e)).trim();
	}

	/**
	 * Helper method to parse the record type from a response packet.
	 * @param data Data to parse.
	 * @return The type of record found.
	 */
	private static String parseType(byte[] data) {
		int
			s = findNIndex(data, (byte) ' ', 0),
			e = findNIndex(data, (byte) ' ', 1);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, s + 1, e)).trim();
	}

	/**
	 * Helper method to parse the record value from a response packet.
	 * @param data Data to parse.
	 * @return The value of the record found.
	 */
	private static String parseValue(byte[] data) {
		int
			s = findNIndex(data, (byte) ' ', 1),
			e = findNIndex(data, (byte) ' ', 2);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, s + 1, e)).trim();
	}

	/**
	 * Helper method that finds the index of the nth occurrence of a byte, in an array of bytes.
	 * @param values Values to search in.
	 * @param searchFor Byte to search for.
	 * @param nth Number of matching bytes to skip. (0 will return the first occurrence, 1 the second, etc...).
	 * @return The index of the nth matching byte. -1 if not found.
	 */
	private static int findNIndex(byte[] values, byte searchFor, int nth) {
		for (int i = 0; i < values.length; i++)
			if (values[i] == searchFor) {
				if (nth <= 0) return i;
				else nth--;
			}
		return -1;
	}

	/**
	 * Helper method to simplify the dns requesting process for other classes.
	 * @param url Url to resolve.
	 * @param myDnsPort The port for the response.
	 * @param destIp The ip of the dns server.
	 * @param destPort The port of the dns server.
	 * @return The found record in the format { recordType, recordValue }. Null if not found.
	 * @throws IOException Throws on a socket exception, usualy caused by a timeout.
	 */
	static String[] request(String url, int myDnsPort, String destIp, int destPort) throws IOException {
		DatagramSocket socket = new DatagramSocket(myDnsPort); // open a socket
		socket.setSoTimeout(REQUEST_TIMEOUT); // set timeout for receiving
		socket.send(createRequest(url, destIp, destPort)); // send request packet

		final byte[] BUFF = new byte[PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(BUFF, BUFF.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			socket.close();// make sure to clean up
			throw new IllegalStateException("Dns timeout.");
		}
		socket.close();
		return new String[]{parseType(BUFF), parseValue(BUFF)};
	}

}
