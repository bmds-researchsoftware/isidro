package tables

import models.UniqueFile
import slick.driver.JdbcProfile
import java.sql.Date

trait UniqueFileTable extends DataRequestTable {
/*  implicit def dateConvert(d: java.util.Date) = new java.sql.Timestamp(d.getTime)
  implicit def dateConvert(d: java.sql.Timestamp) = new java.util.Date(d.getTime) */

  protected val driver: JdbcProfile
  import driver.api._
  class UniqueFiles(tag: Tag) extends Table[UniqueFile](tag, "unique_file") {

    def isDeleted = column[Boolean]("isDeleted", O.PrimaryKey, O.AutoInc)
    def password = column[Option[String]]("password")
    def fileLocation = column[String]("fileLocation")
    def uniqueName = column[String]("uniqueName")
    def requestId = column[Int]("requestId")
    def fileName = column[String]("fileName")
    def dateCreated = column[Date]("dateCreated")

    def * = (isDeleted, password, fileLocation, uniqueName, requestId, fileName, dateCreated) <> (UniqueFile.tupled, UniqueFile.unapply _)
  }

}
