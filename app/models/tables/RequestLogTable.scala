package models.tables

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp

import models.RequestLog

class RequestLogTable(tag: Tag) extends Table[RequestLog](tag, "request_log") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def request = column[Int]("requestId")
  def user = column[String]("userId")
  def text = column[String]("notes")
  def time = column[Timestamp]("timeMod")

  def * = (id, request, user, text, time) <> (RequestLog.tupled, RequestLog.unapply _)
}
