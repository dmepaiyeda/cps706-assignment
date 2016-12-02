import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.net.*;
import java.util.HashMap;
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
	public final String PROTOCOL_DELIM = "\r\n\r\n";

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
			out.print("Url: ");
			String url = in.next();
			try {
				out.println(get(url));
			} catch (IOException e) {
				out.println(e.getMessage());
			}
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
		scanner.useDelimiter(PROTOCOL_DELIM);

		socket.getOutputStream().write(("get " + uri.getPath()).getBytes());

		return scanner.next();
	}

	// todo: resolve urls to ips
	public String dnsLookup(String url) throws SocketException {
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
