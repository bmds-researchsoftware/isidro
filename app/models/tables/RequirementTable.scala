package models.tables

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

import models.Requirement

class RequirementTable(tag: Tag) extends Table[Requirement](tag, "requirement") {
  def id = column[Int]("requirementId", O.PrimaryKey)
  def title = column[String]("requirementName")
  def text = column[String]("requirementText")
  def order = column[Int]("requirementOrder")

  override def * = (id, title, text, order) <> (Requirement.tupled, Requirement.unapply _)
}
