/**
 *
 */
package edu.dartmouth.geisel.isidro.read;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.csveed.api.CsvClient;
import org.csveed.api.CsvClientImpl;
import org.csveed.api.Row;

import edu.dartmouth.geisel.isidro.checksum.ExcelChecksum;

/**
 * @author Patrick Eads
 */
public final class CsvReader
{
    /**
     * Private constructor of utilities class.
     */
    private CsvReader()
    {
    }

    /**
     * Create POI workbook object from given 2D list of Strings.
     *
     * @param worksheetName
     *            String name of worksheet.
     * @param csvContents
     *            2D list of String forming the contents of the workbook.
     * @return POI workbook with contents of 2D list of Strings.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws DataIntegrityMismatchException
     */
    public static XSSFWorkbook getExcelWorkbook(final String worksheetName,
            final List<List<String>> csvContents)
            throws NoSuchAlgorithmException, IOException,
            DataIntegrityMismatchException

    {
        final String csvChecksum = ExcelChecksum.checksum(csvContents);
        final XSSFWorkbook workbook = new XSSFWorkbook();
        final XSSFSheet sheet = workbook.createSheet(worksheetName);
        int rowsCount = 0;
        for (final List<String> row : csvContents)
        {
            final XSSFRow sheetRow = sheet.createRow(rowsCount++);
            int cellsCount = 0;
            for (final String cell : row)
            {
                final Cell sheetCell = sheetRow.createCell(cellsCount++);
                sheetCell.setCellValue(cell);
            }
        }
        final String workbookChecksum = ExcelChecksum
                .checksum(processWorkbook(workbook));

        if (workbookChecksum != null && !workbookChecksum.equals(csvChecksum))
        {
            throw new DataIntegrityMismatchException(
                    formatText("Expected: %s, but got: %s", csvChecksum,
                            workbookChecksum));
        }

        return workbook;
    }

    /**
     * Read a given CSV into a 2D list of Strings.
     *
     * @param file
     *            File to read.
     * @return 2D list of Strings of content from CSV.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static List<List<String>> read(final File file)
            throws FileNotFoundException, IOException
    {
        return processCsv(new FileReader(file));
    }

    /**
     * Read a given CSV into a 2D list of Strings.
     *
     * @param path
     *            String path of file to read.
     * @return 2D list of Strings of content from CSV.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static List<List<String>> read(final String path)
            throws FileNotFoundException, IOException
    {
        return read(new File(path));
    }

    private static String formatText(final String format, final Object... args)
    {
        final Formatter formatter = new Formatter(Locale.getDefault());
        try
        {
            formatter.format(format, args);
            return formatter.toString();
        } finally
        {
            formatter.close();
        }
    }

    /**
     * Method to process each row of CSV into 2D list of Strings.
     *
     * @param reader
     *            Reader object from which to read CSV data.
     * @return 2D list of Strings of content from CSV.
     */
    private static List<List<String>> processCsv(final Reader reader)
    {
        final List<List<String>> result = new LinkedList<>();
        final CsvClient<Row> csvClnt = new CsvClientImpl<Row>(reader)
                .setEscape('"').setSeparator(',').setUseHeader(false);
        final List<Row> rowList = csvClnt.readRows();

        final Iterator<Row> rows = rowList.iterator();

        rows.forEachRemaining((row) ->
        {
            final List<String> curr = new LinkedList<>();
            final Iterator<String> cells = row.iterator();
            cells.forEachRemaining((cell) ->
            {
                curr.add(cell);
            });
            result.add(curr);
        });

        return result;
    }

    /**
     * Read a given XSSF workbook into a 2D list of Strings.
     *
     * @param wb
     *            workbook to read.
     * @return 2D list of Strings of content from workbook.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static List<List<String>> processWorkbook(final Workbook wb)
    {
        final List<List<String>> result = new LinkedList<>();
        final Sheet sheet = wb.getSheetAt(0);
        final Iterator<org.apache.poi.ss.usermodel.Row> rows = sheet
                .rowIterator();

        rows.forEachRemaining((row) ->
        {
            final List<String> curr = new LinkedList<>();
            final Iterator<Cell> cells = row.cellIterator();
            cells.forEachRemaining((cell) ->
            {
                curr.add(cell.getStringCellValue());
            });
            result.add(curr);
        });

        return result;
    }
}
