package tables

import models.RequestRequirement
import slick.driver.JdbcProfile

trait RequestRequirementTable {
  protected val driver: JdbcProfile
  import driver.api._

  class RequestRequirements(tag: Tag) extends Table[RequestRequirement](tag, "request_requirement") {
    def request = column[Int]("requestId")
    def requirement = column[Int]("requirementId")
    def completed = column[Boolean]("completed")

    def * = (request, requirement, completed) <> (RequestRequirement.tupled, RequestRequirement.unapply _)
  }
}
