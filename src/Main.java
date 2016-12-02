import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by Frank on 2016-11-27.
 */
public class Main {
	public static final String LOCAL_DNS_IP = "localhost";
	public static final int
		WEB_PORT = 8080,
		DNS_PORT = 5353;
	private static final String
		COMMAND_CLIENT = "client",
		COMMAND_WEB = "web",
		COMMAND_DNS = "dns";

	public static void main(String[] args) throws IOException, URISyntaxException {

		switch(args[0].toLowerCase()) {
			case COMMAND_CLIENT:
				runClient(Integer.parseInt(args[1]));
				break;
			case COMMAND_WEB:
				System.out.println("NOT IMPLEMENTED YET");
				break;
			case COMMAND_DNS:
				System.out.println("NOT IMPLEMENTED YET");
				break;
			default:
				System.out.println("WRONG! Ussage: app <client|dns|server> <port> [configFile]");
		}
	}

	public static void runClient(int myUdpPort) {
		Client client = new Client(myUdpPort, WEB_PORT, DNS_PORT, LOCAL_DNS_IP);
		client.run(System.in, System.out);
	}
}
