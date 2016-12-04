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

	public static void main(String[] args) throws IOException, URISyntaxException {

		loadPortConfigurations(CONFIG_FILE);

		switch(args[0].toLowerCase()) {
			case COMMAND_CLIENT:
				runClient(webPort, dnsPort, args[1]);
				break;
			case COMMAND_WEB:
				runWeb(webPort, Arrays.copyOfRange(args, 1, args.length));
				break;
			case COMMAND_DNS:
				runDNS(dnsPort, args[1]);
				break;
			default:
				System.out.println("WRONG! Usage: app <client|dns|server> <configFile1.txt[...]>");
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
			}
		}
		scanner.close();
	}

	private static void runClient(int webPort, int dnsPort, String localDnsIp) {
		Client client = new Client(webPort, dnsPort, localDnsIp);
		client.run(System.in, System.out);
	}

	private static void runDNS(int port, String databaseFile) {
		Dns dns = null;
		try {
			dns = new Dns(port, databaseFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (dns != null)
			dns.run();
	}

	private static void runWeb(int port, String...files) throws IOException {
		Web server = new Web(port, files);
		server.run(System.out);
	}

}
