package dns.protocol;

import java.net.InetAddress;

/**
 * The DNSRequest object helps model how data is sent in this mock DNS protocol.
 *
 * Helps keep things consistent between parts of the application.
 */
public class DNSRequest extends DNSType {

	private final String url;
	private final InetAddress senderIP;
	private final int senderPort;

	public DNSRequest(String url, InetAddress senderIP, int senderPort) {
		this.url = url;
		this.senderIP = senderIP;
		this.senderPort = senderPort;
	}

	public DNSRequest(String[] requestData, InetAddress senderIP, int senderPort) {
		this.url = requestData[1];
		this.senderIP = senderIP;
		this.senderPort = senderPort;
	}

	public String getUrl() {
		return this.url;
	}

	public InetAddress getSenderIP() {
		return this.senderIP;
	}

	public int getSenderPort() {
		return this.senderPort;
	}

	public byte[] packetFormattedRequest() {
		String response = "request " + url;
		return response.getBytes();
	}

}
