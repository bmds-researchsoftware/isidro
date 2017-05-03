package models.tables

import slick.driver.PostgresDriver.api._

import models.DataRequest

class DataRequestTable(tag: Tag) extends Table[DataRequest](tag, "request") {

  def id = column[Int]("requestId", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("userId")
  def email = column[String]("email")
  def title = column[String]("title")
  def description = column[String]("description")
  def status = column[Int]("status")
  def pi = column[String]("pi")
  def phone = column[String]("phone")
  def cphs = column[String]("cphs")

  override def * = (id, userId, email, title, description, status, pi, phone, cphs) <> (DataRequest.tupled, DataRequest.unapply _)
}
