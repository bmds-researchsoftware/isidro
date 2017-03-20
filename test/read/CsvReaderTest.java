/**
 *
 */
package edu.dartmouth.geisel.isidro.read;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.dartmouth.geisel.isidro.util.TestUtils;
import junit.framework.TestCase;

/**
 * @author Patrick Eads
 */
public class CsvReaderTest extends TestCase
{
    public static final List<List<String>> CSV_TEST_LIST = Arrays.asList(
            Arrays.asList("some", "really", "stupid", "csv", "file"),
            Arrays.asList("that", "I", "would", "never", "truly"));

    @Test
    public void testNonHappyPath()
    {
        final Class<?> clazz = CsvReader.class;
        TestUtils.initPrivateConstructor(clazz);
        final Class<?>[] params = { String.class, Object[].class };
        final Object[] args1 = { "%s", new Object[] {} };
        final Object[] args2 = { "%s", new Object[] { "test" } };
        TestUtils.invokePrivateStaticMethod(clazz, "formatText", params, args1);
        TestUtils.invokePrivateStaticMethod(clazz, "formatText", params, args2);
        new DataIntegrityMismatchException("test");
    }

    /**
     * Test method for
     * {@link edu.dartmouth.geisel.isidro.read.CsvReader#read(java.lang.String)}
     * .
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testRead() throws FileNotFoundException, IOException
    {

        final List<List<String>> csvContents = CsvReader
                .read(getClass().getResource("/test.csv").getFile());

        assertEquals("Contents of CSV should match expected.", CSV_TEST_LIST,
                csvContents);
    }

}
