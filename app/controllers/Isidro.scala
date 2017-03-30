package controllers

import edu.dartmouth.geisel.isidro.read.CsvReader
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException
import edu.dartmouth.isidro.util.TextUtils
import models.{DataRequest, RequestLogService, RequestRequirement, UniqueFileServ}
import tables.{DataRequestTable, RequirementTable, RequestLogTable, RequestRequirementTable, UniqueFileTable}
import utils.Constants
import utils.FileUtils
import utils.MailService
import utils.Mailer
import utils.RandomUtils
import utils.silhouette._
import views.html.{ auth => viewsAuth }

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Paths
import javax.inject.Inject
import javax.mail.MessagingException
import play.api.Logger
import play.api.Play
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


  /**
   * Landing page. (Dead end)
   */
  def index = UserAwareAction { implicit request =>
    Ok(views.html.index())
  }

  /**
   * New request form
   */
  def newRequest = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.request.newRequest(newRequestForm)))
  }

  /**
   * View list of requests
   *
   * @param showClosed False=>Show only closed requests.  True=>Show only open requests
   */
  def requests(showClosed: Boolean) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(if (showClosed) _.status === Constants.CLOSED else _.status =!= Constants.CLOSED).sortBy(_.id).result).map(req =>
      Ok(views.html.brokerRequests(req.toList, showClosed)))
  }

  /**
   *  View log of request activity
   *
   * @param rid The request's id
   */
  def viewLog(rid: Int) = SecuredAction.async { implicit request =>
    val q = for {
      (log, u) <- requestLogs.filter(_.request === rid) joinLeft users on (_.user === _.id)
    } yield (log, u)
    db.run(q.result.zip(dataRequests.filter(_.id === rid).result)).map(logs => {
      Ok(views.html.request.viewLog(logs._2.head, logs._1.toList.sortBy(_._1.time.getTime)))
    })
  }

  /**
    * Redirect to the edit page corresponding to the request's current state.
    *
    * @param rid: The request's id
    * @param state: The request's current state
    * @return Redirect response to the appropriate edit page, or requests list
    */
  private def redirectToCurrentState(req: DataRequest) = {
    Redirect(
      if (req.status == Constants.AWAITINGDOWNLOAD) {
        routes.Isidro.editAwaitingDownload(req.id)
      } else if (req.status == Constants.DOWNLOADED) {
        routes.Isidro.editDownloaded(req.id)
      } else if (req.status == Constants.CLOSED) {
        routes.Isidro.viewLog(req.id)
      } else {
        routes.Isidro.requests(false)
      }
    ).flashing("info" -> Messages("request.updated", req.statusString))
  }

  /**
   * Form to edit a request
   *
   * @param rid The request's id
   */
  def editRequest(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      if (req.head.status >= Constants.AWAITINGDOWNLOAD) {
        redirectToCurrentState(req.head)
      } else {
        Ok(views.html.request.editRequest(rid, newRequestForm.fill(req.head)))
      }
    })
  }

  /**
   * Form to edit a request's requirements
   *
   * @param rid The request's id
   */
  def editRequirements(rid: Int) = SecuredAction.async { implicit request =>
    val q3 = for {
      rr <- requestRequirements if rr.request === rid
    } yield (rr.requirement)

    db.run(dataRequests.filter(_.id === rid).result.zip(requirements.sortBy(r => r.order).result.zip(q3.result))).map(req => {
      req._1.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= Constants.AWAITINGDOWNLOAD) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.editRequirements(theRequest, req._2._1.toList, req._2._2.toList))
          }
        }
        case _ => Redirect(routes.Isidro.requests(false)).flashing("error" -> Messages("request.not.found"))
      }
    })
  }

  /**
   * Form to edit a request's requirement progress
   *
   * @param rid The request's id
   */
  def editProgress(rid: Int) = SecuredAction.async { implicit request =>
    val q3 = for {
      rr <- requestRequirements if rr.request === rid
      r <- requirements if r.id === rr.requirement
    } yield (rr.requirement, rr.completed, r.title)

    db.run(dataRequests.filter(_.id === rid).result.zip(q3.result)).map(req => {
      req._1.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= Constants.AWAITINGDOWNLOAD) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.trackRequirements(theRequest, req._2.toList))
          }
        }
        case _ => Redirect(routes.Isidro.requests(false)).flashing("error" -> Messages("request.not.found"))
      }
    })
  }

  /**
   * Form to upload PHI file.
   *
   * @param rid The request's id
   */
  def sendFile(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= Constants.AWAITINGDOWNLOAD) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.sendFile(theRequest))
          }
        }
        case _ => Redirect(routes.Isidro.requests(false)).flashing("error" -> Messages("request.not.found"))
      }
    })
  }

  /**
   * Form to edit request that is waiting to be downloaded.
   *
   * @param rid The request's id
   */
  def editAwaitingDownload(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= Constants.DOWNLOADED) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.editAwaiting(theRequest))
          }
        }
        case _ => Redirect(routes.Isidro.requests(false)).flashing("error" -> Messages("request.not.found"))
      }
    })
  }

  /**
   *  Form to edit (close) request that has been downloaded.
   *
   * @param rid The request's id
   */
  def editDownloaded(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).result).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= Constants.CLOSED) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.editDownloaded(theRequest))
          }
        }
        case _ => Redirect(routes.Isidro.requests(false)).flashing("error" -> Messages("request.not.found"))
      }
    })
  }

  /**
   * Form handler for request requirement progress updating.
   *
   * @param rid The request's id
   */
  def handleProgress(rid: Int) = SecuredAction.async { implicit request =>
    val complete = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt).toList

    // Update db for all incomplete requirements for this request
    val qIncomplete = for {
      c <- requestRequirements if c.request === rid if !c.requirement.inSetBind(complete)
    } yield c.completed
    Await.result(db.run(qIncomplete.update(false)), Duration.Inf)

    // Update db for all complete requirements for this request
    val qComplete = for {
      c <- requestRequirements if c.request === rid if c.requirement inSetBind complete
    } yield c.completed
    Await.result(db.run(qComplete.update(true)), Duration.Inf)

    // Log requirement progress
    val qStatus = for {
      rr <- requestRequirements if rr.request === rid
      requirement <- requirements if requirement.id === rr.requirement
    } yield (requirement.title, rr.completed)

    val rrs = Await.result(db.run(qStatus.result), Duration.Inf)
    val rrList = rrs.toList
    val completeList = rrList.filter(_._2).map(_._1).mkString("\n")
    val incompleteList = rrList.filter(!_._2).map(_._1).mkString("\n")
    RequestLogService.log(rid, request.identity.id, Messages("request.progress", completeList, incompleteList))

    // Update request state based on whether any requirements are left incomplete
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status))
      .update(if (incompleteList.isEmpty) Constants.READYTOSEND else Constants.AWAITINGREQUIREMENTS))
      .map(_ => Redirect(routes.Isidro.requests(false)))
  }

  /**
   * Form handler to edit request's requirements.
   *
   * @param rid The request's id
   */
  def handleRequirements(rid: Int) = SecuredAction.async { implicit request =>
    val reqs = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt)
    val rrs = reqs.map(new RequestRequirement(rid, _))
    Await.result(db.run(requestRequirements.filter(_.request === rid).delete), Duration.Inf)

    val qRequirements = for {
      r <- requirements if r.id inSetBind(reqs)
    } yield (r.title)

    db.run(qRequirements.result).map (reqText =>
      RequestLogService.log(rid, request.identity.id, Messages("request.requirements.edited", reqText.mkString("\n")))
    )
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.AWAITINGREQUIREMENTS))
    db.run(requestRequirements ++= rrs).map(_ => Redirect(routes.Isidro.requests(false)))
  }

  /**
   * Form handler for new requests
   */
  def handleNewRequest = handleEditRequest(-1)

  /**
   * Form handler for request editing, and new requests.
   *
   * @param rid The request's id, or -1 for new request.
   */
  def handleEditRequest(rid: Int) = SecuredAction.async { implicit request =>
    newRequestForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.request.newRequest(formWithErrors))),
      req => {
        if (rid < 0) { // todo: change this to Option
          val fullReq = req.copy(userId = request.identity.id, status = Constants.NEWREQUEST)
          val insertQ = dataRequests returning dataRequests.map(_.id)
          db.run(insertQ += fullReq).map(newId => {
            RequestLogService.log(newId, request.identity.id, fullReq.logString)
            Redirect(routes.Isidro.requests(false))
          })
        } else {
          val fullReq = req.copy(id = rid, userId = request.identity.id, status = Constants.NEWREQUEST)
          RequestLogService.log(rid, request.identity.id, Messages("request.edited", fullReq.logString))
          db.run(dataRequests.filter(_.id === rid).update(fullReq)).map(_ => Redirect(routes.Isidro.requests(false)))
        }
      }
    )
  }

  /**
   * Form handler for withdrawing a request's uploaded data file.
   *
   * @param rid The request's id
   */
  def handleWithdrawFile(rid: Int) = SecuredAction.async { implicit request =>
    val fileq = for {
      uf <- uniqueFiles if (uf.requestId === rid && !uf.isDeleted)
      r <- dataRequests if uf.requestId === r.id
    } yield (uf, r)
    db.run(fileq.result).map(res => res.headOption match {
      case Some((uf, req)) => {
        uf.delete
        db.run(uniqueFiles.filter(_.requestId === rid).map(x => (x.isDeleted)).update(true))
        db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.READYTOSEND))
        RequestLogService.log(rid, request.identity.id, Messages("request.withdrawn", uf.fileLocation))
        Redirect(routes.Isidro.requests(false))
      }
      case _ => {
        Logger.error(Messages("request.no.file", rid))
        Redirect(routes.Isidro.requests(false)).flashing(("error" -> Messages("request.no.file", rid)))
      }
    })
  }

  /**
   * Form handler for closing a completed request.
   *
   * @param rid The request's id
   */
  def handleClose(rid: Int) = SecuredAction.async { implicit request =>
    RequestLogService.log(rid, request.identity.id, Messages("request.close"))
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.CLOSED)).map(_ => Redirect(routes.Isidro.requests(false)))
  }

  /**
   * Download and delete a request's data file.
   *
   * @param uniqueName: Secure name from download link to identify file.
   */
  def downloadFile(uniqueName: String) = Action.async {
    val fileq = for {
      uf <- uniqueFiles if uf.uniqueName === uniqueName
      r <- dataRequests if uf.requestId === r.id
    } yield (uf, r)
    db.run(fileq.result).map(res => res.headOption match {
      case Some((uf, req)) => {
        val isFileExpired = uf.isFileExpired(Constants.getInt("fileExpiration"))
        val filePath = new File(uf.fileLocation)
        val fileName = uf.fileName

        if (uf.isFileExpired(Constants.getInt("fileExpiration"))) {
          Logger.debug(Messages("download.deleted", uf.fileLocation))
          Redirect(routes.Isidro.index)
        } else if (!filePath.exists || uf.isDeleted) {
          Logger.debug(Messages("download.missing", uf.fileLocation))
          Redirect(routes.Isidro.index)
        } else {
          val password = uf.password
          val fileToServe = TemporaryFile(filePath)
          if (password.isDefined) {
            Mailer.sendPasswordEmail(req.email, password.get)
          }
          RequestLogService.log(req.id, 0L, Messages("download.downloaded"))
          db.run(dataRequests.filter(_.id === req.id).map(x => (x.status)).update(Constants.DOWNLOADED))
          db.run(uniqueFiles.filter(_.uniqueName === uniqueName).map(_.isDeleted).update(true))
          Ok.sendFile(fileToServe.file, onClose = () => { fileToServe.clean })
        }
      }
      case _ => {
        Logger.error(s"no file: $uniqueName")
        Redirect(routes.Isidro.index).flashing(("error" -> Messages("download.expired")))
      }
    })
  }

  /**
   * Secure download link landing page.  Provides a link to download the file, or a note that the file has expired or been deleted.
   *
   * @param uid Unique name of file to download
   */
  def download(uid: String) = UserAwareAction.async { implicit request =>
    val q = for {
      uf <- uniqueFiles if uf.uniqueName === uid
      r <- dataRequests if uf.requestId === r.id
    } yield (uf, r)

    db.run(q.result).map { res => res.headOption match {
      case Some((uf, r)) => {
        if (uf.isDeleted || uf.isFileExpired(Constants.getInt("fileExpiration"))) {
          Ok(views.html.downloads.expired())
        } else {
          Ok(views.html.downloads.download(uid))
        }
      }
      case _ => {
        Logger.error(Messages("download.missing", uid))
        Redirect(routes.Isidro.index)
      }
    }}
  }

  /**
   * Form handler for request data upload form.  Builds xlsx file and emails researcher with secure download link.
   *
   * @param rid The request's id
   */
  def handleFileUpload(rid: Int) = SecuredAction(parse.multipartFormData) { implicit request =>
    val logEntry = new StringBuilder()

    val params = request.body.asFormUrlEncoded

    request.body.file("dataFile").map { dataFile =>
      val filesPath = Paths.get(Constants.getString("outputDir"), rid.toString).toString
      val csvPath = Paths.get(filesPath, Constants.getString("outputCsv")).toString
      val csvFile = new File(csvPath)
      val xlsxPath = Paths.get(filesPath, Constants.getString("outputXlsx")).toString
      val filesDir = new File(filesPath)
      filesDir.mkdirs()
      dataFile.ref.moveTo(csvFile)

      val csvContents = CsvReader.read(csvPath)
      csvFile.delete()


      val uniqueFile = FileUtils.buildFile(rid, xlsxPath, csvContents, logEntry,
        params.contains("fingerprint"),
        params.contains("watermark"),
        params.contains("signature"),
        params.contains("encrypt"))

      filesDir.delete()

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
      Redirect(routes.Isidro.requests(false)).flashing("success" -> Messages("download.mailed"))
    }.getOrElse {
      Redirect(routes.Isidro.sendFile(rid)).flashing("error" -> Messages("error.chooseFile"))
    }

  }


  /**
   * Send an email with the data file download link.
   *
   * @param rid The request's id
   * @param uniqueName Data file identifier
   * @param uid Signed in user's id
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
          val url = new URL(TextUtils.formatText("%s/downloads/%s", Constants.getString("serverName"), uniqueName))
          val mailTxt = Mailer.sendDownloadEmail(theRequest.email, url.toString)
          RequestLogService.log(rid, uid, s"Mail sent to ${theRequest.email}:\n$mailTxt")
        }
        case _ => Unit
      }
    })
  }
}
