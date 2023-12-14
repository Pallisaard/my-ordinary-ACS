package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreConcurrencyTest} tests the {@link BookStore} interface in a
 * concurrent manor.
 *
 * @see BookStore
 */

public class BookStoreConcurrencyTest {

    /**
     * The Constant TEST_ISBN.
     */
    private static final int TEST_ISBN = 3044560;

    /**
     * The Constant NUM_COPIES.
     */
    private static final int NUM_COPIES = 2000;

    /**
     * The local test.
     */
    private static boolean localTest = true;

    /**
     * Single lock test
     */
    private static boolean singleLock = false;

    /**
     * Number of times getBooks is called
     */
    private static int numGetBooks = 100;

    /**
     * Array of number of books to buy and add for each SW book
     */
    private static int[] numBooks = {100, 90, 110};

    /**
     * The store manager.
     */
    private static StockManager storeManager;

    /**
     * The client.
     */
    private static BookStore client;

    /**
     * The client2.
     */
    private static BookStore client2;

    /**
     * Sets the up before class.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
            localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

            String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
            singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

            if (localTest) {
                if (singleLock) {
                    SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
                    storeManager = store;
                    client = store;
                    client2 = store;
                } else {
                    TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
                    storeManager = store;
                    client = store;
                    client2 = store;
                }
            } else {
                storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
                client = new BookStoreHTTPProxy("http://localhost:8081");
            }

            storeManager.removeAllBooks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to add some books.
     *
     * @param isbn   the isbn
     * @param copies the copies
     * @throws BookStoreException the book store exception
     */
    public void addBooks(int isbn, int copies) throws BookStoreException {
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
        StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
                0, false);
        booksToAdd.add(book);
        storeManager.addBooks(booksToAdd);
    }

    /**
     * Helper method to get the default book used by initializeBooks.
     *
     * @return the default book
     */
    public StockBook getDefaultBook() {
        return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
                false);
    }

    public StockBook getSWBook(Integer episode) {
        if (episode == 4) {
            return new ImmutableStockBook(1, "Star Wars: Episode IV - A New Hope", "George Lucas", (float) 10, NUM_COPIES, 0, 0, 0, false);
        } else if (episode == 5) {
            return new ImmutableStockBook(2, "Star Wars: Episode V - The Empire Strikes Back", "George Lucas", (float) 10, NUM_COPIES, 0, 0, 0, false);
        } else if (episode == 6) {
            return new ImmutableStockBook(3, "Star Wars: Episode VI - Return of the Jedi", "George Lucas", (float) 10, NUM_COPIES, 0, 0, 0, false);
        } else {
            return getDefaultBook();
        }
    }

    public ArrayList<StockBook> getStarWarsTrilogy() {
        StockBook book1 = getSWBook(4);
        StockBook book2 = getSWBook(5);
        StockBook book3 = getSWBook(6);
        ArrayList<StockBook> books = new ArrayList<StockBook>();
        books.add(book1);
        books.add(book2);
        books.add(book3);
        return books;
    }

    /**
     * Method to add a book, executed before every test case is run.
     *
     * @throws BookStoreException the book store exception
     */
    @Before
    public void initializeBooks() throws BookStoreException {
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
        booksToAdd.add(getDefaultBook());
        booksToAdd.addAll(getStarWarsTrilogy());
        storeManager.addBooks(booksToAdd);
    }

    /**
     * Method to clean up the book store, execute after every test case is run.
     *
     * @throws BookStoreException the book store exception
     */
    @After
    public void cleanupBooks() throws BookStoreException {
        storeManager.removeAllBooks();
    }

    /**
     * Tests that in 2 different threads, if the client calls buyBooks() while storeManager calls addCopies(), after
     * a certain number of iterations, the number of copies of the book should be the same as the number of copies
     * when we started.
     */
    @Test
    public void testConcurrencyTest1() {
        Thread clientThread = new Thread(() -> {
            try {
                Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
                booksToBuy.add(new BookCopy(TEST_ISBN, 10));
                for (int i = 0; i < 100; i++) {
                    client.buyBooks(booksToBuy);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });

        Thread storeManagerThread = new Thread(() -> {
            try {
                Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
                booksToAdd.add(new BookCopy(TEST_ISBN, 10));
                for (int i = 0; i < 100; i++) {
                    storeManager.addCopies(booksToAdd);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });

        clientThread.start();
        storeManagerThread.start();

        try {
            clientThread.join();
            storeManagerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        try {
            List<StockBook> books = storeManager.getBooks();
            for (StockBook book : books) {
                if (book.getISBN() == TEST_ISBN) {
                    assertEquals(NUM_COPIES, book.getNumCopies());
                }
            }
        } catch (BookStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests that in 2 different threads, if first thread calls buyBooks() and addCopies immediately after to replenish the
     * stock, after a certain number of iterations, the number of copies of the book should be the same as the number.
     * The second thread repeatedly calls getBooks to ensure that the snapshot returned is consistent. This test succeeds
     * after numGetBooks calls to getBooks have been made. The test fails if an inconsistent snapshot is returned.
     */
    @Test
    public void testConcurrencyTest2() throws BookStoreException, InterruptedException {
        Thread c1Thread = new Thread(() -> {
            try {
                Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
                booksToBuy.add(new BookCopy(1, numBooks[0]));
                booksToBuy.add(new BookCopy(2, numBooks[1]));
                booksToBuy.add(new BookCopy(3, numBooks[2]));

                Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
                booksToAdd.add(new BookCopy(1, numBooks[0]));
                booksToAdd.add(new BookCopy(2, numBooks[1]));
                booksToAdd.add(new BookCopy(3, numBooks[2]));

                for (int i = 0; i < numGetBooks; i++) {
                    client.buyBooks(booksToBuy);

                    storeManager.addCopies(booksToAdd);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });


        c1Thread.start();

        for (int i = 0; i < numGetBooks; i++) {
            List<StockBook> snapshot = storeManager.getBooks();

            for (StockBook book : snapshot) {
                if (book.getISBN() == 1) {
                    assertTrue(NUM_COPIES == book.getNumCopies()
                            || NUM_COPIES - numBooks[0] == book.getNumCopies());
                } else if (book.getISBN() == 2) {
                    assertTrue(NUM_COPIES == book.getNumCopies()
                            || NUM_COPIES - numBooks[1] == book.getNumCopies());
                } else if (book.getISBN() == 3) {
                    assertTrue(NUM_COPIES == book.getNumCopies()
                            || NUM_COPIES - numBooks[2] == book.getNumCopies());
                }
            }
        }

        c1Thread.join();
    }

    /**
     * Tests that in 2 different threads, if first thread calls addBooks() and removeBooks() immediately after each other
     * to either add a series of books or remove them all again, leaving the store empty. The second thread repeatedly
     * calls getBooks to ensure that the snapshot returned is consistent. This test succeeds after numGetBooks calls to
     * getBooks have been made. The test fails if an inconsistent snapshot is returned.
     */
    @Test
    public void testConcurrencyTest3() throws BookStoreException, InterruptedException {
        Thread c1Thread = new Thread(() -> {
            try {
                Set<StockBook> booksToAdd = new HashSet<StockBook>();
                booksToAdd.add(new ImmutableStockBook(1, "Star Wars: Episode IV - A New Hope", "George Lucas", (float) 10, NUM_COPIES, 0, 0, 0, false));
                booksToAdd.add(new ImmutableStockBook(2, "Star Wars: Episode V - The Empire Strikes Back", "George Lucas", (float) 10, NUM_COPIES, 0, 0, 0, false));
                booksToAdd.add(new ImmutableStockBook(3, "Star Wars: Episode VI - Return of the Jedi", "George Lucas", (float) 10, NUM_COPIES, 0, 0, 0, false));

                Set<Integer> booksToRemove = new HashSet<Integer>();
                booksToRemove.add(1);
                booksToRemove.add(2);
                booksToRemove.add(3);

                for (int i = 0; i < numGetBooks; i++) {
                    storeManager.removeBooks(booksToRemove);

                    storeManager.addBooks(booksToAdd);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });

        c1Thread.start();

        for (int i = 0; i < numGetBooks; i++) {
            List<StockBook> snapshot = storeManager.getBooks();

            for (StockBook book : snapshot) {
                if (book.getISBN() == 1) {
                    assertTrue(NUM_COPIES == book.getNumCopies()
                            ||  0 == book.getNumCopies());
                } else if (book.getISBN() == 2) {
                    assertTrue(NUM_COPIES == book.getNumCopies()
                            || 0 == book.getNumCopies());
                } else if (book.getISBN() == 3) {
                    assertTrue(NUM_COPIES == book.getNumCopies()
                            || 0 == book.getNumCopies());
                }
            }
        }

        c1Thread.join();
    }

    /**
     * Tests that in 3 different threads, if the first two thread calls buyBooks from a client and the third calls
     * addCopies twice from a store manager, after a certain number of iterations, the number of copies of the book
     * should be the same as the number of copies when we started. The test fails if an inconsistent snapshot is returned.
     * The test succeeds after numGetBooks calls to getBooks have been made.
     */
    @Test
    public void testConcurrencyTest4() throws BookStoreException, InterruptedException {
        Thread c1Thread = new Thread(() -> {
            try {
                Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
                booksToBuy.add(new BookCopy(TEST_ISBN, 10));

                for (int i = 0; i < numGetBooks; i++) {
                    client.buyBooks(booksToBuy);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });

        Thread c2Thread = new Thread(() -> {
            try {
                Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
                booksToBuy.add(new BookCopy(TEST_ISBN, 10));

                for (int i = 0; i < numGetBooks; i++) {
                    client2.buyBooks(booksToBuy);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });

        Thread storeManagerThread = new Thread(() -> {
            try {
                Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
                booksToAdd.add(new BookCopy(TEST_ISBN, 10));

                for (int i = 0; i < numGetBooks; i++) {
                    storeManager.addCopies(booksToAdd);
                    storeManager.addCopies(booksToAdd);
                }
            } catch (BookStoreException e) {
                e.printStackTrace();
                fail();
            }
        });

        c1Thread.start();
        c2Thread.start();
        storeManagerThread.start();

        c1Thread.join();
        c2Thread.join();
        storeManagerThread.join();

        try {
            List<StockBook> books = storeManager.getBooks();
            for (StockBook book : books) {
                if (book.getISBN() == TEST_ISBN) {
                    assertEquals(NUM_COPIES, book.getNumCopies());
                }
            }
        } catch (BookStoreException e) {
            e.printStackTrace();
            fail();
        }
    }
}
