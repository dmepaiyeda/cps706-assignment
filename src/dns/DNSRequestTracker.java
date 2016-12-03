package dns;

import dns.protocol.DNSRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Since this mock DNS server should handle recursive response, it needs to be able to track responses
 * and continue to make more until it can resolve the IP address.
 *
 * This class is a wrapper around a Map, which will just map URLs to a DNSRequest object, continuing to update
 * the entry until an IP is found, it then packages the response and sends it back to the stored DNSRequest object.
 */
public class DNSRequestTracker {
	private Map<String, DNSRequest> db;

	DNSRequestTracker() {
		db = new HashMap<>();
	}

	/**
	 * When making recursive queries, when a response is received that is not an A record,
	 * for example, a CNAME is received, then the DNS server will make a new request to the
	 * authoritative DNS server for the received url in response.
	 *
	 * When that happens, the old Request has been fulfilled, and a new Request is made and needs to be tracked.
	 *
	 * That's what this method is for.
	 *
	 * @param from the original url key
	 * @param to the url key to change the original request to
	 * @return The DNS request.
	 */
	public DNSRequest updateRequest(String from, String to) {
		DNSRequest dnsRequest = db.remove(from);
		return db.put(to, dnsRequest);
	}

	public DNSRequest addRequest(String url, DNSRequest dnsRequest) {
		return db.put(url, dnsRequest);
	}
}
