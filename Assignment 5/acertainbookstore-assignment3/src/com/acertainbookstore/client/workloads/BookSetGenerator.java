package com.acertainbookstore.client.workloads;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	List<StockBook> booksToAdd = null;

	public BookSetGenerator() {
		booksToAdd = new ArrayList<StockBook>();
		for (int i = 1; i <= 100; i++) {
			// Editor pick if one of the first 10 or even numbered
			Boolean editorPick = i <= 10 || i % 2 == 0;
			booksToAdd.add(new ImmutableStockBook(i, "bookTitle " + i, "author " + i, 10, 100, 0, 0, 0, editorPick));
		}
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 *
	 * @param isbns
	 * @param num
	 * @return num of random isbns from the input set
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		List<Integer> isbnList = new ArrayList<Integer>(isbns);
		Collections.shuffle(isbnList);
		return isbnList.stream().limit(num).collect(Collectors.toSet());
	}

	/**
	 * Generates a set of newly generated
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		List<StockBook> shuffledList = new ArrayList<>(booksToAdd);
		Collections.shuffle(shuffledList);
		return shuffledList
				.stream()
				.limit(num)
				.collect(Collectors.toSet());
	}

}
