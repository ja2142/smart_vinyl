package tk.letsplaybol.smartvinyl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tk.letsplaybol.smart_vinyl.CacheDb;

public class TestCacheDb {
    static final String TEST_DIR = "test_dbs";

    @BeforeAll
    static void setUp() {
        File testDir = new File(TEST_DIR);
        if (testDir.exists()) {
            for (File fileInTestDir : testDir.listFiles()) {
                if (fileInTestDir.isFile()) {
                    fileInTestDir.delete();
                }
            }
        } else {
            testDir.mkdir();
        }
    }

    @Test
    void testCreateDb() throws IOException {
        CacheDb db = new CacheDb(TEST_DIR + "/test_create.db");
        assertEquals(false, db.has("nonexistent"), "newly created db shouldn't have any streams");
        assertEquals(0, db.getSize(), "newly created db should have size 0");

        db.close();
    }

    @Test
    void testReadExisting() throws IOException {
        String name = "name - 1234";
        int size = 123;
        CacheDb db = new CacheDb(TEST_DIR + "/test_read.db");
        db.add(name, size);

        assertEquals(false, db.has("nonexistent"), "db shouldn't have streams that weren't added");
        assertEquals(true, db.has(name), "db should have added stream");
        assertEquals(size, db.getSize(), "size should be equal to added stream");

        db.close();

        CacheDb db2 = new CacheDb(TEST_DIR + "/test_read.db");
        assertEquals(false, db2.has("nonexistent"), "opened db shouldn't have streams that weren't added");
        assertEquals(true, db2.has(name), "opened db should have added stream");
        assertEquals(size, db2.getSize(), "opened db size should be equal to added stream");

        db2.close();
    }

    @ParameterizedTest
    @MethodSource("testSizeArguments")
    void testSize(String name, int size) throws IOException {
        CacheDb db = new CacheDb(TEST_DIR + "/test_size.db");
        long sizeBefore = db.getSize();

        db.add(name, size);

        assertEquals(sizeBefore + size, db.getSize(), "size should be equal to added stream");

        db.close();
    }

    static Stream<Arguments> testSizeArguments() {
        return Stream.of(
                Arguments.of("name 0", 10),
                Arguments.of("name 1", 1),
                Arguments.of("name 2", 521789),
                Arguments.of("name 3", 2 << 30));
    }

    @ParameterizedTest
    @MethodSource("testRemoveLRUArguments")
    void testRemoveLRU(Map<String, Integer> entries) throws InterruptedException, IOException {
        try {
            new File(TEST_DIR + "/test_remove.db").delete();
        } catch (IOError e) {
            // for first test, it won't exist
        }

        CacheDb db = new CacheDb(TEST_DIR + "/test_remove.db");

        String firstAdded = addEntriesToDb(entries, db).get(0);

        long sizeBeforeRemove = db.getSize();
        String removed = db.removeLeastRecentlyUsed();

        assertEquals(firstAdded, removed, "return of removeLeastRecentlyUsed should be the first added stream");
        assertEquals(sizeBeforeRemove - entries.get(removed), db.getSize(),
                "db size should shrink by size of removed stream");

        assertEquals(false, db.has(removed), "db shouldn't contain removed stream");

        for (String name : entries.keySet()) {
            if (!name.equals(removed)) {
                assertEquals(true, db.has(name), "db should have all other streams");
            }
        }

        db.close();
    }

    static Stream<Arguments> testRemoveLRUArguments() {
        return Stream.of(
                Arguments.of(new HashMap<String, Integer>() {
                    {
                        put("name 0", 1234);
                    }
                }),
                Arguments.of(new HashMap<String, Integer>() {
                    {
                        put("name 0", 53421);
                        put("name 1", 754);
                    }
                }),
                Arguments.of(new HashMap<String, Integer>() {
                    {
                        put("name 0", 53421);
                        put("name 1", 754);
                        put("name 2", 9851795);
                    }
                }));
    }

    @ParameterizedTest
    @MethodSource("testRemoveLRUArguments")
    void testHasUpdatesLastUsed(Map<String, Integer> entries) throws InterruptedException, IOException {
        try {
            new File(TEST_DIR + "/test_remove.db").delete();
        } catch (IOError e) {
            // for first test, it won't exist
        }

        CacheDb db = new CacheDb(TEST_DIR + "/test_remove.db");

        List<String> addedOrder = addEntriesToDb(entries, db);

        assertTrue(db.has(addedOrder.get(0)));
        // move first added to the end of list, first element should now be lru
        addedOrder.add(addedOrder.remove(0));
        String expectedRemoved = addedOrder.get(0);

        long sizeBeforeRemove = db.getSize();
        String removed = db.removeLeastRecentlyUsed();

        assertEquals(expectedRemoved, removed, "return of removeLeastRecentlyUsed should be the first added stream");
        assertEquals(sizeBeforeRemove - entries.get(removed), db.getSize(),
                "db size should shrink by size of removed stream");

        assertEquals(false, db.has(removed), "db shouldn't contain removed stream");

        for (String name : entries.keySet()) {
            if (!name.equals(removed)) {
                assertEquals(true, db.has(name), "db should have all other streams");
            }
        }

        db.close();
    }

    List<String> addEntriesToDb(Map<String, Integer> entries, CacheDb db) throws InterruptedException {
        List<String> added = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            added.add(entry.getKey());
            db.add(entry.getKey(), entry.getValue());

            // wait for next entry to be newer
            Thread.sleep(2000);
        }
        return added;
    }
}
