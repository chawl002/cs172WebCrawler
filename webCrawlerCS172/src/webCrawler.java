
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

import javax.xml.ws.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import RobotExclusionUtil.RobotExclusionUtil;
import RobotExclusionUtil.RobotExclusionUtil.java;

import website.website;


public class webCrawler 
{
	public static Queue<website> theFrontier = new LinkedList<website>();//stores all the web-sites the crawler will eventually crawl to
	public static ConcurrentHashMap<URL, Integer> crawled_already = new ConcurrentHashMap<URL, Integer>();//stores all the web-sites that have been crawled
	
	//////////////////HELPER FUNCTIONS////////////////////////
	
	//prints the entire contents of theFrontier queue
	public static void printQueue() {
		System.out.println("START! This line is not in the queue");
		Iterator iterator = theFrontier.iterator();
		while(iterator.hasNext()){
		  website element = (website) iterator.next();
		  System.out.println("URL: " + element.url.toString());
		}
		System.out.println("END! This line is not in the queue");
	}
	
	public static void printHash()
	{
		Iterator it = crawled_already.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry pairs = (Map.Entry)it.next();
			URL temp = (URL) pairs.getKey();
			System.out.println("Key: " + temp);
		}
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
	
	public static boolean connectionTimeOut(URL url) throws IOException
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

///////////////////////////////HELPER FUNCTIONS END	
	
	
	//Load all the seeds from the seed.txt file and puts them into Frontier
	public static void load_seeds()
	{
		File file = new File("seed.txt"); //open seeds file
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(file));
			String url_text = null;
			
			while((url_text = reader.readLine()) != null) //while there are links in the file, add them to the theFrontier
			{	
				URL url = new URL(url_text);
				website site = new website(url);
				theFrontier.add(site);
			}
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
				//close the file when done
				if(reader != null)
				{
					reader.close();
				}
			}
			catch(IOException e)
			{ System.out.println("IOException in load seeds");}
		}
	}
	
public static void crawl_frontier(Integer numPagesToSearchFor, Integer numHopsAwayFromSeed) throws URISyntaxException, IOException
{
	String url;
	website site = theFrontier.peek();
	URL urlObject;
	Queue<URL> extractedSites = new LinkedList<URL>();
	Integer pageNum = 0;
	boolean robotsShouldFollow = false;
	
	//continue this code until theFrontier runs out of links, the limit on pages has been reached, and the numbers of hops has been reached
	while(pageNum < numPagesToSearchFor && theFrontier.size() != 0)
	{
		if(theFrontier.peek() != null)
		{
			site = theFrontier.peek();
			urlObject = site.url;
			//if the current website has already been crawled, ignore it and find one that hasnt been crawled yet
			while(crawled_already.containsKey(site.url) && !crawled_already.isEmpty())
			{
				theFrontier.remove();
				site = theFrontier.peek();
				urlObject = site.url;
			}
		}
		else{
			return;
		}
			
		try {
			
			//use URI to clean and normalize the URLS
			URI uri = urlObject.toURI().normalize();
			
			//check if robots.txt file exists	
			RobotExclusionUtil robotUtility = new RobotExclusionUtil();
			robotsShouldFollow = robotUtility.robotsShouldFollow(urlObject.getHost()); //Should we crawl this page or not?
			
			//If we should crawl the page, then connect to the website and extract the HTML and more links for frontier
			if(robotsShouldFollow){
			
				Document doc;
				Elements questions;
				String protocol = urlObject.getProtocol() + "://";
				String hostName = urlObject.getHost();
				String path = urlObject.getPath();
				
			/////Connect to URL
				doc = Jsoup.connect(protocol + hostName + path).get();
				
			//////CHECK TO SEE IF LINK REDIRECTS OR NOT, if so get new redirected URL
				String redirectedURL = Jsoup.connect("http://" + urlObject.getHost()).followRedirects(true).execute().url().toExternalForm();
				
			////////EXTRACT HTML and save it
				String htmlContent = doc.html();
				String tempURI = "http://" + uri.getHost(); //get a new name for the txt file based on the url
				tempURI = tempURI.replaceAll("http://", "_") + uri.getPath();
				String fileName = tempURI.replaceAll("/", "_");
				saveAsFile(fileName, htmlContent);
				
			///////SCAN HTML for links and extract them
				questions = doc.select("a[href]");
				Integer i = 0;
				boolean isCrawled = false;
				
				//go through entire HTML doc searchin for links out
				Iterator it = crawled_already.entrySet().iterator();
				for(Element link : questions)
				{		
					if(link.attr("href").contains(".edu") && !link.attr("href").contains("mailto:"))//if the link ends with .edu, we want to save it for later
					{
						String url_text = link.attr("abs:href");
						URL temp = new URL(url_text); //convert url string into a Website object
						
						if(!crawled_already.containsKey(temp))//check to see if we've already crawled the URL. if not, extract it
						{
							extractedSites.add(temp);//add the URLs from this HTML file to a queue
							theFrontier.add(new website(temp));//add the extracted website to crawl it later

						}
					}
				}
				
				//clear the site.url as a roundabout to copying a temporary queue of URLs to the actual URLs in a web site
				site.urls.clear();
				site.urls.addAll(extractedSites);

				//update loop iterator
				pageNum++;
				
				URL newUrl = new URL(redirectedURL);
				
				//added the new redirected URL to the crawler (not the original URL)
				crawled_already.put(urlObject, 1);
				
				//push out Hashed objects to console in real time
				System.out.println("Going into Hash: " + urlObject);
				
				//remove the URL we just looked at from theFrontier
				theFrontier.remove();
			}
		}
		catch(MalformedURLException e) {
			System.out.println("	Could not create a URL\n");
			theFrontier.remove(); 
			theFrontier.remove(); 
			site = theFrontier.peek();
			urlObject = site.url;
			pageNum--;
		}
		catch(URISyntaxException e1){
			System.out.println("Cannot convert to a URI\n");
			theFrontier.remove(); 
			site = theFrontier.peek();
			urlObject = site.url;
			pageNum--;
		}
		catch(IOException e2) {
			System.out.println("	IO Exception: Restricted Access, other media types, etc");
				theFrontier.remove(); 
				site = theFrontier.peek();
				urlObject = site.url;
				pageNum--;
		}
	}
}
	
	public static void main(String [] args) throws MalformedURLException
	{
		Integer numberOfPagesToCrawl;
		//Integer numberOfHopsAwayFromSeed = 1;//6;
		if(args.length > 1)
		{
			numberOfPagesToCrawl = Integer.parseInt(args[0]);
			System.out.println(numberOfPagesToCrawl);
			//numberOfHopsAwayFromSeed = Integer.parseInt(args[2]);
		}
		else {
			//do nothing
			numberOfPagesToCrawl = 100;
			System.out.println("Default value, 100, is used for number of pages to crawl\n");
		}

		
		//load the seeds into frontier
		boolean success = (new File("htmlFiles")).mkdirs();
		if(!success) {
			System.out.println("Couldn't produce directory for HTML files.\n");
		}
		load_seeds();
		try {
			printQueue();
			System.out.println();
			crawl_frontier(numberOfPagesToCrawl, 0);//numberOfHopsAwayFromSeed);
			//printQueue();
			//printHash();
			System.out.println("End of Crawl");
		}
		catch(IOException e) {System.out.println("crawl_frontier had an IOException\n");}
		catch(URISyntaxException e1) {System.out.println("crawl_frontier had an URISyntaxException\n");}

	}
}
