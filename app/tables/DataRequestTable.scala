package tables

import models.DataRequest
import slick.driver.JdbcProfile

trait DataRequestTable extends UserTable{
  protected val driver: JdbcProfile
  import driver.api._
  class DataRequests(tag: Tag) extends Table[DataRequest](tag, "request") {

    def id = column[Int]("requestId", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("userId")
    def email = column[String]("email")
    def title = column[String]("title")
    def description = column[String]("description")
    def status = column[Int]("status")
    def pi = column[String]("pi")
    def phone = column[String]("phone")
    def cphs = column[String]("cphs")


    def * = (id, userId, email, title, description, status, pi, phone, cphs) <> (DataRequest.tupled, DataRequest.unapply _)

    def user = foreignKey("USER_FK", userId, users)(_.id)
  }
  val users = TableQuery[Users]

}
