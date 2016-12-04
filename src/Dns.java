import org.omg.CORBA.TIMEOUT;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by Frank on 2016-12-03.
 */
public class Dns {

	static final int PACKET_SIZE = 1026;
	final static int REQUEST_TIMEOUT = 3000;
	static final byte
		DNS_RESPONSE = 1,
		DNS_REQUEST = 2;
	final static String
		DNS_TYPE_A = "A",
		DNS_TYPE_CNAME = "CNAME",
		DNS_TYPE_NS = "NS",
		DNS_TYPE_NONE = "NONE";

	final int PORT;

	HashMap<String, HashMap<String, String>> records = new HashMap<>();
	HashMap<String, String> requests = new HashMap<>();


	public Dns(int port, HashMap<String, HashMap<String, String>> records) {
		PORT = port;
		this.records = records;
	}

	public Dns(int port, String filename) throws FileNotFoundException {
		PORT = port;
		records = readRecordsFromFile(filename);
		System.out.println("Loaded files: " + records);
	}

	HashMap<String, HashMap<String, String>> readRecordsFromFile(String filename) throws FileNotFoundException {
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

	public void run(OutputStream out) {
		PrintWriter writer = new PrintWriter(out);
		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket(PORT);
			writer.printf("Server started on port: %d\n", PORT); writer.flush();
		} catch (SocketException e) {
			e.printStackTrace();
			// TODO handle exception
		}

		final byte[] BUFF = new byte[PACKET_SIZE];
		while (true) {
			DatagramPacket receivedPacket = new DatagramPacket(BUFF, BUFF.length);
			try {
				socket.receive(receivedPacket);
			} catch (IOException e) {
				e.printStackTrace();
				// TODO handle exception
				continue;
			}

			switch (BUFF[0]) {
				case DNS_REQUEST:
					System.out.printf("Got request for: %s\n", parseUrl(BUFF));
					handleRequest(parseUrl(BUFF), receivedPacket, socket);
					break;
				case DNS_RESPONSE:
					System.out.printf("Got response for: %s\n", parseUrl(BUFF));
					handleResponse(parseUrl(BUFF), parseType(BUFF), parseValue(BUFF), socket);
					break;

			}
			Arrays.fill(BUFF, (byte) 0);
		}
	}

	void handleRequest(String requestedUrl, DatagramPacket requestPacket, DatagramSocket socket) {
		System.out.println("HandleRequest looking up: " + requestedUrl);
		String requestIp = requestPacket.getAddress().toString();
		if (requestIp.startsWith("/")) requestIp = requestIp.substring(1);
		handleRequest(
			requestedUrl,
			String.format("%s:%s:%d", requestedUrl, requestIp, requestPacket.getPort()),
			socket
		);
	}

	void handleRequest(String requestedUrl, String requestRecord, DatagramSocket socket) {
		String[] result = localUrlLookup(requestedUrl);

		final String[] requestRecordTokens = requestRecord.split(":");
		final String
			originalUrlRequest = requestRecordTokens[0],
			requestIp = requestRecordTokens[1];
		final int requestPort = Integer.parseInt(requestRecordTokens[2]);

		if (result==null) {
			try {
				socket.send(createResponse(originalUrlRequest, DNS_TYPE_NONE, DNS_TYPE_NONE, requestIp, requestPort));
			} catch (IOException e) {
				e.printStackTrace();
				// TODO: make it graceful
			}
			return;
		}

		switch (result[0]) {
			case DNS_TYPE_A:
				// We have the ip!!
				System.out.printf("%s -A-> %s\n", requestedUrl, result[1]);
				try {
					socket.send(createResponse(originalUrlRequest, DNS_TYPE_A, result[1], requestIp, requestPort));
				} catch (IOException e) {
					e.printStackTrace();
					// TODO: make it graceful
				};
				break;
			case DNS_TYPE_CNAME:
				System.out.printf("%s -CNAME-> %s\n", requestedUrl, result[1]);
				if (localUrlLookup(result[1]) != null)
					handleRequest(result[1], requestRecord, socket);
				else
					try {
						socket.send(createResponse(originalUrlRequest, DNS_TYPE_CNAME, result[1], requestIp, requestPort));
					} catch (IOException e) {
						e.printStackTrace();
						// TODO: make it graceful
					};
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
				try {
					socket.send(createRequest(requestedUrl, nsIp, nsPort));
				} catch (IOException e) {
					e.printStackTrace();
					// TODO: make it graceful
				}
				break;
		}
	}

	void handleResponse(String requestedUrl, String responseType, String responseValue, DatagramSocket socket) {
		String requestRecord = requests.remove(requestedUrl);
		if (requestRecord == null) return;
		final String[] requestRecordTokens = requestRecord.split(":");
				final String
					originalUrlRequest = requestRecordTokens[0],
					requestIp = requestRecordTokens[1];
				final int requestPort = Integer.parseInt(requestRecordTokens[2]);
				
				switch(responseValue) {
				case DNS_TYPE_A:
					try {
						socket.send(createResponse(originalUrlRequest, DNS_TYPE_A, responseValue, requestIp, requestPort));
					} catch (IOException e) {
						e.printStackTrace();
						// TODO handle this with grace
					}
					break;
				default:
					handleRequest(responseValue, requestRecord, socket);
					break;
				}
			/*	
		switch (responseType) {
			case DNS_TYPE_A:
				try {
					socket.send(createResponse(originalUrlRequest, DNS_TYPE_A, responseValue, requestIp, requestPort));
				} catch (IOException e) {
					e.printStackTrace();
					// TODO handle this with grace
				}
				break;
			case DNS_TYPE_CNAME:
				String[] result = localUrlLookup(responseValue);
				if (result != null) {
					switch(result[0]) {
					case DNS_TYPE_A:
						break;
					case DNS_TYPE_CNAME:
						break;
					case DNS_TYPE_NS:
						break;
					}
					System.out.println("recurse "+Arrays.toString(result));

					requests.put(requestedUrl, requestRecord);
					if (result[0].equals(DNS_TYPE_NS)) {
						
						String nsIp = result[1];
						int nsPort = PORT;
						if (nsIp.contains(":")) {
							String[] nsTokens = nsIp.split(":");
							nsIp = nsTokens[0];
							nsPort = Integer.parseInt(nsTokens[1]);
						}
						requests.put(requestedUrl, requestRecord);
						System.out.printf("%s -NS-> %s\n", requestedUrl, result[1]);
						
						
						try {
							socket.send(createRequest(responseValue, nsIp, nsPort));
						} catch (IOException e) {
							e.printStackTrace();
							// TODO: make it graceful
						}
					} else {
						handleResponse(result[1], result[0], requestRecord, socket);
					}
				} else {
								System.out.println("sendback loool");

					try {
						socket.send(createResponse(originalUrlRequest, DNS_TYPE_CNAME, responseValue, requestIp, requestPort));
					} catch (IOException e) {
						e.printStackTrace();
						// TODO handle this with grace
					}
				}
				break;
		}
		*/
		System.out.printf("Recieved %s (%s)\n", responseValue, responseType);
	}

	String[] localUrlLookup(String url) {
		String ns = getNsDomain(url);
		if (records.get(DNS_TYPE_A).containsKey(url)) {
			return new String[]{DNS_TYPE_A, records.get(DNS_TYPE_A).get(url)};
		}else if (records.get(DNS_TYPE_CNAME).containsKey(url)) {
			return new String[]{DNS_TYPE_CNAME, records.get(DNS_TYPE_CNAME).get(url)};
		} else if (ns != null) {
			return new String[]{DNS_TYPE_NS, ns};
		}
		else return null;
	}

	String getNsDomain(String domain) {
		if (records.get(DNS_TYPE_NS).containsKey(domain))
			return records.get(DNS_TYPE_NS).get(domain);

		String root = domain.substring(domain.indexOf('.')+1);
		if (records.get(DNS_TYPE_NS).containsKey(root))
			return records.get(DNS_TYPE_NS).get(root);
		return null;
	}

	static DatagramPacket createRequest(String url, String destIp, int destPort) throws UnknownHostException {
		if (destIp.contains(":")) {
			String[] tokens = destIp.split(":");
			destIp = tokens[0];
			destPort = Integer.parseInt(tokens[1]);
		}
		byte[] buff = String.format("\2%s", url).getBytes();
		DatagramPacket request = new DatagramPacket(buff, buff.length, InetAddress.getByName(destIp), destPort);
		return request;
	}

	static DatagramPacket createResponse(String url, String type, String value, String destIp, int destPort) throws UnknownHostException {
		if (destIp.contains(":")) {
			String[] tokens = destIp.split(":");
			destIp = tokens[0];
			destPort = Integer.parseInt(tokens[1]);
		}
		byte[] buff = String.format("\1%s %s %s", url, type, value).getBytes();
		if (destIp.startsWith("/")) destIp = destIp.substring(1);
		InetAddress addr = InetAddress.getByName(destIp);
		DatagramPacket request = new DatagramPacket(buff, buff.length, addr, destPort);
		return request;
	}

	static String parseUrl(byte[] data) {
		int e = findNIndex(data, (byte)' ', 0);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, 1, e)).trim();
	}

	static String parseType(byte[] data) {
		int
			s = findNIndex(data, (byte)' ', 0),
			e = findNIndex(data, (byte)' ', 1);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, s+1, e)).trim();
	}

	static String parseValue(byte[] data) {
		int
			s = findNIndex(data, (byte)' ', 1),
			e = findNIndex(data, (byte)' ', 2);
		if (e == -1) e = data.length;
		return new String(Arrays.copyOfRange(data, s+1, e)).trim();
	}

	static int findNIndex(byte[] values, byte searchFor, int nth) {
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
