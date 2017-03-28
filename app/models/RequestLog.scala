package models
import java.text.SimpleDateFormat
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import play.api.{Logger, Play}
import slick.driver.JdbcProfile
import tables.RequestLogTable

case class RequestLog(
  id: Long,
  request: Int,
  user: Long,
  text: String,
  time: java.sql.Timestamp) {
  def dateString = {
    val df = new SimpleDateFormat("MM/dd/yyyy hh:mm");
    df.format(time);
  }
}

object RequestLogService extends RequestLogTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val logs = TableQuery[RequestLogs]

  def log(rid: Int, user: Long, log: String) {
    Logger.debug(s"Log: $rid $user $log")
    db.run(logs += RequestLog(0L, rid, user, log, new java.sql.Timestamp(new java.util.Date().getTime)))
  }
}

