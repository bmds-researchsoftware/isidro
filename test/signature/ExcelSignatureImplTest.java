/**
 *
 */
package edu.dartmouth.geisel.isidro.signature;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import edu.dartmouth.geisel.isidro.encrypt.ExcelEncryptImplTest;
import edu.dartmouth.geisel.isidro.read.CsvReader;
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException;
import edu.dartmouth.geisel.isidro.util.CsvTest;
import edu.dartmouth.geisel.isidro.util.TestUtils;
import junit.framework.TestCase;

/**
 * @author Patrick Eads
 */
public class ExcelSignatureImplTest extends TestCase implements CsvTest
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
            }

            ExcelSignature.sign(getClass().getResource("/mykey.p12").getFile(),
                    ExcelEncryptImplTest.PASSWORD, xlsxFilePath);

            try (final OPCPackage pkg = OPCPackage.open(xlsxFilePath,
                    PackageAccess.READ))
            {
                final SignatureConfig sic = new SignatureConfig();
                sic.setOpcPackage(pkg);
                final SignatureInfo si = new SignatureInfo();
                si.setSignatureConfig(sic);
                assertTrue("Signature should be valid.", si.verifySignature());
            }

            final File xlsxFile = new File(xlsxFilePath);
            xlsxFile.delete();
        } catch (SecurityException | IOException | InvalidFormatException
                | GeneralSecurityException | IllegalArgumentException
                | XMLSignatureException | MarshalException
                | DataIntegrityMismatchException e)
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
        TestUtils.initPrivateConstructor(ExcelSignature.class);
    }

    @Test
    public void testSign() throws InvalidFormatException, IOException // NOPMD -
                                                                      // assert
                                                                      // is
                                                                      // handled
                                                                      // in
                                                                      // passed
                                                                      // consumer.
    {
        TestUtils.walkCsvsPerformTestAction(this::processCsvs);
    }
}
