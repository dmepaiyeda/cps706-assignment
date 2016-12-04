import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * The application entry point. Provides some cli, and dynamic config file features.
 * Created by Frank on 2016-11-27.
 */
public class Main {
	/** Config file to load the default ports from. */
	private static final String CONFIG_FILE = "config.txt";
	/** Default ports if the config file could not be found. */
	private static final int
		DEFAULT_WEB_PORT = 8080,
		DEFUALT_DNS_PORT = 5353;
	/** Cli command options */
	private static final String
		COMMAND_CLIENT = "client",
		COMMAND_WEB = "web",
		COMMAND_DNS = "dns";

	/** Loaded default ports. */
	private static int
		dnsPort = DEFUALT_DNS_PORT,
		webPort = DEFAULT_WEB_PORT;

	public static void main(String[] args) throws IOException, URISyntaxException {

		/** Load ports from config file. */
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

	/**
	 * Loads ports from a file.
	 * @param filename Filename to load ports from.
	 * @throws FileNotFoundException Thrown if the file does not exist.
	 */
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

	/**
	 * Runs the client application.
	 * @param webPort Port to use for connecting to web servers by tcp.
	 * @param dnsPort Port to use for resolving dns queries by udp (Of the loacl dns server).
	 * @param localDnsIp Ip address of the loacl dns server.
	 */
	private static void runClient(int webPort, int dnsPort, String localDnsIp) {
		Client client = new Client(webPort, dnsPort, localDnsIp);
		client.run(System.in, System.out);
	}

	/**
	 * Runs the Dns server application.
	 * @param port Port to listen and send on.
	 * @param databaseFile Filename of the config file to init dns records.
	 */
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

	/**
	 * Starts the Web server application.
	 * @param port Port to listen for connections on.
	 * @param files List of files to be accessible as content.
	 * @throws IOException Throws if there was a problem starting the socket.
	 */
	private static void runWeb(int port, String...files) throws IOException {
		Web server = new Web(port, files);
		server.run(System.out);
	}

}
