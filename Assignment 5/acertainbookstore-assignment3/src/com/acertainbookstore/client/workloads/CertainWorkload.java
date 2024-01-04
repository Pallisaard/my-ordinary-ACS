/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.ImmutableStockBook;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the
 * environment for the workers using WorkloadConfiguration objects and reports
 * the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 1000;
		String serverAddress = "http://localhost:8081";
		boolean localTest = false;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();
		// 7650523.793585229 / 37608.5475356355
		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore, stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		Boolean outputForTsv = true;
		double throughput = 0.0;
		double latency = 0.0;
		long freqBookStoreInteractions = 0;
		long totalRuns = 0;
		long successfulInteractions = 0;

		for (WorkerRunResult res : workerRunResults) {
			double timeElapsedInSecs = (double) (res.getElapsedTimeInNanoSecs() * 10e-9);
			double timeElapsedInMicroSecs = (double) (res.getElapsedTimeInNanoSecs() * 10e-3);
			throughput += (double) res.getSuccessfulInteractions() / (double) timeElapsedInSecs;
			latency += (double) timeElapsedInMicroSecs / (double) res.getSuccessfulInteractions();
			freqBookStoreInteractions += res.getTotalFrequentBookStoreInteractionRuns();
			totalRuns += res.getTotalRuns();
			successfulInteractions += res.getSuccessfulInteractions();
			// System.out.println("time in nano secs: " + res.getElapsedTimeInNanoSecs());

		}
		latency = latency / (double) workerRunResults.size();
		double customerShare = (double) freqBookStoreInteractions / (double) totalRuns;
		double successfulShare = (double) successfulInteractions / (double) totalRuns;

		if (outputForTsv) {
			// csv header:
			// Throughput (agg suc-int/sec),Latency (mean sec/suc-int),Customer Share,
			// Success Rate
			String csvLine = "";
			csvLine += "" + throughput;
			csvLine += ";" + latency;
			csvLine += ";" + customerShare;
			csvLine += ";" + successfulShare;
			System.out.printf(";%f;%f;%f;%f", throughput, latency, customerShare, successfulShare);
		} else {
			System.out.println("Throughput: " + throughput);
			System.out.println("Latency: " + latency);

			System.out.println("Customer share: " + customerShare);
			System.out.println("Success rate: " + successfulShare);
		}
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore, StockManager stockManager) throws BookStoreException {
		stockManager.removeAllBooks();
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		for (int i = 1; i <= 10; i++) {
			booksToAdd.add(new ImmutableStockBook(i, "bookTitle " + i, "author " + i, 10,
					100, 0, 0, 0, true));
		}
		stockManager.addBooks(booksToAdd);
	}
}
