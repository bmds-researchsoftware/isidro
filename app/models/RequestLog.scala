package models
import javax.inject.Inject
import java.text.SimpleDateFormat
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import play.api.{ Logger, Play }
import slick.driver.JdbcProfile
import tables.RequestLogTable

case class RequestLog(
  id: Long,
  request: Int,
  user: String,
  text: String,
  time: java.sql.Timestamp) {
  def dateString = {
    val df = new SimpleDateFormat("MM/dd/yyyy hh:mm");
    df.format(time);
  }
}
