package models.services

import javax.inject.Inject

import models.{ DataRequest, RequestRequirement, UniqueFile }

import scala.concurrent.Future
import models.daos.{ RequestDAO, UniqueFileDAO }
/**
 * Handles actions to DataRequests
 */
class RequestService @Inject() (requestDAO: RequestDAO, uniqueFileDAO: UniqueFileDAO) {

  /**
   * Retrieve requests
   *
   * @param closed: If true, returns only closed requests.  If false, returns all but closed requests
   * @return The sequence of retrieved requests
   */
  def retrieve(closed: Boolean = false): Future[Seq[DataRequest]] = requestDAO.find(closed)

  /**
   * Load a request
   *
   * @param rid: The request's id
   * @return The request if it exists
   */
  def load(rid: Int): Future[Option[DataRequest]] = requestDAO.findById(rid)

  /**
   * Insert a request
   *
   * @param req: The request to be inserted
   * @return The id of the inserted request
   */
  def insert(req: DataRequest): Future[Int] = requestDAO.insert(req)

  /**
   * Update a request
   *
   * @param rid: The id of the request to be updated
   * @param req: The values to be updated
   * @return The number of requests updated
   */
  def update(rid: Int, req: DataRequest): Future[Int] = requestDAO.update(rid, req)

  /**
   * Get request requirement list for a request
   *
   * @param rid: The id of the request
   * @return (The Request, List of all Requirement, List of RequestRequirements for this request)
   */
  def getRequirements(rid: Int) = requestDAO.getRequirements(rid)

  /**
   * Delete a request's requirements
   *
   * @param rid: The id of the request to delete
   */
  def deleteRequirements(rid: Int): Unit = requestDAO.deleteRequirements(rid)

  /**
   * Retrieve titles of the given requirements
   *
   * @param reqs: Id's of requested requirements
   * @return The titles of the requested requirements
   */
  def getRequirementTitles(reqs: Iterable[Int]): Future[Seq[String]] = requestDAO.getRequirementTitles(reqs)

  /**
   * Set a request's state
   *
   * @param rid: The id of the request
   * @param state: The request's new State value
   */
  def setState(rid: Int, state: Int): Unit = requestDAO.setState(rid, state)

  /**
   * Add request requirements to the db
   *
   * @param rrs: Request Requirements to be added
   */
  def addRequirements(rrs: Iterable[RequestRequirement]) = requestDAO.addRequirements(rrs)

  /**
   * Get a request's requirement progress
   *
   * @param rid: The id of the request
   * @return (The request, Sequence of (requirement id, completed, requirement title))
   */
  def getRequirementProgress(rid: Int): Future[(Option[DataRequest], Seq[(Int, Boolean, String)])] = requestDAO.getRequirementProgress(rid)

  /**
   * Update a request's requirement progress
   *
   * @param rid: The id of the request
   * @complete: List of requirement id's which have been completed
   */
  def updateProgress(rid: Int, complete: List[Int]): Unit = requestDAO.updateProgress(rid, complete)

  /**
   * Mark a request's Unique File as deleted
   *
   * @param rid: The id of the request
   */
  def setDeleted(rid: Int) = requestDAO.setDeleted(rid)

  /**
   * Retrieves (non-deleted) UniqueFiles for given request
   *
   * @param rid: The id of the request
   * @return Sequence of (UniqueFile, Request)
   */
  def getUniqueFiles(rid: Int): Future[(Seq[UniqueFile], DataRequest)] = requestDAO.getUniqueFiles(rid)

  /**
   * Get a UniqueFile from it's unique name
   *
   * @param uid: The unique name to load
   * @return (The requested UniqueFile, The Unique File's DataRequest)
   */
  def getUniqueFileByName(uid: String): Future[Seq[(UniqueFile, DataRequest)]] = uniqueFileDAO.getByName(uid)

  /**
   * Insert a UniqueFile into the db
   *
   * @param uf: The UniqueFile to insert
   * @return The unique name of the added file
   */
  def insert(uf: UniqueFile): Future[String] = uniqueFileDAO.insert(uf)

  /**
   * Get log entries for a request
   *
   * @param rid: The id of the request
   * @return (Request, Sequence of log messages for given request)
   */
  def getLog(rid: Int) = requestDAO.getLog(rid)

  /**
   * Add a log message to the database
   *
   * @param rid: The id of the request
   * @param user: The signed in user (data broker)
   * @param msg: The log message
   */
  def log(rid: Int, user: String, msg: String) = requestDAO.log(rid, user, msg)

}
