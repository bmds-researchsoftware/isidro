package edu.dartmouth.geisel.isidro.watermark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Patrick Eads
 */
public final class ExcelWatermark
{
    /**
     * Private constructor of utilities class.
     */
    private ExcelWatermark()
    {
    }

    /**
     * Apply given watermark image to given workbook on given worksheet.
     *
     * @param workbook
     *            POI workbook object to watermark.
     * @param worksheetName
     *            String worksheet name on which to place watermark.
     * @param pathToImage
     *            String path to watermark image file.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void watermark(final XSSFWorkbook workbook,
            final String worksheetName, final File pathToImage)
            throws FileNotFoundException, IOException
    {
        if (workbook != null)
        {
            final XSSFSheet sheet = workbook.getSheet(worksheetName);

            final InputStream is = new FileInputStream(pathToImage);

            final byte[] bytes = IOUtils.toByteArray(is);
            final int picIndx = workbook.addPicture(bytes,
                    Workbook.PICTURE_TYPE_PNG);

            final CreationHelper helper = workbook.getCreationHelper();

            final Drawing drawing = sheet.createDrawingPatriarch();

            // add a picture
            final ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(1);
            anchor.setRow1(1);
            final Picture pict = drawing.createPicture(anchor, picIndx);
            pict.resize();
            is.close();
        }
    }
}
