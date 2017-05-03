package utils

import play.api.Play
import java.lang.RuntimeException
import java.time.LocalDate

object Constants {
  def getString(key: String) = Play.current.configuration.getString(key)
    .getOrElse(throw new RuntimeException(s"Bad or missing $key in application.conf"))
  def getInt(key: String) = Play.current.configuration.getInt(key)
    .getOrElse(throw new RuntimeException(s"Bad or missing $key in application.conf"))

  val EDITREQUEST = 0
  val NEWREQUEST = 1
  val AWAITINGREQUIREMENTS = 2
  val READYTOSEND = 3
  val AWAITINGDOWNLOAD = 4
  val DOWNLOADED = 5
  val CLOSED = 6
  val RANDOMBITS = 64
  val RADIX = 32
  val DAYHOURS = 24
  val THISYEAR = LocalDate.now().getYear
}

