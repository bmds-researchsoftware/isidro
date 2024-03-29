package utils

import edu.dartmouth.geisel.isidro.checksum.ExcelChecksum
import edu.dartmouth.geisel.isidro.encrypt.ExcelEncrypt
import edu.dartmouth.geisel.isidro.signature.ExcelSignature
import edu.dartmouth.geisel.isidro.watermark.ExcelWatermark
import edu.dartmouth.geisel.isidro.read.CsvReader

import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

import models.UniqueFileServ

import org.apache.poi.poifs.crypt.CipherAlgorithm

class FileUtils @Inject() (constants: Constants, uniqueFileServ: UniqueFileServ) {

  /**
   * Create an xlsx file from csv data.
   *
   * @param xlsxPath    Path to xlsx file to be created
   * @param csvContents Data read from csv file (2D string array)
   * @param watermark   True if the xlsx file should be watermarked.
   */
  type CsvData = java.util.List[java.util.List[String]]
  private def createXlsx(xlsxPath: String, csvContents: CsvData, watermark: Boolean): String = {
    try {
      val workbook = CsvReader.getExcelWorkbook(constants.getString("isidroWorksheetName"), csvContents)
      val os = new FileOutputStream(xlsxPath)
      val rval = if (watermark) {
        ExcelWatermark.watermark(workbook, constants.getString("isidroWorksheetName"), new File(constants.getString("watermarkImagePath")))
        s"Watermark: ${constants.getString("watermarkImagePath")}\n"
      } else {
        ""
      }
      workbook.write(os)
      rval
    } catch {
      case _: Throwable => "Watermark error"
    }
  }

  /**
   * Encrypt xlsx file with a random password
   *
   * @param xlsxPath Path to xlsx file to be encrypted
   * @return The password used for encryption
   */
  private def encryptXlsx(xlsxPath: String) = {
    val pw = RandomUtils.generateRandomNumberString(constants.RANDOMBITS, constants.RADIX);
    ExcelEncrypt.encrypt(xlsxPath, pw, CipherAlgorithm.valueOf(constants.getString("encryptionAlgorithm")));
    pw
  }

  /**
   * Build xlsx file from csv data, with the suppled options.
   *
   * @param rid Id of request for this file
   * @param xlsxPath Path to xlsx file to be created
   * @param csvContents Data read from csv file (2D string array)
   * @param logEntry StringBuilder to append with request log information
   * @param fingerprint Enable fingerprint
   * @param watermark Enable watermark
   * @param signature Enable signature
   * @param encrypt Enable encrytion
   */
  def buildFile(
    rid: Int,
    xlsxPath: String,
    csvContents: CsvData,
    logEntry: StringBuilder,
    fingerprint: Boolean,
    watermark: Boolean,
    signature: Boolean,
    encrypt: Boolean) = {

    // CAUTION: fingerprint, watermark, signature, and encryption
    // order of operation important.
    val checksum = ExcelChecksum.checksum(csvContents)

    if (fingerprint) logEntry.append(s"Fingerprint: $checksum\n")

    logEntry.append(createXlsx(xlsxPath, csvContents, watermark))

    if (signature) {
      ExcelSignature.sign(constants.getString("inputSignature"), constants.getString("inputPassword"), xlsxPath);
      logEntry.append("Signed\n");
    }

    val password: Option[String] = if (encrypt) {
      logEntry.append("Encrypted\n");
      Some(encryptXlsx(xlsxPath))
    } else {
      None
    }

    val uniqueFile = uniqueFileServ.generateUniqueFile(xlsxPath, checksum, constants.getString("storage.dir"), rid, password)
    new File(xlsxPath).delete

    uniqueFile
  }

}
