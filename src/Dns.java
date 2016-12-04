import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by Frank on 2016-12-03.
 */
public class Dns {

	private static final int PACKET_SIZE = 1026;
	private final static int REQUEST_TIMEOUT = 3000;

	private static final byte
		DNS_RESPONSE = 1,
		DNS_REQUEST = 2;
	private final static String
		DNS_TYPE_A = "A",
		DNS_TYPE_CNAME = "CNAME",
		DNS_TYPE_NS = "NS",
		DNS_TYPE_NONE = "NONE";

	private final int PORT;

	private HashMap<String, HashMap<String, String>> records = new HashMap<>();
	private HashMap<String, String> requests = new HashMap<>();

	Dns(int port, HashMap<String, HashMap<String, String>> records) {
		PORT = port;
		this.records = records;
	}

	Dns(int port, String filename) throws FileNotFoundException {
		PORT = port;
		records = readRecordsFromFile(filename);
		System.out.println("Loaded files: " + records);
	}
	private HashMap<String, HashMap<String, String>> readRecordsFromFile(String filename) throws FileNotFoundException {
		HashMap<String, HashMap<String, String>> records = new HashMap<>();
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

	void run(OutputStream out) {
		PrintWriter writer = new PrintWriter(out);
		DatagramSocket socket;

		try {
			socket = new DatagramSocket(PORT);
			writer.printf("Server started on port: %d\n", PORT);
			writer.flush();
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
				throw new IllegalStateException("ERROR - Could not recieve packet.");
			}

			switch (BUFF[0]) {
				case DNS_REQUEST:
					System.out.printf("Got a request for: %s\n", parseUrl(BUFF));
					handleRequest(parseUrl(BUFF), receivedPacket, socket);
					break;
				case DNS_RESPONSE:
					System.out.printf("Got a response for: %s\n", parseUrl(BUFF));
					handleResponse(parseUrl(BUFF), parseValue(BUFF), socket);
					break;

			}
			Arrays.fill(BUFF, (byte) 0);
		}
	}

	private void handleRequest(String requestedUrl, DatagramPacket requestPacket, DatagramSocket socket) {
		System.out.println("HandleRequest looking up: " + requestedUrl);
		String requestIp = requestPacket.getAddress().toString();
		if (requestIp.startsWith("/")) requestIp = requestIp.substring(1);
		processRequest(
			requestedUrl,
			String.format("%s:%s:%d", requestedUrl, requestIp, requestPacket.getPort()),
			socket
		);
	}

	private void processRequest(String requestedUrl, String requestRecord, DatagramSocket socket) {
		String[] result = localUrlLookup(requestedUrl);

		final String[] requestRecordTokens = requestRecord.split(":");
		final String
			originalUrlRequest = requestRecordTokens[0],
			requestIp = requestRecordTokens[1];
		final int requestPort = Integer.parseInt(requestRecordTokens[2]);

		if (result == null) {
			sendResponse(originalUrlRequest, DNS_TYPE_NONE, DNS_TYPE_NONE, requestIp, requestPort, socket);
			return;
		}

		switch (result[0]) {
			case DNS_TYPE_A:
				System.out.printf("%s -A-> %s\n", requestedUrl, result[1]);
				sendResponse(originalUrlRequest, DNS_TYPE_A, result[1], requestIp, requestPort, socket);
				break;
			case DNS_TYPE_CNAME:
				System.out.printf("%s -CNAME-> %s\n", requestedUrl, result[1]);
				if (localUrlLookup(result[1]) != null)
					processRequest(result[1], requestRecord, socket);
				else
					sendResponse(originalUrlRequest, DNS_TYPE_CNAME, result[1], requestIp, requestPort, socket);
				break;
			case DNS_TYPE_NS:
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

	private void handleResponse(String requestedUrl, String responseValue, DatagramSocket socket) {
		String requestRecord = requests.remove(requestedUrl);
		if (requestRecord == null) return;
		final String[] requestRecordTokens = requestRecord.split(":");
		final String
			originalUrlRequest = requestRecordTokens[0],
			requestIp = requestRecordTokens[1];
		final int requestPort = Integer.parseInt(requestRecordTokens[2]);

		switch (responseValue) {
			case DNS_TYPE_A:
				sendResponse(originalUrlRequest, DNS_TYPE_A, responseValue, requestIp, requestPort, socket);
				break;
			default:
				processRequest(responseValue, requestRecord, socket);
				break;
		}
	}

	private String[] localUrlLookup(String url) {
		String ns = getNsDomain(url);
		if (records.get(DNS_TYPE_A).containsKey(url)) {
			return new String[]{DNS_TYPE_A, records.get(DNS_TYPE_A).get(url)};
		} else if (records.get(DNS_TYPE_CNAME).containsKey(url)) {
			return new String[]{DNS_TYPE_CNAME, records.get(DNS_TYPE_CNAME).get(url)};
		} else if (ns != null) {
			return new String[]{DNS_TYPE_NS, ns};
		} else return null;
	}

	private String getNsDomain(String domain) {
		if (records.get(DNS_TYPE_NS).containsKey(domain))
			return records.get(DNS_TYPE_NS).get(domain);

		String root = domain.substring(domain.indexOf('.') + 1);
		if (records.get(DNS_TYPE_NS).containsKey(root))
			return records.get(DNS_TYPE_NS).get(root);
		return null;
	}

	private void sendRequest(String url, String destIp, int destPort, DatagramSocket socket) {
		try {
			socket.send(createRequest(url, destIp, destPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendResponse(String url, String type, String value, String destIp, int destPort, DatagramSocket socket) {
		try {
			socket.send(createResponse(url, type, value, destIp, destPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static DatagramPacket createRequest(String url, String destIp, int destPort) throws UnknownHostException {
		if (destIp.contains(":")) {
			String[] tokens = destIp.split(":");
			destIp = tokens[0];
			destPort = Integer.parseInt(tokens[1]);
		}
		byte[] buff = String.format("\2%s", url).getBytes();
		return new DatagramPacket(buff, buff.length, InetAddress.getByName(destIp), destPort);
	}

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

	private static String parseUrl(byte[] data) {
		int e = findNIndex(data, (byte) ' ', 0);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, 1, e)).trim();
	}

	private static String parseType(byte[] data) {
		int
			s = findNIndex(data, (byte) ' ', 0),
			e = findNIndex(data, (byte) ' ', 1);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, s + 1, e)).trim();
	}

	private static String parseValue(byte[] data) {
		int
			s = findNIndex(data, (byte) ' ', 1),
			e = findNIndex(data, (byte) ' ', 2);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, s + 1, e)).trim();
	}

	private static int findNIndex(byte[] values, byte searchFor, int nth) {
		for (int i = 0; i < values.length; i++)
			if (values[i] == searchFor) {
				if (nth <= 0) return i;
				else nth--;
			}
		return -1;
	}

	static String[] request(String url, int myDnsPort, String destIp, int destPort) throws IOException {
		DatagramSocket socket = new DatagramSocket(myDnsPort);
		socket.setSoTimeout(REQUEST_TIMEOUT);
		socket.send(createRequest(url, destIp, destPort));

		final byte[] BUFF = new byte[PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(BUFF, BUFF.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket.close();


		// TODO: handle null responces
		return new String[]{parseType(BUFF), parseValue(BUFF)};
	}

}
