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

case class User(id: Long, email: String, emailConfirmed: Boolean, password: String, firstName: String, lastName: String, services: String) extends IdentitySilhouette {
  def key = email
  def fullName: String = firstName + " " + lastName
}

object UserServ extends UserTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val services = Seq("A", "B")
  val users = TableQuery[Users]
  val testUsers = scala.collection.mutable.HashMap[Long, User](
    1L -> User(1L, "feelthebern@isidro.dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password, "Bernie", "Sanders", "master"),
    2L -> User(2L, "hillary@isidro.dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password, "Hillary", "Clinton", "serviceA"),
    3L -> User(3L, "winner@isidro.dartmouth.edu", true, (new BCryptPasswordHasher()).hash("xyzzy123").password, "Donald", "Trump", "serviceB"),
    4L -> User(4L, "aria@gmail.com", true, (new BCryptPasswordHasher()).hash("xyzzy123").password, "Aria", "Stark", "serviceAB")
  )

  def findByEmail(email: String): Future[Option[User]] = {
    db.run(users.filter(_.email === email).result.headOption)
  }

  def save(user: User): Future[User] = {
    val q = users.insertOrUpdate(user)
    println("save user:" + q)
    println("save:" + q.statements)
    println("user:" + user)
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

