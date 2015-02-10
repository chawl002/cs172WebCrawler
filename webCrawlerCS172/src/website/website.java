package website;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
public class website {
	public class java {

	}

	public URL url;
	public Queue<URL> urls = new LinkedList<URL>();
	
	public website(URL url) {
		this.url = url;
	}
	public website(URL url, String hostName, String pathName, boolean allowed) {}
}
