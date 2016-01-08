package tables

import models.DataRequest
import slick.driver.JdbcProfile

trait DataRequestTable extends UserTable{
  protected val driver: JdbcProfile
  import driver.api._
  class DataRequests(tag: Tag) extends Table[DataRequest](tag, "request") {

    def id = column[Int]("requestId", O.PrimaryKey)
    def userId = column[Long]("userId")
    def title = column[String]("requestText")
    def description = column[String]("reasonForRequest")

    def * = (id, userId, title, description) <> (DataRequest.tupled, DataRequest.unapply _)

    def user = foreignKey("USER_FK", userId, users)(_.id)
  }
  val users = TableQuery[Users]

}
