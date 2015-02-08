
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardOpenOption.*;
import java.nio.file.*;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URI;
import java.net.SocketTimeoutException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import RobotExclusionUtil.RobotExclusionUtil;
import RobotExclusionUtil.RobotExclusionUtil.java;


public class webCrawler 
{
	public static final int SEARCH_LIMIT = 10;
	public static Queue<String> Frontier = new LinkedList<String>();
	//public static Queue<String> crawled_already = new LinkedList<String>();
	public static ConcurrentHashMap<String, Integer> crawled_already = new ConcurrentHashMap<String, Integer>();
	
	//Load all the seeds from the seed.txt file and puts them into Frontier
	
	public static void printQueue() {
		Iterator iterator = Frontier.iterator();
		while(iterator.hasNext()){
		  String element = (String) iterator.next();
		  System.out.println("URL: " + element);
		}
		System.out.println("END! This line is not in the queue");
	}
	public static boolean urlExistsInFrontier(String url) {
		Iterator iterator = Frontier.iterator();
		while(iterator.hasNext()) {
			if(iterator.equals(url)){
				return true;
			}
		}
		return false;
		
	}
	public static void load_seeds()
	{
		//More info on this code: stackoverflow.com/questions/3806062
		File file = new File("seed.txt");
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(file));
			String url_text = null;
			
			while((url_text = reader.readLine()) != null)
			{
				Frontier.add(url_text);
			}
			printQueue();
		}catch(FileNotFoundException e) //returns an exception if it cant find the file
		{
			//This function tells you where the exception happened to help you find errors easier
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(reader != null)
				{
					reader.close();
				}
			}
			catch(IOException e)
			{}
		}
	}
	
public static boolean robotSafe(URL url) throws IOException
{
	try{
		String strHost = url.getHost();
		String strRobot = "http://" + strHost + "robots.txt";
		Jsoup.connect(strRobot)
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:14.0) Gecko/20100101 Firefox/14.0.1")
				.timeout(5000)
				.execute();
		return true;
	}catch(SocketTimeoutException e)
	{
		System.out.println("Timeout occured");
		return false;
	}
}
	

public static void crawl_frontier() throws URISyntaxException, IOException
{
	//remove a link from frontier
	String url;
	
	if(Frontier.peek() != null) {
		url = Frontier.remove();
	}
	else {
		return;
	}
	boolean robotsShouldFollow = false;
	try {
		//check if robots.txt file exists
		//URI uri = new URI(url);
		URL urlObject = new URL(url);
		URI uri = urlObject.toURI();
		RobotExclusionUtil robotUtility = new RobotExclusionUtil();
		if(robotSafe(urlObject)) {
			//check the contents of robots.txt file
			robotsShouldFollow = robotUtility.robotsShouldFollow(urlObject.getHost());
			System.out.println("Is robotSafe");
		}
		else
		{
			System.out.println("Is not robotSafe");
		}
	}
	catch(MalformedURLException e) {System.out.println("Could not create a URL\n");}
	catch(URISyntaxException e){System.out.println("Cannot convert to a URI\n");}
	
	Document doc;
	Elements questions;
	ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> map;
	//Open URL and clean up HTML
	if(robotsShouldFollow)
	{//Check what its allowed
		
		
		
		
		
		
	}
		//map = new ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>>(url, RobotExclusionUtil.map);
		doc = Jsoup.connect(url).get();
		crawled_already.put(url, 1);
		String htmlContent = doc.html();
		String fileName = url.replaceAll("http://", "_");
		System.out.println("fileName: " + fileName);
		saveAsFile(fileName, htmlContent);
		
		//scan html doc for links and extract them
		questions = doc.select("a[href]");
		
		for(Element link: questions)
		{
			if(link.attr("href").contains(".edu"))
			{
				
					
				if(!urlExistsInFrontier(link.attr("abs:href")))//!crawled_already.containsKey(link.attr("abs:href") && !urlExistsInFrontier(link.attr("abs:href")))
				{	
					Frontier.add(link.attr("abs:href"));
				}
				//processPage(link.attr("abs:href"));
			}
		}
		System.out.println("hashmap: " + crawled_already);
}
	
	private static void processPage(String attr) {
	// TODO Auto-generated method stub
	
	}
	private static void saveAsFile(String fileName, String htmlContent) {
		try {
			
			PrintWriter writer = new PrintWriter("./htmlFiles/" + fileName + ".txt", "UTF-8");
			writer.println(htmlContent);
			writer.close();
		}
		catch(UnsupportedEncodingException e) {System.out.println("Unsupported Encoding Exception");}
		catch(FileNotFoundException e1) {System.out.println("File Not Found Exception");}
		
	
	}
	public static void main(String [] args)
	{
		//load the seeds into frontier
		boolean success = (new File("htmlFiles")).mkdirs();
		if(!success) {
			System.out.println("Couldn't produce directory for HTML files.\n");
		}
		load_seeds();
		try {
			crawl_frontier();
		}
		catch(IOException e) {System.out.println("crawl_frontier had an IOException\n");}
		catch(URISyntaxException e1) {System.out.println("crawl_frontier had an URISyntaxException\n");}
		
		printQueue();
		//get key words from user
		//System.out.print("Query: ");
		//String query;
		//query = TextIO.getln();
		//crawl_frontier(query);

		
	}
}
