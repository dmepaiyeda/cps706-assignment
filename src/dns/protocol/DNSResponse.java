package dns.protocol;

import dns.db.DBEntry;

/**
 * Like the DNSRequest class, the DNSResponse object is created to provide cohesion between different parts
 * of the application when dealing with the mock dns protocol.
 */
public class DNSResponse extends DNSType {

	private final String type;
	private final String url;
	private final String entry;

	public static DNSResponse NoneDNSResponse(String url) {
		return new DNSResponse(url);
	}

	private DNSResponse(String url) {
		this.type = "NONE";
		this.url = url;
		this.entry = "NONE";
	}

	public DNSResponse(String type, String url, String entry) {
		this.type = type;
		this.url = url;
		this.entry = entry;
	}

	public DNSResponse(String[] data) throws Exception {
		if(data.length < 4) {
			throw new Exception("Given data is malformed");
		}
		this.url = data[1].toLowerCase();
		this.type = data[2].toUpperCase();
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

	public String getType() {
		return this.type;
	}

	public String getUrl() {
		return this.url;
	}

	public String getEntry() {
		return this.getEntry();
	}
}
