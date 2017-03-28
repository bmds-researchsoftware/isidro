/**
 *
 */
package edu.dartmouth.geisel.isidro.watermark;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import edu.dartmouth.geisel.isidro.read.CsvReader;
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException;
import edu.dartmouth.geisel.isidro.util.CsvTest;
import edu.dartmouth.geisel.isidro.util.TestUtils;
import junit.framework.TestCase;

/**
 * @author Patrick Eads
 */
public class ExcelWatermarkImplTest extends TestCase implements CsvTest
{

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

                // watermark the file
                final File testImage = new File(
                        getClass().getResource("/test_image.png").getFile());
                ExcelWatermark.watermark(workbook, TestUtils.WORKSHEET_NAME,
                        testImage);

                // Get image files from spreadsheet. Should only be one.
                byte[] picData = null;
                final List<XSSFPictureData> picturesList = workbook
                        .getAllPictures();
                for (final PictureData pict : picturesList)
                {
                    final String picExtension = pict.suggestFileExtension();

                    if ("png".equals(picExtension))
                    {
                        picData = pict.getData();
                    }

                }

                // compare default output watermark image to image data in
                // spreadsheet.
                if (picData != null)
                {
                    // now compare two pics
                    try (final InputStream is1 = new FileInputStream(testImage);
                            final InputStream is2 = new ByteArrayInputStream(
                                    picData))
                    {

                        for (int is1NB = is1.read(), is2NB = is2.read(); is1
                                .available() != 0
                                && is2.available() != 0; is1NB = is1
                                        .read(), is2NB = is2.read())
                        {
                            assertEquals("Each byte should be the same.", is1NB,
                                    is2NB);
                        }
                    }
                } else
                {
                    fail("Unable to write file.");
                }
            } catch (final NoSuchAlgorithmException e)
            {
                fail(e.getLocalizedMessage());
            }
        } catch (final IOException | DataIntegrityMismatchException e)
        {
            fail(e.getLocalizedMessage());
        } finally
        {
            final File xlsxFile = new File(xlsxFilePath);
            xlsxFile.delete();
        }
    }

    @Test
    public void testNonHappyPath()
    {
        try
        {
            ExcelWatermark.watermark(null, TestUtils.WORKSHEET_NAME, null);
        } catch (final IOException e)
        {
            // an exception is expected
        }
        try
        {
            try (final XSSFWorkbook workbook = CsvReader
                    .getExcelWorkbook(TestUtils.WORKSHEET_NAME, CsvReader.read(
                            getClass().getResource("/test.csv").getPath())))
            {
                ExcelWatermark.watermark(workbook, TestUtils.WORKSHEET_NAME,
                        null);
            }
        } catch (final Exception e)
        {
            // an exception is expected
        }
        try
        {
            final URL url = getClass().getResource("/test.csv");
            try (final XSSFWorkbook workbook = CsvReader.getExcelWorkbook(
                    TestUtils.WORKSHEET_NAME, CsvReader.read(url.getPath())))
            {
                ExcelWatermark.watermark(workbook, TestUtils.WORKSHEET_NAME,
                        new File(url.getPath()));
            }
        } catch (final Exception e)
        {
            // an exception is expected
        }
        TestUtils.initPrivateConstructor(ExcelWatermark.class);
    }

    @Test
    public void testWatermark() // NOPMD - assert is handled in passed consumer.
    {
        TestUtils.walkCsvsPerformTestAction(this::processCsvs);
    }
}
