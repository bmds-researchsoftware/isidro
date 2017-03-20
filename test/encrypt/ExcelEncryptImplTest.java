/**
 *
 */
package edu.dartmouth.geisel.isidro.encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import edu.dartmouth.geisel.isidro.read.CsvReader;
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException;
import edu.dartmouth.geisel.isidro.read.ExcelReader;
import edu.dartmouth.geisel.isidro.util.CsvTest;
import edu.dartmouth.geisel.isidro.util.TestUtils;
import junit.framework.TestCase;

/**
 * @author Patrick Eads
 */
public class ExcelEncryptImplTest extends TestCase implements CsvTest
{
    public static final String PASSWORD = "xyzzy123";

    @Override
    public void processCsvs(final File file)
    {
        final String path = file.getAbsolutePath();
        final String xlsxFilePath = FilenameUtils.concat(
                FilenameUtils.getFullPath(path),
                FilenameUtils.getBaseName(path));
        try
        {
            final List<List<String>> csvContents = CsvReader.read(file);

            try (final XSSFWorkbook workbook = CsvReader
                    .getExcelWorkbook(TestUtils.WORKSHEET_NAME, csvContents))
            {
                TestUtils.generateExcelFile(workbook, xlsxFilePath);
            }

            ExcelEncrypt.encrypt(xlsxFilePath, PASSWORD, CipherAlgorithm.aes256);

            final POIFSFileSystem fs = new POIFSFileSystem(
                    new FileInputStream(xlsxFilePath));
            final EncryptionInfo info = new EncryptionInfo(fs);
            final Decryptor dec = info.getDecryptor();
            dec.verifyPassword(PASSWORD);

            try (final InputStream is = dec.getDataStream(fs))
            {

                final Method createWorkbook = ExcelReader.class
                        .getDeclaredMethod("createWorkbook", InputStream.class);
                final Method processWorkbook = ExcelReader.class
                        .getDeclaredMethod("processWorkbook", Workbook.class);

                createWorkbook.setAccessible(true);
                processWorkbook.setAccessible(true);

                // Suppression ok because compile-time erasure of datatype on
                // generics creates difficulty in checking type. Plus, if it's
                // wrong, an exception gets thrown failing the test anyway.
                @SuppressWarnings("unchecked")
                final List<List<String>> contents = (List<List<String>>) processWorkbook
                        .invoke(null, createWorkbook.invoke(null, is));

                processWorkbook.setAccessible(false);
                createWorkbook.setAccessible(false);

                assertEquals(
                        "Decrypted contents processed should match originally input contents.",
                        csvContents, contents);
            }

            final File xlsxFile = new File(xlsxFilePath);
            xlsxFile.delete();
        } catch (NoSuchMethodException | SecurityException | IOException
                | InvalidFormatException | GeneralSecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | DataIntegrityMismatchException e)
        {
            fail(e.getLocalizedMessage());
        } finally
        {
            final File xlsxFile = new File(xlsxFilePath);
            xlsxFile.delete();
        }
    }

    /**
     * Test method for
     * {@link edu.dartmouth.geisel.isidro.encrypt.ExcelEncrypt#encrypt()}.
     */
    @Test
    public void testEncrypt() // NOPMD - assert is handled in passed consumer.
    {
        TestUtils.walkCsvsPerformTestAction(this::processCsvs);
    }

    @Test
    public void testNonHappyPath()
    {
        TestUtils.initPrivateConstructor(ExcelEncrypt.class);
    }

}
