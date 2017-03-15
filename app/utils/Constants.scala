package utils

import play.api.Play
import java.lang.RuntimeException

object Constants {
  lazy val encryptionAlgorithm = Play.current.configuration.getString("encryptionAlgorithm").getOrElse(throw new RuntimeException("Missing encryptAlgorithm in application.conf"))
  lazy val inputPassword = Play.current.configuration.getString("inputPassword").getOrElse(throw new RuntimeException("Missing inputPasswordr in application.conf"))
  lazy val inputSignature = Play.current.configuration.getString("inputSignature").getOrElse(throw new RuntimeException("Missing inputSignature in application.conf"))
  lazy val isidroWorksheetName = Play.current.configuration.getString("isidroWorksheetName").getOrElse(throw new RuntimeException("Missing isidroWorksheetName in application.conf"))
  lazy val storeDir = Play.current.configuration.getString("storage.dir").getOrElse(throw new RuntimeException("Missing storage.dir in application.conf"))
  lazy val outputCsv = Play.current.configuration.getString("outputCsv").getOrElse(throw new RuntimeException("Missing outputCsv in application.conf"))
  lazy val outputDir = Play.current.configuration.getString("outputDir").getOrElse(throw new RuntimeException("Missing outputDir in application.conf"))
  lazy val outputXlsx = Play.current.configuration.getString("outputXlsx").getOrElse(throw new RuntimeException("Missing outputXlsx in application.conf"))
  lazy val watermarkImagePath = Play.current.configuration.getString("watermarkImagePath").getOrElse(throw new RuntimeException("Missing watermarkImagePath in application.conf"))
  lazy val serverName = Play.current.configuration.getString("serverName").getOrElse(throw new RuntimeException("Missing serverName in application.conf"))
  lazy val fileExpiration = Play.current.configuration.getInt("fileExpiration").getOrElse(throw new RuntimeException("Bad or missing fileExpiration in application.conf"))
  val editRequest = 0
  val newRequest = 1
  val awaitingRequirements = 2
  val readyToSend = 3
  val awaitingDownload = 4
  val downloaded = 5
  val closed = 6
}

