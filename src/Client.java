import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by Frank on 2016-12-01.
 */
public class Client {
	public final int WEB_PORT;
	public final int DNS_PORT;
	public final int MY_DNS_PORT;
	public final String LOCAL_DNS_IP;

	private static final String MESSAGE_PROMPT_URL = "Url: ";
	private static final String MESSAGE_404 = "404 - Not Found.";
	private static final String MESSAGE_UNKNOWN_ERROR = "An unknown error occurred.";

	public Client(int myUdpPort, int webPort, int dnsPort, String localDnsIp) {
		WEB_PORT = webPort;
		DNS_PORT = dnsPort;
		MY_DNS_PORT = myUdpPort;
		LOCAL_DNS_IP = localDnsIp;

	}

	public void run(InputStream inputStream, OutputStream outputStream) {
		Scanner in = new Scanner(inputStream);
		PrintStream out = new PrintStream(outputStream);
		while(true) {
			out.print(MESSAGE_PROMPT_URL);
			String url = in.next();
			try {
				out.println(get(url));
			} catch (IOException e) {
				out.println(e.getMessage());
			}
			out.flush();
		}
	}

	public String get(String url) throws IOException {
		URI uri;
		try {
			uri = new URI(url.startsWith("http://") ? url : "http://" + url);
		} catch(URISyntaxException e) {
			throw new IllegalStateException("Invalid url provided.");
		}
		int destPort = uri.getPort() != -1 ? uri.getPort() : WEB_PORT;
		String host = uri.getHost();

		if (!isIp(host)) {
			// need to resolve this via dns
			host = dnsLookup(host);
		}

		Socket socket = new Socket(host, destPort);
		Scanner scanner = new Scanner(socket.getInputStream());
		scanner.useDelimiter(Web.PROTOCOL_DELIM);

		OutputStream cOut = socket.getOutputStream();

		String path = uri.getPath();
		if (path.isEmpty()) path = "/";

		cOut.write(path.getBytes());
		cOut.write(Web.PROTOCOL_DELIM.getBytes());

		int code = Integer.parseInt(scanner.next());
		switch (code) {
			case Web.STATUS_OK: return scanner.next();
			case Web.STATUS_NOT_FOUND: return MESSAGE_404;
			default: return MESSAGE_UNKNOWN_ERROR;
		}

	}

	// todo: resolve urls to ips
	public String dnsLookup(String url) throws SocketException {
		if (url.equals("localhost")) return url;
		throw new NotImplementedException();
	}

	private boolean isIp(String url) {
		final String IPADDRESS_PATTERN =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		Pattern p = Pattern.compile(IPADDRESS_PATTERN);
		return p.matcher(url).matches();
	}


}
