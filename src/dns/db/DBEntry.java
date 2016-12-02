package dns.db;

/**
 * Rows in the DNSDatabase will be stored as these objects.
 *
 * All data is stored as strings because this assignment is due in less than 48 hours. LOL
 *
 */
public class DBEntry {

	private final String url;
	private final String type;
	private final String entry;

	/**
	 * @param url The url the entry is for
	 * @param type The DNS type {A, CNAME, MX, V (for this project), etc.}
	 * @param entry The ip, or url to return
	 */
	DBEntry(String url, String type, String entry) {
		this.url = url;
		this.type = type;
		this.entry = entry;
	}

	public String getUrl() {
		return this.url;
	}

	public String getType() {
		return this.type;
	}

	public String getEntry() {
		return this.entry;
	}
}
