package models

case class DataRequest(id: Int, userId: Long, email: String, title: String, description: String, status: Int, pi: String, phone: String, cphs: String) {
  def statusString = List("New Request", "Awaiting Requirements", "Ready To Send", "Awaiting Download", "Downloaded", "Closed")(status)
}

