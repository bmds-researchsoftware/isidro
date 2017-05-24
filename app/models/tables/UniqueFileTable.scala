package models.tables

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

import models.UniqueFile

import java.sql.Date

class UniqueFileTable(tag: Tag) extends Table[UniqueFile](tag, "unique_file") {

  def isDeleted = column[Boolean]("isDeleted", O.PrimaryKey, O.AutoInc)
  def password = column[Option[String]]("password")
  def fileLocation = column[String]("fileLocation")
  def uniqueName = column[String]("uniqueName")
  def requestId = column[Int]("requestId")
  def fileName = column[String]("fileName")
  def dateCreated = column[Date]("dateCreated")

  override def * = (isDeleted, password, fileLocation, uniqueName, requestId, fileName, dateCreated) <> (UniqueFile.tupled, UniqueFile.unapply _)
}
