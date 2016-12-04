import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * A client to connect to web and dns servers. Terminal interface built in.
 * Created by Frank on 2016-12-01.
 */
public class Client {
	public final int WEB_PORT;
	public final int DNS_PORT;
	public final int MY_DNS_PORT;
	public final String LOCAL_DNS_IP;

	// Common messages for ui
	private static final String MESSAGE_PROMPT_URL = "Url: ";
	private static final String MESSAGE_DOWNLOADED = "200 - File Downloaded.";
	private static final String MESSAGE_404 = "404 - Not Found.";
	private static final String MESSAGE_CANT_CONNECT = "ERROR - Could not connect to server.";
	private static final String MESSAGE_CANT_WRITE_FILE = "ERROR - Could not write to file.";
	private static final String MESSAGE_CANT_DOWNLOAD_FILE = "ERROR - Could not download file.";
	private static final String MESSAGE_CANT_DISPLAY_CONTENT = "ERROR - Could not display content.";
	private static final String MESSAGE_CANT_RESOLVE = "ERROR - Could not resolve url.";
	private static final String MESSAGE_INVALID_URL = "ERROR - The provided url is invalid.";
	private static final String MESSAGE_UNKNOWN_ERROR = "ERROR - Server could not be contacted.";

	/**
	 * Create a Client listening on a specific port, and resolve dns queries with a specific ip/port.
	 * @param webPort Port to make tcp connections on.
	 * @param dnsPort Port to use for sending and recieving dns requests.
	 * @param localDnsIp Ip address of a dns server.
	 */
	public Client(int webPort, int dnsPort, String localDnsIp) {
		WEB_PORT = webPort;
		DNS_PORT = dnsPort;
		MY_DNS_PORT = dnsPort;
		LOCAL_DNS_IP = localDnsIp;
	}

	/**
	 * Starts the client. This method will run forever until it is force-killed.
	 * @param inputStream The stream to read user input from.
	 * @param outputStream The stream to write log messages to.
	 */
	public void run(InputStream inputStream, OutputStream outputStream) {
		// For outputting logs
		Scanner in = new Scanner(inputStream);
		PrintStream out = new PrintStream(outputStream);
		while(true) {
			out.print(MESSAGE_PROMPT_URL);
			URL url = toUrl(in.next());
			out.println(get(url));
			out.flush();
		}
	}

	/**
	 * Helper to convert strings into URL objects for better parsing.
	 * @param rawUrl The raw string representation of a url.
	 * @return The url object representing the raw string input.
	 */
	private URL toUrl(String rawUrl) {
		try {
			// We need to add a protocol infront of the url for it to be parsed correctly.
			return new URL(rawUrl.startsWith("http://") ? rawUrl : "http://" + rawUrl);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(MESSAGE_INVALID_URL);
		}
	}

	/**
	 * Sends a "get" request, and blocks for the result.
	 * If the file downloaded is not index, or txt, it will be downloaded as a
	 * separate file and opend with the os specific application for the file extention.
	 * @param url Url to query.
	 * @return String result of the file.
	 */
	public String get(URL url) {
		int destPort = url.getPort() != -1 ? url.getPort() : WEB_PORT;
		String host = url.getHost();

		if (!isIp(host)) {
			// If the host is not an explicit ip, ask the dns server to find the ip for us.
			try {
				host = dnsLookup(host);
			} catch (IOException e) {
				throw new IllegalStateException(MESSAGE_CANT_RESOLVE);
			}
		}

		Socket socket;
		BufferedInputStream inBuff;
		OutputStream cOut;

		try { // Create the socket to connect on.
			socket = new Socket(host, destPort);
			inBuff = new BufferedInputStream(socket.getInputStream());
			cOut = socket.getOutputStream();
		} catch (Exception e) {
			throw new IllegalStateException(MESSAGE_CANT_CONNECT);
		}

		// Get the requested file path.
		String path = url.getPath();
		if (path.isEmpty()) path = "/";

		// writes to the open socket stream to request file.
		try {
			cOut.write(path.getBytes());
			cOut.write(Web.PROTOCOL_DELIM.getBytes());
		} catch (IOException e) {
			throw new IllegalStateException(MESSAGE_CANT_CONNECT);
		}

		// Awaits for the responce to be received.
		byte[] codeBuff = new byte[1];
		int code = -1;
		try {
			inBuff.read(codeBuff, 0, 1);
			code = codeBuff[0];
		} catch (IOException e) {
			code = -1;
		}
		String message;
		switch (code) {
			case Web.STATUS_OK:
				if (!getExtension(url).equals(".txt")) {
					try {
						File file = new File(getLocalFileName(url));
						file.delete();
						FileOutputStream fOut = new FileOutputStream(file);
						pipe(inBuff, fOut);
						fOut.flush();
						fOut.close();
						Desktop.getDesktop().open(file);
					} catch (FileNotFoundException e) {
						throw new IllegalStateException(MESSAGE_CANT_WRITE_FILE);
					} catch (IOException e) {
						throw new IllegalStateException(MESSAGE_CANT_DOWNLOAD_FILE);
					}
					message = MESSAGE_DOWNLOADED;
					break;
				} else {
					try {
						message = readString(inBuff);
					} catch (IOException e) {
						message = MESSAGE_CANT_DISPLAY_CONTENT;
					}
					break;
				}
			case Web.STATUS_NOT_FOUND:
				message = MESSAGE_404;
				break;
			default:
				message = MESSAGE_UNKNOWN_ERROR;
		}

		// Clean up our mess
		try {
			inBuff.close();
			cOut.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}

	/**
	 * Lookup a url for resolving a domain name.
	 * @param url The url of the domain you wish to seek.");
	 * @return
	 * @throws IOException Request timeouts, or a problem with socket io.
	 */
	String dnsLookup(String url) throws IOException {
		if (url.equals("localhost")) {
			return url;
		}

		String[] tokens = Dns.request(url, MY_DNS_PORT, LOCAL_DNS_IP, DNS_PORT);

		if (tokens != null && (tokens[0].equals(Dns.DNS_TYPE_A))) {
			return tokens[1];
		} else throw new IllegalStateException(MESSAGE_CANT_RESOLVE);
	}

	/**
	 * Checks if a url is a litteral ip address.
	 * @param url A address string.
	 * @return True if a literal ip. False otherwise.
	 */
	private boolean isIp(String url) {
		final String IPADDRESS_PATTERN =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		Pattern p = Pattern.compile(IPADDRESS_PATTERN);
		return p.matcher(url).matches();
	}

	/**
	 * Gets the local filename for a file to download it. All local filenames are prepended with the 'downloaded_' prefix.
	 * @param url Url to parse path from.
	 * @return The local destination file, with prepended 'downloaded_' prefix.
	 */
	private String getLocalFileName(URL url) {
		String name = url.getFile();
		if (name.startsWith("/")) name = name.substring(1);
		return "downloaded_"+name;
	}

	/**
	 * Gets the extension of the file at a specific endpoint. If no extension is present, it will default to '.txt'.
	 * @param url The url to parse.
	 * @return The extension of the requested file.
	 */
	private String getExtension(URL url) {
		String path = url.getFile();
		int i = path.lastIndexOf(".");
		if (i == -1) return ".txt";
		return path.substring(i);
	}

	/**
	 * Utility for piping one stream into another.
	 * @param in Pipes from this stream...
	 * @param out ...into this one.
	 * @throws IOException Some stream writing exception could be thrown.
	 */
	private void pipe(InputStream in, OutputStream out) throws IOException {
		int bytesRead ;
		byte[] buffer = new byte[1024];
		while ((bytesRead = in.read(buffer)) > 0) {
			out.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Reads a stream to completion, then returns the red bytes as a String.
	 * @param in Stream to read from.
	 * @return Stream output in bytes.
	 * @throws IOException Some stream reading exceptions may be thrown...
	 */
	private String readString(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		int count;
		byte[] buffer = new byte[1024];
		while((count = in.read(buffer)) > 0)
			sb.append(new String(Arrays.copyOfRange(buffer, 0, count)));
		return sb.toString();
	}

}
