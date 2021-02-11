package informationRetrievel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import classes.Query;
import classes.QueryGet;
import classes.URL;

public class WithoutAPI implements InfoRetrival {

	DatabaseHandler db;

	public WithoutAPI() {
		db = new DatabaseHandler();
	}

	public void collectionData() {
		String logfile = Main.LogFile;
		BufferedWriter writer = null;
		PrintWriter logfile_writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(logfile));
			logfile_writer = new PrintWriter(writer);

		} catch (IOException e1) {
			System.err.println("main IOException - new BufferedWriter");
			e1.printStackTrace();
		}

		String folderName = Main.QueryForlder;

		List<Query> DomainQueryLang = new ArrayList<Query>();
		Main.fillList(DomainQueryLang, folderName);

		WebDriver driver; // declare webdriver instance
		System.setProperty("webdriver.chrome.driver", "lib\\chromedriver.exe"); // set
																				// path
		driver = new ChromeDriver(); // instantiate new Chrome driver

		for (Query domain : DomainQueryLang) {
			// search all web in specific lang and domain

			for (String query : domain.getQuery()) {
				// get query

				// get links from google
				driver.get("https://www.google.com"); // open google.com

				driver.findElement(By.name("q")).sendKeys(
						new String[] { query });
				driver.findElement(By.name("q")).submit();
				// Insert code here to locate and click on Search button

				List<String> allURLs = new ArrayList<String>();
				Main.getAllURL(driver, allURLs);// get url from page 1
				/*for (int i = 2; i < 4; i++) {
					Main.nextPage(driver, i);// go to page i
					Main.getAllURL(driver, allURLs);// get url from page i
				}*/

				for (String url : allURLs) {
					try {
						writer.write("Lang: " + domain.getLang());
						writer.newLine();
						writer.write("domain: " + domain.getDomain());
						writer.newLine();
						writer.write("Query: " + query);
						writer.newLine();
						writer.write("URL: " + url);
						writer.newLine();
						writer.write("-------------------------------------------------");
						writer.newLine();
					} catch (IOException e) {
						System.err
								.println("IOException - main - writer.write()");
						e.printStackTrace();
					}

					logfile_writer.println("Lang: " + domain.getLang());
					logfile_writer.println("domain: " + domain.getDomain());
					logfile_writer.println("Query: " + query);
					logfile_writer.println("URL: " + url);
					logfile_writer
							.println("-------------------------------------------------");

					System.out.println("Lang: " + domain.getLang());
					System.out.println("domain: " + domain.getDomain());
					System.out.println("Query: " + query);
					System.out.println("URL: " + url);

					try {
						String scriptResult;
						if(url.endsWith(".pdf"))
						{
							System.out.println("PDF !! saved empty");
							scriptResult = "";
						}
						else
						{
							System.out.println("NOT PDF");
							// get body to each url in this list of urls
							driver.get(url);
							JavascriptExecutor js;
							js = (JavascriptExecutor) driver;
							// String scriptReturningString = "";
							scriptResult = (String) js
									.executeScript("return document.getElementsByTagName('body')[0].innerText");
						}
						
						db.saveCollectDataWithoutAPI(domain.getDomain(),
								domain.getLang(), query, url, scriptResult);
						System.out.println("successfully saved to DB");
						System.out
								.println("-------------------------------------------------");
					} catch (Exception e) {
						db.saveCollectDataWithoutAPI(domain.getDomain(),
								domain.getLang(), query, url, "");
						System.out
								.println("This site can�t be reached EXCEPTION - saved empty in DB");
						System.out
								.println("-------------------------------------------------");
					}

				}

			}
		}

		driver.quit();

		try {
			writer.close();
		} catch (IOException e) {
			System.err.println("main IOException - writer.close()");
			e.printStackTrace();
		}
	}

	public void dataPreProssessing() {
		int threshold = 10;// nb of spaces

		db.filterDataWithoutAPI(threshold);

	}

	public void calculateSimilaraty() {

		List<QueryGet> querys = db.getFilteredQueriesWithoutAPI();
		try {

			int i = 1;
			for (QueryGet queryGet : querys) {
				System.out.println("---------------------------------------");
				System.out.println("Start query " + i);
				i++;
				 Set<Integer> identifiers = new HashSet<Integer>();
				for (URL urlStart : queryGet.getUrls()) {
					 identifiers.add(urlStart.getId());
					for (URL urlCheckSimilarity : queryGet.getUrls()) {
						if (urlStart.getId() == urlCheckSimilarity.getId() || identifiers.contains(urlCheckSimilarity.getId()) )
							continue;
						stringSimilarityCosing(urlStart, urlCheckSimilarity);
						// stringDistance(urlStart, urlCheckSimilarity, conn);
						// stringSimilarity(urlStart, urlCheckSimilarity, conn);
						// MatrixStringDistance(urlStart, urlCheckSimilarity,
						// conn);

					}
				}
			}

		} catch (Exception e) {
			System.err.println("Got an exception!");
			System.err.println(e.toString());
		}
	}

	public void stringSimilarityCosing(URL u1, URL u2) {
		String s1 = u1.getContentText();
		String s2 = u2.getContentText();

		System.out.println(u1.getId() + " vs " + u2.getId());

		System.out.println("cosine : ");
		CosineSimilarity cs1 = new CosineSimilarity();

		double startTime = System.currentTimeMillis();

		double dist = cs1.Cosine_Similarity_Score(s1, s2);

		double endTime = System.currentTimeMillis();
		System.out.println("Similarity1 = " + dist);
		System.out.println("Exec time = " + (endTime - startTime));
		if (dist < Main.threasholdCosine)
			return;

		db.similarityAddWithoutAPI(u1.getId(), u2.getId(), "cosine",
				"similarity", dist, (endTime - startTime));

		System.out.println();
	}

}
