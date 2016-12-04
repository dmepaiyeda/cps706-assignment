import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Frank on 2016-11-27.
 */
public class Main {
	private static final String CONFIG_FILE = "config.txt";
	private static final String DEFAULT_LOCAL_DNS_IP = "localhost";
	private static final int
		DEFAULT_WEB_PORT = 8080,
		DEFUALT_DNS_PORT = 5353;
	private static final String
		COMMAND_CLIENT = "client",
		COMMAND_WEB = "web",
		COMMAND_DNS = "dns";

	private static int
		dnsPort = DEFUALT_DNS_PORT,
		webPort = DEFAULT_WEB_PORT;
	static String localDnsIp = DEFAULT_LOCAL_DNS_IP;

	public static void main(String[] args) throws IOException, URISyntaxException {

		loadPortConfigurations(CONFIG_FILE);

		switch(args[0].toLowerCase()) {
			case COMMAND_CLIENT:
				runClient(Integer.parseInt(args[1]), webPort, dnsPort, localDnsIp);
				break;
			case COMMAND_WEB:
				runWeb(Integer.parseInt(args[1]), Arrays.copyOfRange(args, 2, args.length));
				break;
			case COMMAND_DNS:
				runDNS(Integer.parseInt(args[1]), args[2]);
				break;
			default:
				System.out.println("WRONG! Usage: app <client|dns|server> <port> [configFile]");
		}
	}

	private static void loadPortConfigurations(String filename) throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileInputStream(new File(filename)));
		while(scanner.hasNext()) {
			switch(scanner.next()) {
				case "dns":
					dnsPort = scanner.nextInt();
					break;
				case "web":
					webPort = scanner.nextInt();
					break;
				case "localDnsIp":
					localDnsIp = scanner.next();
					break;
			}
		}
		scanner.close();
	}

	private static void runClient(int myUdpPort, int webPort, int dnsPort, String localDnsIp) {
		Client client = new Client(myUdpPort, webPort, dnsPort, localDnsIp);
		client.run(System.in, System.out);
	}

	private static void runDNS(int port, String databaseFile) {
		Dns dns = null;
		try {
			dns = new Dns(port, databaseFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		dns.run(System.out);
	}

	private static void runWeb(int port, String...files) throws IOException {
		Web server = new Web(port, files);
		server.run(System.out);
	}

}
