package edu.dartmouth.geisel.isidro.read;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public final class ExcelReader
{

    public static List<List<String>> read(final File file)
            throws FileNotFoundException, IOException
    {
        return processWorkbook(createWorkbook(new FileInputStream(file)));
    }

    public static List<List<String>> read(final String path)
            throws FileNotFoundException, IOException
    {
        return read(new File(path));
    }

    private static Workbook createWorkbook(final InputStream is)
            throws FileNotFoundException, IOException
    {
        try
        {
            final Workbook wb = WorkbookFactory.create(is);
            return wb;
        } catch (EncryptedDocumentException | InvalidFormatException e)
        {
            throw new IOException(e);
        }
    }

    private static List<List<String>> processWorkbook(final Workbook wb)
    {
        final List<List<String>> result = new LinkedList<>();
        final Sheet sheet = wb.getSheetAt(0);
        final Iterator<Row> rows = sheet.rowIterator();

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
