package models.daos

import javax.inject.Inject
import java.sql.Timestamp
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
import models.{ DataRequest, RequestLog, RequestRequirement, UniqueFile }
import models.daos.RequestDAO._

import utils.Constants

class RequestDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, constants: Constants) {
  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db: JdbcBackend#DatabaseDef = dbConfig.db

  import dbConfig.driver.api._

  def requestQuery(closed: Boolean = false) = {
    for {
      dataRequests <- requests if (dataRequests.status === constants.CLOSED) === closed
    } yield dataRequests
  }

  /**
   * Retrieve requests
   *
   * @param closed: If true, returns only closed requests.  If false, returns all but closed requests
   * @return The sequence of retrieved requests
   */
  def find(closed: Boolean = false): Future[Seq[DataRequest]] = {
    db.run(requestQuery(closed).result).map { rqs =>
      rqs
    }
  }

  /**
   * Load a request
   *
   * @param rid: The request's id
   * @return The request if it exists
   */
  def findById(rid: Int): Future[Option[DataRequest]] = {
    val q = for {
      r <- requests if r.id === rid
    } yield r

    db.run(q.result).map { rqs =>
      rqs.headOption
    }
  }

  /**
   * Insert a request
   *
   * @param req: The request to be inserted
   * @return The id of the inserted request
   */
  def insert(req: DataRequest): Future[Int] = {
    val insertQ = requests returning requests.map(_.id)
    db.run(insertQ += req)
  }

  /**
   * Update a request
   *
   * @param rid: The id of the request to be updated
   * @param req: The values to be updated
   * @return The number of requests updated
   */
  def update(rid: Int, req: DataRequest) = {
    db.run(requests.filter(_.id === rid).update(req))
  }

  /**
   * Get request requirement list for a request
   *
   * @param rid: The id of the request
   * @return (The Request, List of all Requirement, List of RequestRequirements for this request)
   */
  def getRequirements(rid: Int) = {
    val rrq = for {
      rr <- requestRequirements if rr.request === rid
    } yield (rr.requirement)
    db.run(requests.filter(_.id === rid).result.zip(requirements.sortBy(r => r.order).result.zip(rrq.result)))
  }

  /**
   * Delete a request's requirements
   *
   * @param rid: The id of the request to delete
   */
  def deleteRequirements(rid: Int): Unit = {
    Await.result(db.run(requestRequirements.filter(_.request === rid).delete), Duration.Inf)
  }

  /**
   * Retrieve titles of the given requirements
   *
   * @param reqs: Id's of requested requirements
   * @return The titles of the requested requirements
   */
  def getRequirementTitles(reqs: Iterable[Int]): Future[Seq[String]] = {
    val qRequirements = for {
      r <- requirements if r.id inSetBind (reqs)
    } yield (r.title)
    db.run(qRequirements.result)
  }

  /**
   * Set a request's state
   *
   * @param rid: The id of the request
   * @param state: The request's new State value
   */
  def setState(rid: Int, state: Int): Unit = {
    Await.result(db.run(requests.filter(_.id === rid).map(x => (x.status)).update(state)), Duration.Inf)
  }

  /**
   * Add request requirements to the db
   *
   * @param rrs: Request Requirements to be added
   */
  def addRequirements(rrs: Iterable[RequestRequirement]) = {
    db.run(requestRequirements ++= rrs)
  }

  /**
   * Get a request's requirement progress
   *
   * @param rid: The id of the request
   * @return (The request, Sequence of (requirement id, completed, requirement title))
   */
  def getRequirementProgress(rid: Int): Future[(Option[DataRequest], Seq[(Int, Boolean, String)])] = {
    val progressQ = (for {
      rr <- requestRequirements if rr.request === rid
      r <- requirements if r.id === rr.requirement
    } yield (rr.requirement, rr.completed, r.title)).sortBy(_._1)
    db.run(requests.filter(_.id === rid).result.headOption.zip(progressQ.result))
  }

  /**
   * Update a request's requirement progress
   *
   * @param rid: The id of the request
   * @complete: List of requirement id's which have been completed
   */
  def updateProgress(rid: Int, complete: List[Int]): Unit = {
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

  /**
   * Mark a request's Unique File as deleted
   *
   * @param rid: The id of the request
   */
  def setDeleted(rid: Int) = {
    db.run(uniqueFiles.filter(_.requestId === rid).map(x => (x.isDeleted)).update(true))
  }

  /**
   * Retrieves (non-deleted) UniqueFiles for given request
   *
   * @param rid: The id of the request
   * @return UniqueFiles for this request, and the request itself
   */
  def getUniqueFiles(rid: Int): Future[(Seq[UniqueFile], DataRequest)] = {
    val fileq = for {
      uf <- uniqueFiles if (uf.requestId === rid && !uf.isDeleted)
    } yield (uf)
    db.run(fileq.result.zip(requests.filter(_.id === rid).result.head))
  }

  def getLog(rid: Int) = {
    val q = (for {
      (log, u) <- requestLogs.filter(_.request === rid) joinLeft users on (_.user === _.userID)
    } yield (log, u)).sortBy(_._1.time)
    db.run(q.result.zip(requests.filter(_.id === rid).result))
  }

  def log(rid: Int, user: String, msg: String) = {
    val now = new java.util.Date()
    val newLog = RequestLog(0L, rid, user, msg, new Timestamp(now.getTime))
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

