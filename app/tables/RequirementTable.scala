package tables

import models.Requirement
import slick.driver.JdbcProfile

trait RequirementTable {
  protected val driver: JdbcProfile
  import driver.api._

  class Requirements(tag: Tag) extends Table[Requirement](tag, "requirement") {

    def id = column[Int]("requirementId", O.PrimaryKey)
    def title = column[String]("requirementName")
    def text = column[String]("requirementText")
    def order = column[Int]("requirementOrder")

    def * = (id, title, text, order) <> (Requirement.tupled, Requirement.unapply _)
  }
}
