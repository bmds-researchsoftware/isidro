package tables

import models.RequestLog
import slick.driver.JdbcProfile
import java.sql.Timestamp

trait RequestLogTable {
  protected val driver: JdbcProfile
  import driver.api._

  class RequestLogs(tag: Tag) extends Table[RequestLog](tag, "request_log") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def request = column[Int]("requestId")
    def user = column[Long]("userId")
    def text = column[String]("notes")
    def timeMod = column[Timestamp]("timeMod")

    def * = (id, request, user, text, timeMod) <> (RequestLog.tupled, RequestLog.unapply _)
  } 
}
