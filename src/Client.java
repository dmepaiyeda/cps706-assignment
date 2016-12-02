import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by Frank on 2016-12-01.
 */
public class Client {

	String dnsIp;
	int tcpPort, udpPort;

	HashMap<String, String> dns = new HashMap<>();

	public Client(int tcpPort, int udpPort, String dnsIp) {
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		this.dnsIp = dnsIp;
	}

	public String get(String url) throws URISyntaxException, IOException {
		URI uri = new URI(url.startsWith("http://") ? url : "http://" + url);
		Socket socket = new Socket(uri.getHost(), tcpPort);
		Scanner scanner = new Scanner(socket.getInputStream());
		scanner.useDelimiter("\r\n");
		socket.getOutputStream().write(("get " + uri.getPath()).getBytes());
		return scanner.next();
	}

	public String dnsLookup(String url) {
		return null;
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
