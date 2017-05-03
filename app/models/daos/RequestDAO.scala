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

import models.tables.{ DataRequestTable, RequestLogTable, RequestRequirementTable, RequirementTable, UniqueFileTable, UserTable }
import models.tables.{ DbPasswordInfo, PasswordInfoTable }
import models.{ DataRequest, RequestLog, RequestRequirement }
import models.daos.RequestDAO._

import utils.Constants

class RequestDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, constants: Constants) {
  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db: JdbcBackend#DatabaseDef = dbConfig.db

  import dbConfig.driver.api._

  def requestQuery(closed: Boolean = false) = { //: Query[DataRequestTable, DataRequest, Seq] = {
    for {
      dataRequests <- requests if (dataRequests.status === constants.CLOSED) === closed
    } yield dataRequests
  }

  def find(closed: Boolean = false): Future[Seq[DataRequest]] = {
    db.run(requestQuery(closed).result).map { rqs =>
      rqs
    }
  }

  def findById(rid: Int): Future[Option[DataRequest]] = {
    val q = for {
      r <- requests if r.id === rid
    } yield r

    db.run(q.result).map { rqs =>
      rqs.headOption
    }
  }

  def insert(req: DataRequest): Future[Int] = {
    val insertQ = requests returning requests.map(_.id)
    return db.run(insertQ += req)
  }

  def update(rid: Int, req: DataRequest) = {
    db.run(requests.filter(_.id === rid).update(req))
  }

  def getRequirements(rid: Int) = {
    val rrq = for {
      rr <- requestRequirements if rr.request === rid
    } yield (rr.requirement)
    db.run(requests.filter(_.id === rid).result.zip(requirements.sortBy(r => r.order).result.zip(rrq.result)))
  }

  def deleteRequirements(rid: Int) = {
    Await.result(db.run(requestRequirements.filter(_.request === rid).delete), Duration.Inf)
  }

  def getRequirementTitles(reqs: Iterable[Int]) = {
    val qRequirements = for {
      r <- requirements if r.id inSetBind (reqs)
    } yield (r.title)
    db.run(qRequirements.result)
  }

  def setState(rid: Int, state: Int) = {
    Await.result(db.run(requests.filter(_.id === rid).map(x => (x.status)).update(state)), Duration.Inf)
  }

  def addRequirements(rrs: Iterable[RequestRequirement]) = {
    db.run(requestRequirements ++= rrs)
  }

  def getRequirementProgress(rid: Int) = {
    val q3 = for {
      rr <- requestRequirements if rr.request === rid
      r <- requirements if r.id === rr.requirement
    } yield (rr.requirement, rr.completed, r.title)
    db.run(requests.filter(_.id === rid).result.zip(q3.result))
  }

  def updateProgress(rid: Int, complete: List[Int]) = {
    // Update db for all incomplete requirements for this request
    val qIncomplete = for {
      c <- requestRequirements if c.request === rid if !c.requirement.inSetBind(complete)
    } yield c.completed
    Await.result(db.run(qIncomplete.update(false)), Duration.Inf)

    // Update db for all complete requirements for this request
    val qComplete = for {
      c <- requestRequirements if c.request === rid if c.requirement inSetBind complete
    } yield c.completed
    Await.result(db.run(qComplete.update(true)), Duration.Inf)
  }

  def setDeleted(rid: Int) = {
    db.run(uniqueFiles.filter(_.requestId === rid).map(x => (x.isDeleted)).update(true))
  }

  def getUniqueFiles(rid: Int) = {
    val fileq = for {
      uf <- uniqueFiles if (uf.requestId === rid && !uf.isDeleted)
      r <- requests if uf.requestId === r.id
    } yield (uf, r)
    println(s"${fileq.result.statements}")
    db.run(fileq.result)
  }

  def getLog(rid: Int) = {
    val q = for {
      (log, u) <- requestLogs.filter(_.request === rid) joinLeft users on (_.user === _.userID)
    } yield (log, u)
    db.run(q.result.zip(requests.filter(_.id === rid).result))
  }

  def log(rid: Int, user: String, msg: String) = {
    val now = new java.util.Date()
    val newLog = RequestLog(0L, rid, user, msg, new java.sql.Timestamp(now.getTime))
    db.run(requestLogs += newLog)
  }
}

object RequestDAO {
  private val requests = TableQuery[DataRequestTable]
  private val requestRequirements = TableQuery[RequestRequirementTable]
  private val requirements = TableQuery[RequirementTable]
  private val uniqueFiles = TableQuery[UniqueFileTable]
  private val requestLogs = TableQuery[RequestLogTable]
  private val users = TableQuery[UserTable]
}

