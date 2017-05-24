package models

case class DataRequest(
  id: Int,
  email: String,
  title: String,
  description: String,
  status: Int,
  pi: String,
  phone: String,
  cphs: String) {
  def statusString = List("Edit Request", "New Request", "Awaiting Requirements", "Ready To Send", "Awaiting Download", "Downloaded", "Closed")(status)
  def logString = s"New Request\nTitle: $title\nEmail: $email\nPhone: $phone\nCPHS #: $cphs\nPI: $pi\nDescription: $description\n"
}

