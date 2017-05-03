package models.daos

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend
import slick.lifted.TableQuery

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.tables.{ DataRequestTable, UniqueFileTable }
import models.UniqueFile
import models.daos.UniqueFileDAO._

class UniqueFileDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db: JdbcBackend#DatabaseDef = dbConfig.db

  import dbConfig.driver.api._

  def insert(uf: UniqueFile): Future[String] = {
    val insertQ = uniqueFiles returning uniqueFiles.map(_.uniqueName)
    db.run(insertQ += uf)
  }

  def getByName(uid: String) = {
    val q = for {
      uf <- uniqueFiles if uf.uniqueName === uid
      r <- requests if uf.requestId === r.id
    } yield (uf, r)
    db.run(q.result)
  }
}

object UniqueFileDAO {
  private val uniqueFiles = TableQuery[UniqueFileTable]
  private val requests = TableQuery[DataRequestTable]
}

