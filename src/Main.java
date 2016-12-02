import java.io.IOException;
import java.net.URL;

/**
 * Created by Frank on 2016-11-27.
 */
public class Main {
	public static void main(String[] args) throws IOException {
		URL uri = new URL("http://192.168.1.1");
		System.out.println(uri.getHost());
		System.out.println(uri.getPath());
		System.out.println(uri.getQuery());
		System.out.println(uri.getPort());
	}
}
