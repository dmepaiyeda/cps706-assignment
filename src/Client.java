import sun.nio.ch.IOUtil;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.net.*;
import java.util.Arrays;
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
	private static final String MESSAGE_DOWNLOADED = "200 - File Downloaded.";
	private static final String MESSAGE_404 = "404 - Not Found.";
	private static final String MESSAGE_CANT_CONNECT = "ERROR - Could not connect to server.";
	private static final String MESSAGE_CANT_WRITE_FILE = "ERROR - Could not write to file.";
	private static final String MESSAGE_CANT_DOWNLOAD_FILE = "ERROR - Could not download file.";
	private static final String MESSAGE_CANT_RESOLVE = "ERROR - Could not resolve url.";
	private static final String MESSAGE_INVALID_URL = "ERROR - The provided url is invalid.";
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
			URL url = toUrl(in.next());
			try {
				out.println(get(url));
			} catch (NotImplementedException e) {
				out.println("Dns lookup not yet implemented...");
			} catch (Exception e) {
				out.println(e.getMessage());
			}
			out.flush();
		}
	}

	private URL toUrl(String rawUrl) {
		try {
			return new URL(rawUrl.startsWith("http://") ? rawUrl : "http://" + rawUrl);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(MESSAGE_INVALID_URL);
		}
	}

	public String get(URL url) {
		int destPort = url.getPort() != -1 ? url.getPort() : WEB_PORT;
		String host = url.getHost();

		if (!isIp(host)) {
			// Lookup via dns
			try {
				host = dnsLookup(host);
			} catch (SocketException e) {
				throw new IllegalStateException(MESSAGE_CANT_RESOLVE);
			}
		}

		Socket socket;
		BufferedInputStream inBuff;
		OutputStream cOut;

		try {
			socket = new Socket(host, destPort);
			inBuff = new BufferedInputStream(socket.getInputStream());
			cOut = socket.getOutputStream();
		} catch (Exception e) {
			throw new IllegalStateException(MESSAGE_CANT_CONNECT);
		}

		String path = url.getPath();
		if (path.isEmpty()) path = "/";

		try {
			cOut.write(path.getBytes());
			cOut.write(Web.PROTOCOL_DELIM.getBytes());
		} catch (IOException e) {
			throw new IllegalStateException(MESSAGE_CANT_CONNECT);
		}

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
						/*
						int count;
						byte[] buff = new byte[1024];
						while((count = inBuff.read(buff)) > 0)
							fOut.write(buff, 0, count);
							*/
						pipe(inBuff, fOut);
						fOut.flush();
						fOut.close();
						file = null;
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
						e.printStackTrace();
						message="oops...";
					}
					break;
				}
			case Web.STATUS_NOT_FOUND:
				message = MESSAGE_404;
				break;
			default:
				message = MESSAGE_UNKNOWN_ERROR;
		}

		try {
			cOut.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
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

	private String getLocalFileName(URL url) {
		String name = url.getFile();
		if (name.startsWith("/")) name = name.substring(1);
		return "downloaded_"+name;
	}

	private String getExtension(URL url) {
		String path = url.getFile();
		int i = path.lastIndexOf(".");
		if (i == -1) return ".txt";
		return path.substring(i);
	}

	private void pipe(InputStream in, OutputStream out) throws IOException {
		int bytesRead ;
		byte[] buffer = new byte[1024];
		while ((bytesRead = in.read(buffer)) > 0) {
			out.write(buffer, 0, bytesRead);
		}
	}

	private String readString(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		int count;
		byte[] buffer = new byte[1024];
		while((count = in.read(buffer)) > 0)
			sb.append(new String(Arrays.copyOfRange(buffer, 0, count)));
		return sb.toString();
	}

}
