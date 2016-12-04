import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * A Web server that hosts content.
 * Created by Frank on 2016-12-02.
 */
public class Web {
	/**  */
	public static final byte
		STATUS_OK = (byte) 200,
		STATUS_NOT_FOUND = (byte) 404;
	public static final String PROTOCOL_DELIM = "\r\n\r\n";

	private final String[] FILES;

	public final int LOCAL_PORT;

	public Web(int port, String...files) throws IOException {
		FILES = files;
		LOCAL_PORT = port;
	}

	/**
	 * Starts the server and enters a connection accepting loop. This method never returns.
	 * @param msgOut Output to write log messages to.
	 */
	public void run(OutputStream msgOut) {
		final PrintWriter writer = new PrintWriter(msgOut);
		ServerSocket sSocket;

		writer.printf("Starting up server with content: %s\n", Arrays.toString(FILES));
		writer.flush();
		try { // Open the socket
			sSocket = new ServerSocket(LOCAL_PORT);
		} catch (IOException e) {
			writer.printf("Failed to start server on port %d\n", LOCAL_PORT);
			writer.flush();
			return;

		}
		writer.printf("Server started on port %d.\n", LOCAL_PORT);
		writer.flush();

		while(true) {
			// accept
			Socket socket;
			Scanner in;
			OutputStream out;

			try {
				socket = sSocket.accept();
				in = new Scanner(new BufferedInputStream(socket.getInputStream()));
				out = socket.getOutputStream();
			} catch (IOException e) {
				writer.printf("ERROR - Could not accept the connection.\n");
				return;
			}

			in.useDelimiter(PROTOCOL_DELIM);
			String filename = in.next();
			if (filename.equals("/")) filename = "/index.txt";

			try {
				if (containsFile(filename)) {
					writer.printf("200 - %s Requested file: %s\n", socket.getInetAddress().toString(), filename);
					out.write(new byte[]{STATUS_OK});
					readContent(out, filename);
				} else {
					writer.printf("404 - %s Requested file: %s\n", socket.getInetAddress().toString(), filename);
					out.write(new byte[]{STATUS_NOT_FOUND});
				}
				out.flush();
				out.close();
			} catch (IOException e) {
				writer.println("ERROR - There was an error writing to a connection.");
			}

			writer.flush();

			// Doesent matter if it works or not... no way to fix it
			try {socket.close();} catch(IOException ignored) {}
		}
	}

	/**
	 * Checks to see if this server has the requested file.
	 * @param file Name of the requested file.
	 * @return True if the server has the file available, false otherwise.
	 */
	private boolean containsFile(String file) {
		String r = file.startsWith("/") ? file.substring(1) : file;
		for (String s : FILES) if (s.equals(r)) return true;
		return false;
	}

	/**
	 * Pipes a file into an output stream.
	 * @param out Stream to pipe file to.
	 * @param filename Name of file to stream.
	 * @throws IOException Throws if something happens while streaming or loading the file.
	 */
	public void readContent(OutputStream out, String filename) throws IOException {
		if (filename.equals("/")) filename = "index.txt";
		if (filename.startsWith("/")) filename = filename.substring(1);
		FileInputStream fOs = new FileInputStream(new File(filename));
		pipe(fOs, out);
		out.flush();
		fOs.close();
	}

	/**
	 * Connects an input to an output stream.
	 * @param in Input stream that pipes...
	 * @param out ...directly to the output stream.
	 * @throws IOException Thows if there was an exception reading or writing to streams.
	 */
	private void pipe(InputStream in, OutputStream out) throws IOException {
		int read;
		byte[] buff = new byte[1024];
		while((read = in.read(buff)) > 0) {
			out.write(buff, 0, read);
		}
	}
}
