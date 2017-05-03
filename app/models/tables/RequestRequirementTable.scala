package models.tables

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

import models.RequestRequirement

class RequestRequirementTable(tag: Tag) extends Table[RequestRequirement](tag, "request_requirement") {
  def request = column[Int]("requestId")
  def requirement = column[Int]("requirementId")
  def completed = column[Boolean]("completed")

  override def * = (request, requirement, completed) <> (RequestRequirement.tupled, RequestRequirement.unapply _)
}
