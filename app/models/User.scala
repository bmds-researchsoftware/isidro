package models

import java.util.UUID

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

/**
 * The user object.
 *
 * @param userID    The unique ID of the user.
 * @param loginInfo The linked login info.
 * @param firstName Maybe the first name of the authenticated user.
 * @param lastName  Maybe the last name of the authenticated user.
 * @param fullName  Maybe the full name of the authenticated user.
 * @param email     Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 * @param activated Indicates that the user has activated its registration.
 */
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
    1L -> User(1L, "test.user@example.com", true, (new BCryptPasswordHasher()).hash("xyzzy123").password,
      "Test", "User", "master")
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
  }
}
