package dns.protocol;

import dns.db.DBEntry;

/**
 * Like the DNSRequest class, the DNSResponse object is created to provide cohesion between different parts
 * of the application when dealing with the mock dns protocol.
 */
public class DNSResponse {

	private final String NS_RECORD = "NS";
	private final String CNAME_RECORD = "CNAME";
	private final String A_RECORD = "A";

	private final String type;
	private final String url;
	private final String entry;

	public DNSResponse(String[] data) throws Exception {
		if(data.length < 4) {
			throw new Exception("Given data is malformed");
		}
		this.url = data[1];
		this.type = data[2];
		this.entry = data[3];
	}

	public DNSResponse(DBEntry dbEntry) {
		this.url = dbEntry.getUrl();
		this.type = dbEntry.getType();
		this.entry = dbEntry.getEntry();
	}

	public byte[] packetFormattedResponse() {
		String response = "response " + this.url + " " + this.type + " " + this.entry;
		return response.getBytes();
	}
}
