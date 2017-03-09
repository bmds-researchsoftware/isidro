package utils

import play.api.Play

object Constants {
  val outputDir = Play.current.configuration.getString("outputDir").getOrElse("missing")
  val outputCsv = Play.current.configuration.getString("outputCsv").getOrElse("missing")
  val outputXlsx = Play.current.configuration.getString("outputXlsx").getOrElse("missing")
  val isidroWorksheetName = Play.current.configuration.getString("isidroWorksheetName").getOrElse("missing")
  val watermarkImagePath = Play.current.configuration.getString("watermarkImagePath").getOrElse("missing")
  val inputSignature = Play.current.configuration.getString("inputSignature").getOrElse("missing")
  val inputPassword = Play.current.configuration.getString("inputPassword").getOrElse("missing")
  val encryptionAlgorithm = Play.current.configuration.getString("encryptionAlgorithm").getOrElse("missing")
}
