package dns.db;

import java.util.HashMap;
import java.util.Map;

/**
 * All of the entries for the DNS server are stored in this DNSDatabase object.
 *
 */
public class DNSDatabase {

	private Map<String, DBEntry> db;

	public DNSDatabase() {
		db = new HashMap<>();
	}

	public DBEntry addEntry(String url, String type, String entry) {
		return db.put(url, new DBEntry(url, type, entry));
	}

	public DBEntry findEntry(String url) {
		return db.getOrDefault(url.toLowerCase(), new DBEntry("", "", ""));
	}

}
