package controllers

import edu.dartmouth.geisel.isidro.checksum.ExcelChecksum
import edu.dartmouth.geisel.isidro.encrypt.ExcelEncrypt
import edu.dartmouth.geisel.isidro.read.CsvReader
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException
import edu.dartmouth.geisel.isidro.signature.ExcelSignature
import edu.dartmouth.geisel.isidro.watermark.ExcelWatermark
import edu.dartmouth.isidro.util.TextUtils
import models.{DataRequest, RequestLogService, RequestRequirement, UniqueFileServ}
import tables.{DataRequestTable, RequirementTable, RequestLogTable, RequestRequirementTable, UniqueFileTable}
import utils.Constants
import utils.MailService
import utils.Mailer
import utils.RandomUtils
import utils.silhouette._
import views.html.{ auth => viewsAuth }

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Paths
import javax.inject.Inject
import javax.mail.MessagingException
import org.apache.poi.poifs.crypt.CipherAlgorithm
import play.api.Logger
import play.api.Play
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{email, mapping, ignored, nonEmptyText, text}
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.i18n.{ MessagesApi, Messages, Lang, I18nSupport }
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Action
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import slick.driver.JdbcProfile

trait Tables extends DataRequestTable with RequirementTable with RequestRequirementTable with UniqueFileTable with RequestLogTable

class Isidro @Inject() (val env: AuthenticationEnvironment, val messagesApi: MessagesApi, val mailService: MailService)
    extends AuthenticationController with I18nSupport with Tables with HasDatabaseConfig[JdbcProfile] {
  implicit val ms = mailService

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val dataRequests = TableQuery[DataRequests]
  val uniqueFiles = TableQuery[UniqueFiles]
  val requestRequirements = TableQuery[RequestRequirements]
  val requirements = TableQuery[Requirements]
  val requestLogs = TableQuery[RequestLogs]

  val newRequestForm = Form(mapping(
    "id" -> ignored(0),
    "userId" -> ignored(0L),
    "email" -> email,
    "title" -> nonEmptyText,
    "description" -> nonEmptyText,
    "status" -> ignored(0),
    "pi" -> text,
    "phone" -> text,
    "cphs" -> text)
    (DataRequest.apply _ )(DataRequest.unapply _))

  def index = UserAwareAction { implicit request =>
    Ok(views.html.index())
  }

  /*def myAccount = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.myAccount()))
  }*/

  def newRequest = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.request.newRequest(newRequestForm)))
  }

  def requests = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.status =!= Constants.CLOSED).result).map(req =>
      Ok(views.html.brokerRequests(req.toList)))
  }

  def closedRequests = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.status === Constants.CLOSED).result).map(req =>
      Ok(views.html.brokerRequests(req.toList, true)))
  }

  def viewLog(rid: Int) = SecuredAction.async { implicit request =>
    val q = for {
      log <- requestLogs if log.request === rid
      u <- users if u.id === log.user
    } yield (log, u)
    db.run(q.result.zip(dataRequests.filter(_.id === rid).result)).map(logs => {
      Ok(views.html.request.viewLog(logs._2.head, logs._1.toList))
    })
  }

  def editRequest(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      Ok(views.html.request.editRequest(rid, newRequestForm.fill(req.head)))
    })
  }

  def editRequirements(rid: Int) = SecuredAction.async { implicit request =>
    val q3 = for {
      rr <- requestRequirements if rr.request === rid
    } yield (rr.requirement)

    db.run(dataRequests.filter(_.id === rid).result.zip(requirements.sortBy(r => r.order).result.zip(q3.result))).map(req => {
      req._1.headOption match {
        case Some(theRequest) => {
          Ok(views.html.request.editRequirements(theRequest, req._2._1.toList, req._2._2.toList))
        }
        case _ => Redirect(routes.Isidro.requests)
      }
    })
  }

  def editProgress(rid: Int) = SecuredAction.async { implicit request =>
    val q3 = for {
      rr <- requestRequirements if rr.request === rid
      r <- requirements if r.id === rr.requirement
    } yield (rr.requirement, rr.completed, r.title)

    db.run(dataRequests.filter(_.id === rid).result.zip(q3.result)).map(req => {
      req._1.headOption match {
        case Some(theRequest) => {
          Ok(views.html.request.trackRequirements(theRequest, req._2.toList))
        }
        case _ => Redirect(routes.Isidro.requests)
      }
    })
  }

  def sendFile(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          Ok(views.html.request.sendFile(theRequest))
        }
        case _ => Redirect(routes.Isidro.requests)
      }
    })
  }

  def editAwaitingDownload(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          Ok(views.html.request.editAwaiting(theRequest))
        }
        case _ => Redirect(routes.Isidro.requests)
      }
    })
  }

  def editDownloaded(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          Ok(views.html.request.editDownloaded(theRequest))
        }
        case _ => Redirect(routes.Isidro.requests)
      }
    })
  }

  def handleProgress(rid: Int) = SecuredAction { implicit request =>
    val complete = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt).toList

    // Update db for all incomplete requirements for this request
    val qIncomplete = for {
      c <- requestRequirements if c.request === rid if !c.requirement.inSetBind(complete)
    } yield c.completed
    db.run(qIncomplete.update(false)).map(rows => {
      // Update request state based on whether any requirements are left incomplete
      val qStatus = for {
        rr <- requestRequirements if rr.request === rid
        requirement <- requirements if requirement.id === rr.requirement
      } yield (requirement.title, rr.completed)

      db.run(qStatus.result).map(rrs => {
        val rrList = rrs.toList
        val complete = rrList.filter(_._2).map(_._1).mkString("\n")
        val incomplete = rrList.filter(!_._2).map(_._1).mkString("\n")
        RequestLogService.log(rid, request.identity.id, s"Requirement progress updated\nComplete:\n$complete\nIncomplete:\n$incomplete")
      })
      db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(if (rows>0) Constants.AWAITINGREQUIREMENTS else Constants.READYTOSEND))
    })

    // Update db for all complete requirements for this request
    val qComplete = for {
      c <- requestRequirements if c.request === rid if c.requirement inSetBind complete
    } yield c.completed
    db.run(qComplete.update(true))

    Redirect(routes.Isidro.requests)
  }

  def handleRequirements(rid: Int) = SecuredAction.async { implicit request =>
    val reqs = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt)
    val rrs = reqs.map(new RequestRequirement(rid, _))
    Await.result(db.run(requestRequirements.filter(_.request === rid).delete), Duration.Inf)
    RequestLogService.log(rid, request.identity.id, "Requirements edited.")
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.AWAITINGREQUIREMENTS))
    db.run(requestRequirements ++= rrs).map(_ => Redirect(routes.Isidro.requests))
  }

  def handleNewRequest = handleEditRequest(-1)

  def handleEditRequest(rid: Int) = SecuredAction.async { implicit request =>
    newRequestForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.request.newRequest(formWithErrors))),
      req => {
        if (rid < 0) { // todo: change this to Option
          val fullReq = req.copy(userId = request.identity.id, status = Constants.NEWREQUEST)
          val insertQ = dataRequests returning dataRequests.map(_.id)
          db.run(insertQ += fullReq).map(newId => {
            RequestLogService.log(newId, request.identity.id, fullReq.logString)
            Redirect(routes.Isidro.requests)
          })
        } else {
          val fullReq = req.copy(id = rid, userId = request.identity.id, status = Constants.NEWREQUEST)
          RequestLogService.log(rid, request.identity.id, s"Request edited:\n${fullReq.logString}")
          db.run(dataRequests.filter(_.id === rid).update(fullReq)).map(_ => Redirect(routes.Isidro.requests))
        }
      }
    )
  }

  def handleWithdrawFile(rid: Int) = SecuredAction.async { implicit request =>
    val fileq = for {
      uf <- uniqueFiles if uf.requestId === rid
      r <- dataRequests if uf.requestId === r.id
    } yield (uf, r)
    db.run(fileq.result).map(res => res.headOption match {
      case Some((uf, req)) => {
        uf.delete
        db.run(uniqueFiles.filter(_.requestId === rid).map(x => (x.isDeleted)).update(true))
        db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.READYTOSEND))
        RequestLogService.log(rid, request.identity.id, s"Data file ${uf.fileLocation} withdrawn and deleted.")
        Redirect(routes.Isidro.requests)
      }
      case _ => {
        Logger.error(s"no file for request: $rid")
        Redirect(routes.Isidro.requests)
      }
    })
  }

  def handleClose(rid: Int) = SecuredAction.async { implicit request =>
    RequestLogService.log(rid, request.identity.id, "Close request")
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.CLOSED)).map(_ => Redirect(routes.Isidro.requests))
  }

  def downloadFile(uniqueName: String) = Action.async {
    val fileq = for {
      uf <- uniqueFiles if uf.uniqueName === uniqueName
      r <- dataRequests if uf.requestId === r.id
    } yield (uf, r)
    db.run(fileq.result).map(res => res.headOption match {
      case Some((uf, req)) => {
        val isFileExpired = uf.isFileExpired(Constants.fileExpiration)
        val filePath = new File(uf.fileLocation)
        val fileName = uf.fileName

        if (uf.isFileExpired(Constants.fileExpiration)) {
          Logger.debug(s"""Data file "${uf.fileLocation}" expired and deleted.""")
          Redirect(routes.Isidro.index)
        } else if (!filePath.exists || uf.isDeleted) {
          Logger.debug(s"""Data file "${uf.fileLocation}" does not exist on disk.""")
          Redirect(routes.Isidro.index)
        } else {
          val password = uf.password
          val fileToServe = TemporaryFile(filePath)
          if (password.isDefined) {
            Mailer.sendPasswordEmail(req.email, password.get)
          }
          db.run(dataRequests.filter(_.id === req.id).map(x => (x.status)).update(Constants.DOWNLOADED))
          RequestLogService.log(req.id, 1, "File downloaded and deleted") // TODO No user
          Ok.sendFile(fileToServe.file, onClose = () => { fileToServe.clean })
        }
      }
      case _ => {
        Logger.error(s"no file: $uniqueName")
        Redirect(routes.Isidro.index)
      }
    })
  }

  def download(uid: String) = UserAwareAction.async { implicit request =>
    val q = for {
      uf <- uniqueFiles if uf.uniqueName === uid
      r <- dataRequests if uf.requestId === r.id
    } yield (uf, r)

    db.run(q.result).map { res => res.headOption match {
      case Some((uf, r)) => {
        Ok(views.html.downloads.download(uid))
      }
      case _ => {
        Logger.error(s"Data file not found: $uid")
        Redirect(routes.Isidro.index)
      }
    }}
  }

  type CsvData = java.util.List[java.util.List[String]]
  private def createXlsx(xlsxPath: String, csvContents: CsvData, watermark: Boolean) = {
    try {
      val workbook = CsvReader.getExcelWorkbook(Constants.isidroWorksheetName, csvContents)
      if (watermark) {
        ExcelWatermark.watermark(workbook, Constants.isidroWorksheetName, new File(Constants.watermarkImagePath))
        s"Watermark: ${Constants.watermarkImagePath}\n"
      }
      val os = new FileOutputStream(xlsxPath)
      workbook.write(os)
    } catch {
      case _:Throwable => "Watermark error"
    }
  }


  private def encryptXlsx(xlsxPath: String) = {
    val pw = RandomUtils.generateRandomNumberString(Constants.RANDOMBITS, Constants.RADIX);
    ExcelEncrypt.encrypt(xlsxPath, pw, CipherAlgorithm.valueOf(Constants.encryptionAlgorithm));
    pw
  }

  private def buildFile(
    rid: Int,
    xlsxPath: String,
    csvContents: CsvData,
    logEntry: StringBuilder,
    fingerprint: Boolean,
    watermark: Boolean,
    signature: Boolean,
    encrypt: Boolean) = {

    // CAUTION: fingerprint, watermark, signature, and encryption
    // order of operation important.
    val checksum = ExcelChecksum.checksum(csvContents)

    if (fingerprint) logEntry.append(s"Fingerprint: $checksum\n")

    logEntry.append(createXlsx(xlsxPath, csvContents, watermark))

    if (signature) {
      ExcelSignature.sign(Constants.inputSignature, Constants.inputPassword, xlsxPath);
      logEntry.append("Signed\n");
    }

    val password:Option[String] = if (encrypt) {
      logEntry.append("Encrypted\n");
      Some(encryptXlsx(xlsxPath))
    } else {
      None
    }

    val uniqueFile = UniqueFileServ.generateUniqueFile(xlsxPath, checksum, Constants.storeDir, rid, password)
    new File(xlsxPath).delete

    uniqueFile
  }

  def handleFileUpload(rid: Int) = SecuredAction(parse.multipartFormData) { implicit request =>
    val logEntry = new StringBuilder()

    val params = request.body.asFormUrlEncoded

    request.body.file("dataFile").map { dataFile =>
      val filesPath = Paths.get(Constants.outputDir, rid.toString).toString
      val csvPath = Paths.get(filesPath, Constants.outputCsv).toString
      val csvFile = new File(csvPath)
      val xlsxPath = Paths.get(filesPath, Constants.outputXlsx).toString
      val filesDir = new File(filesPath)
      filesDir.mkdirs

      dataFile.ref.moveTo(csvFile)

      val csvContents = CsvReader.read(csvPath)
      csvFile.delete()


      val uniqueFile = buildFile(rid, xlsxPath, csvContents, logEntry,
        params.contains("checksum"),
        params.contains("watermark"),
        params.contains("signature"),
        params.contains("encrypt"))

      filesDir.delete

      db.run(uniqueFiles += uniqueFile)

      logEntry.append(s"File: ${uniqueFile.fileLocation}\n")
      if (params.contains("notes")) {
        logEntry.append(s"---Notes---\n${params("notes").head}\n")
      }
      logEntry.append("---PHI---\n")
      params.filter(_._1.startsWith("phi")).map(p => logEntry.append(s"${p._2.head}\n"))
      if (params.contains("other") && (params.get("other") != None)) {
        logEntry.append(s"Other phi: ${params.get("other").get.head}\n")
      }
      sendDownloadEmail(rid, uniqueFile.uniqueName, request.identity.id)
      RequestLogService.log(rid, request.identity.id, s"Send download link:\n${logEntry.toString().trim()}")
      db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.AWAITINGDOWNLOAD))
    }.getOrElse {
      Redirect(routes.Isidro.sendFile(rid)).flashing("error" -> "Missing file")
    }

    Redirect(routes.Isidro.requests)
  }


  /**
   * Send an email with the data file download link.
   *
   * @param request The request
   * @param uniqueName Data file identifier
   * @throws MessagingException If thrown by mailHandler
   * @throws MalformedURLException If resulting URL is malformed
   */
  @throws(classOf[MessagingException])
  @throws(classOf[MalformedURLException])
  private def sendDownloadEmail(rid: Int, uniqueName: String, uid: Long):Unit = {
    val q = for {
      d <- dataRequests if d.id === rid
    } yield(d)
    db.run(q.result).map(dr => {
      dr.headOption match {
        case Some(theRequest) => {
          val url = new URL(TextUtils.formatText("%s/downloads/%s", Constants.serverName, uniqueName))
          val mailTxt = Mailer.sendDownloadEmail(theRequest.email, url.toString)
          RequestLogService.log(rid, uid, s"Mail sent to ${theRequest.email}:\n$mailTxt")
        }
        case _ => Unit
      }
    })
  }
}
