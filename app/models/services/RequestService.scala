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
   * Retrieves requests
   *
   * @param closed
   * @return The list of retrieved requests
   */
  def retrieve(closed: Boolean = false): Future[Seq[DataRequest]] = requestDAO.find(closed)

  def load(rid: Int): Future[Option[DataRequest]] = requestDAO.findById(rid)

  def insert(req: DataRequest): Future[Int] = requestDAO.insert(req)

  def update(rid: Int, req: DataRequest) = requestDAO.update(rid, req)

  def getRequirements(rid: Int) = requestDAO.getRequirements(rid)

  def deleteRequirements(rid: Int) = requestDAO.deleteRequirements(rid)

  def getRequirementTitles(reqs: Iterable[Int]) = requestDAO.getRequirementTitles(reqs)

  def setState(rid: Int, state: Int) = requestDAO.setState(rid, state)

  def addRequirements(rrs: Iterable[RequestRequirement]) = requestDAO.addRequirements(rrs)

  def getRequirementProgress(rid: Int) = requestDAO.getRequirementProgress(rid)

  def updateProgress(rid: Int, complete: List[Int]) = requestDAO.updateProgress(rid, complete)

  def setDeleted(rid: Int) = requestDAO.setDeleted(rid)

  def getUniqueFiles(rid: Int) = requestDAO.getUniqueFiles(rid)

  def getUniqueFileByName(uid: String) = uniqueFileDAO.getByName(uid)

  def insert(uf: UniqueFile) = uniqueFileDAO.insert(uf)

  def getLog(rid: Int) = requestDAO.getLog(rid)

  def log(rid: Int, user: String, msg: String) = requestDAO.log(rid, user, msg)

}
