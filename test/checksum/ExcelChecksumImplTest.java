/**
 *
 */
package edu.dartmouth.geisel.isidro.checksum;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.junit.Test;

import edu.dartmouth.geisel.isidro.read.CsvReader;
import edu.dartmouth.geisel.isidro.util.CsvTest;
import edu.dartmouth.geisel.isidro.util.TestUtils;
import junit.framework.TestCase;

public final class ExcelChecksumImplTest extends TestCase implements CsvTest
{

    @Override
    public void processCsvs(final File file)
    {
        try
        {

            final List<List<String>> csvContents = CsvReader.read(file);

            final Process getSha512Process = Runtime.getRuntime()
                    .exec("sha512sum " + file.getAbsolutePath());
            final BufferedReader input = new BufferedReader(
                    new InputStreamReader(getSha512Process.getInputStream()));

            String output = "";
            for (String line = input.readLine(); line != null; line = input
                    .readLine())
            {
                output += line;
            }
            getSha512Process.waitFor();

            final String correctFp = String.format("%02x",
                    new BigInteger(output.split("\\s+")[0], 16));

            final String fp = ExcelChecksum.checksum(csvContents);

            assertEquals("Checksum should match expected checksum.", correctFp,
                    fp);
        } catch (IOException | InterruptedException
                | NoSuchAlgorithmException e)
        {
            fail(e.getLocalizedMessage());
        }
    }

    /**
     * Test method for
     * {@link edu.dartmouth.geisel.isidro.Checksum.ExcelChecksumImpl#Checksum()}
     * .
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testChecksum() // NOPMD - assert is handled in passed consumer.
            throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        TestUtils.walkCsvsPerformTestAction(this::processCsvs);
    }

    @Test
    public void testNonHappyPath()
    {
        TestUtils.initPrivateConstructor(ExcelChecksum.class);
    }
}
