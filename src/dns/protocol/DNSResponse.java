package dns.protocol;

/**
 * Like the DNSRequest class, the DNSResponse object is created to provide cohesion between different parts
 * of the application when dealing with the mock dns protocol.
 */
public class DNSResponse {

	private final String type;
	private final String url;
	private final String entry;

	public DNSResponse(String type, String url, String entry) {
		this.type = type;
		this.url = url;
		this.entry = entry;
	}
}
