package models

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Date
import javax.inject.Inject

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.time.DateUtils

import utils.RandomUtils
import utils.Constants

case class UniqueFile(
  isDeleted: Boolean,
  password: Option[String],
  fileLocation: String,
  uniqueName: String,
  requestId: Int,
  fileName: String,
  dateCreated: java.sql.Date
) {

  /**
   * Checks if this file is more than configured number of days old.
   *
   * @param filesExpiration Age in days when file expires
   * @return if the file has expired.
   */
  def isFileExpired(filesExpiration: Int) = {
    val numDaysAgo = DateUtils.addDays(new java.util.Date, -filesExpiration)
    dateCreated.before(numDaysAgo)
  }

  def delete = {
    val filePath = new File(fileLocation)
    filePath.delete
  }
}

class UniqueFileServ @Inject() (constants: Constants) {
  /**
   * Associates given file with randomly generated unique identifier by copying and storing it in
   * the given storage location and then returning DAO for insertion into database.
   *
   * @param fileName String file path to be stored.
   * @param storeDir String dir path to store file.
   * @param id int request ID to be used as static file name.
   * @return uniquely identified mapping DAO.
   * @throws IOException thrown file I/O fails.
   */
  def generateUniqueFile(inputPath: String, originalFileName: String,
    storeDir: String, id: Int, password: Option[String]) /*throws IOException*/ = {
    val path = Paths.get(storeDir, id + ".xlsx")
    new File(storeDir).mkdirs
    val is = new FileInputStream(inputPath)
    val os = new FileOutputStream(path.toFile)
    FileUtils.copyFile(new File(inputPath), path.toFile)

    val uniqueName = RandomUtils.generateRandomNumberString(constants.RANDOMBITS, constants.RADIX)
    new UniqueFile(false, password, path.toString, uniqueName, id, originalFileName, new java.sql.Date(new java.util.Date().getTime))
  }
}
