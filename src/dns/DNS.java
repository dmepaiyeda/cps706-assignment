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
 * Responses are made in the form [TYPE] [URL] Ex. A hisCinema.com
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
	private DNSDatabase db;

	/**
	 * @param port The port to listen for requests from
	 * @param filename The name of the database file
	 * @throws FileNotFoundException If there was a problem with the given filename
	 */
	public DNS(int port, String filename) throws FileNotFoundException {
		this.port = port;
		this.db = this.populatedDatabaseFromFile(filename);
	}

	/**
	 * @param filename the name of the database file
	 * @return The populated DNSDatabase
	 */
	private DNSDatabase populatedDatabaseFromFile(String filename) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(filename));
		DNSDatabase db = new DNSDatabase();
		while(sc.hasNext()) {
			String line = sc.nextLine();
			StringTokenizer stringTokenizer = new StringTokenizer(line);
			db.addEntry(stringTokenizer.nextToken(), stringTokenizer.nextToken(), stringTokenizer.nextToken());
		}
		return db;
	}
}
