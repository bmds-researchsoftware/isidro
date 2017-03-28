package tables

import models.User
import slick.driver.JdbcProfile

trait UserTable {
  protected val driver: JdbcProfile
  import driver.api._
  class Users(tag: Tag) extends Table[User](tag, "isidro_user") {
    def id = column[Long]("userId", O.PrimaryKey)
    def email = column[String]("email")
    def emailConfirmed = column[Boolean]("emailConfirmed")
    def password = column[String]("password")
    def firstName = column[String]("firstName")
    def lastName = column[String]("lastName")
    def services = column[String]("services")

    def * = (id, email, emailConfirmed, password, firstName, lastName, services) <> (User.tupled, User.unapply _)
  }
}
