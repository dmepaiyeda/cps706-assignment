package dns;

import dns.db.DNSDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * The DNS server for this application.
 * Uses a UDP socket to listen for requests, and responds with the requested information.
 *
 * Requests are made in the form [URL]
 * Responses are made in the form [TYPE] [URL] (one record per line). Ex. A hisCinema.com
 *
 * Records are stored in the db text file in the form [url] [type] [entry]
 * The full URL is stored for simplicity.
 * So for the record video.hisCinema.com, the authoritative DNS server would store the line:
 *
 * video.hisCinema.com	A	1.2.3.4
 *
 * OR
 *
 * video.hisCinema.com 	CNAME	hercdn.com
 *
 * This DNS server does recursive querying, so if a request is made, and a CNAME record is returned,
 * then it will go ahead and resolved that url as well, and so on.
 */
public class DNS {

	private int port;

	public DNS(int port, String filename) throws FileNotFoundException {
		this.port = port;
		this.verifyFile(filename);

	}

	private void verifyFile(String filename) throws FileNotFoundException {
		File db = new File(filename);
		Scanner sc = new Scanner(db);
		this.populateDatabase(sc);
	}

	private DNSDatabase populateDatabase(Scanner sc) {
		DNSDatabase db = new DNSDatabase();
		while(sc.hasNext()) {
			String line = sc.nextLine();
			StringTokenizer stringTokenizer = new StringTokenizer(line);
			db.addEntry(stringTokenizer.nextToken(), stringTokenizer.nextToken(), stringTokenizer.nextToken());
		}
		return db;
	}
}
