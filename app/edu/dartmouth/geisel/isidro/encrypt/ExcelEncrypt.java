package edu.dartmouth.geisel.isidro.encrypt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * @author Patrick Eads
 */
public final class ExcelEncrypt
{
    /**
     * Private constructor of utilities class.
     */
    private ExcelEncrypt()
    {
    }

    /**
     * Encrypts the excel file with configurable encryption algorithm.
     *
     * @param xlsxFilePath
     *            String path to excel spreadsheet to encrypt
     * @param password
     *            String password with which to encrypt spreadsheet
     * @throws InvalidFormatException,
     *             {@link GeneralSecurityException},
     *             {@link FileNotFoundException}, {@link IOException}
     */
    public static void encrypt(final String xlsxFilePath, final String password, final CipherAlgorithm algorithm)
            throws InvalidFormatException, GeneralSecurityException,
            FileNotFoundException, IOException
    {

        encrypt(xlsxFilePath, xlsxFilePath, password, algorithm);
    }

    /**
     * Encrypts the excel file with configurable encryption algorithm.
     *
     * @param inputXlsxFilePath
     *            String path to excel spreadsheet to encrypt input
     * @param outputXlsxFilePath
     *            String path to excel spreadsheet to encrypt output
     * @param password
     *            String password with which to encrypt spreadsheet
     * @throws InvalidFormatException,
     *             {@link GeneralSecurityException},
     *             {@link FileNotFoundException}, {@link IOException}
     */
    public static void encrypt(final String inputXlsxFilePath,
            final String outputXlsxFilePath, final String password, final CipherAlgorithm algorithm)
            throws InvalidFormatException, GeneralSecurityException,
            FileNotFoundException, IOException
    {

        final EncryptionInfo info = new EncryptionInfo(EncryptionMode.agile,
        		algorithm, null, -1, -1, null);

        final Encryptor enc = info.getEncryptor();
        enc.confirmPassword(password);

        final POIFSFileSystem fs = new POIFSFileSystem();
        final OPCPackage opc = OPCPackage.open(inputXlsxFilePath,
                PackageAccess.READ_WRITE);
        final OutputStream os = enc.getDataStream(fs);
        opc.save(os);
        opc.close();

        final FileOutputStream fos = new FileOutputStream(outputXlsxFilePath);
        fs.writeFilesystem(fos);
        fos.close();
        fs.close();
    }
}
