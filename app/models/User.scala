package models

import utils.silhouette.IdentitySilhouette
import com.mohiva.play.silhouette.impl.util.BCryptPasswordHasher
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api._

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile
import tables.UserTable

case class User(
  id: Long,
  email: String,
  emailConfirmed: Boolean,
  password: String,
  firstName: String,
  lastName: String,
  services: String)
    extends IdentitySilhouette {
  def key = email
  def fullName: String = firstName + " " + lastName
}

object UserServ extends UserTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val services = Seq("A", "B")
  val users = TableQuery[Users]
  val testUsers = scala.collection.mutable.HashMap[Long, User](
    4L -> User(4L, "steven.b.andrews@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Steven", "Andrews", "master"),
    3L -> User(3L, "patrick.eads@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Patrick", "Eads", "master"),
    8L -> User(8L, "m.scottie.eliassen@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Scottie", "Eliassen", "master"),
    5L -> User(5L, "rebecca.faill@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Rebecca", "Faill", "master"),
    6L -> User(6L, "craig.h.ganoe@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Craig", "Ganoe", "master"),
    1L -> User(1L, "john.higgins@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "John", "Higgins", "master"),
    2L -> User(2L, "rodney.jacobson@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Rodney", "Jacobson", "master"),
    7L -> User(7L, "sukdith.punjasthitkul@dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Sukie", "Punjasthitkul", "master")
  )

  def findByEmail(email: String): Future[Option[User]] = {
    db.run(users.filter(_.email === email).result.headOption)
  }

  def save(user: User): Future[User] = {
    val q = users.insertOrUpdate(user)
    db.run(q)
    Future.successful(user)
  }

  def remove(email: String): Future[Unit] = {
    try {
      db.run(users.filter(_.email === email).delete).map(_ => Unit)
    }
    finally db.close
  }
}

