package controllers.pages

import javax.inject.Inject
import javax.mail.MessagingException
import java.io.File
import java.net.{ URL, MalformedURLException }
import java.nio.file.Paths

import edu.dartmouth.geisel.isidro.read.CsvReader
import edu.dartmouth.isidro.util.TextUtils

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import controllers.{ WebJarAssets, pages }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, Controller }
import utils.auth.DefaultEnv
import play.api.libs.Files.TemporaryFile
import play.api.Logger

import scala.concurrent.Future
import utils.{ Mailer, MailService, Constants }
import utils.FileUtils
import forms.pages.{ NewRequestForm, RequirementForm, RequirementListData }
import models.services.{ RequestService }
import models.{ DataRequest, RequestRequirement, User }

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets The webjar assets implementation.
 */
class ApplicationController @Inject() (
  implicit
  val constants: Constants,
  requestService: RequestService,
  fileUtils: FileUtils,
  implicit val mailService: MailService,
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  implicit val webJarAssets: WebJarAssets)
  extends Controller with I18nSupport {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.home(request.identity)))
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(pages.routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

  /**
   * New request form
   */
  def newRequest = silhouette.SecuredAction.async { implicit request =>
    implicit val user = request.identity
    Future.successful(Ok(views.html.request.newRequest(NewRequestForm.form)))
  }

  /**
   * View list of requests
   *
   * @param showClosed False=>Show only closed requests.  True=>Show only open requests
   */
  def requests(showClosed: Boolean) = silhouette.SecuredAction.async { implicit request =>
    requestService.retrieve(showClosed).map(req =>
      Ok(views.html.brokerRequests(request.identity, req.toList, showClosed)))
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
      if (req.status == constants.AWAITINGDOWNLOAD) {
        pages.routes.ApplicationController.editAwaitingDownload(req.id)
      } else if (req.status == constants.DOWNLOADED) {
        pages.routes.ApplicationController.editDownloaded(req.id)
      } else if (req.status == constants.CLOSED) {
        pages.routes.ApplicationController.viewLog(req.id)
      } else {
        pages.routes.ApplicationController.requests(false)
      }
    ).flashing("info" -> messagesApi("request.updated", req.statusString))
  }

  /**
   * Form to edit a request
   *
   * @param rid The request's id
   */
  def editRequest(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    implicit val user = request.identity
    requestService.load(rid).map(req => {
      if (req.head.status >= constants.AWAITINGDOWNLOAD) {
        redirectToCurrentState(req.head)
      } else {
        Ok(views.html.request.editRequest(rid, NewRequestForm.form.fill(req.head)))
      }
    })
  }

  /**
   * Form to edit a request's requirements
   *
   * @param rid The request's id
   */
  def editRequirements(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.getRequirements(rid).map {
      case (req, (requirements, requestRequirements)) => {
        req.headOption match {
          case Some(theRequest) => {
            if (theRequest.status >= constants.AWAITINGDOWNLOAD) {
              redirectToCurrentState(theRequest)
            } else {
              Ok(views.html.request.editRequirements(request.identity, theRequest, requirements, requestRequirements))
            }
          }
          case _ => Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("request.not.found"))
        }
      }
    }
  }

  /**
   * Form to edit a request's requirement progress
   *
   * @param rid The request's id
   */
  def editProgress(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.getRequirementProgress(rid).map {
      case (req, requestRequirements) => {
        req.headOption match {
          case Some(theRequest) => {
            if (theRequest.status >= constants.AWAITINGDOWNLOAD) {
              redirectToCurrentState(theRequest)
            } else {
              Ok(views.html.request.trackRequirements(request.identity, theRequest, requestRequirements))
            }
          }
          case _ => Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("request.not.found"))
        }
      }
    }
  }

  /**
   * Form to upload PHI file.
   *
   * @param rid The request's id
   */
  def sendFile(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.load(rid).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= constants.AWAITINGDOWNLOAD) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.sendFile(request.identity, theRequest))
          }
        }
        case _ => Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("request.not.found"))
      }
    })
  }

  /**
   * Form to edit request that is waiting to be downloaded.
   *
   * @param rid The request's id
   */
  def editAwaitingDownload(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.load(rid).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= constants.DOWNLOADED) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.editAwaiting(request.identity, theRequest))
          }
        }
        case _ => Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("request.not.found"))
      }
    })
  }

  /**
   * Secure download link landing page.  Provides a link to download the file, or a note that the file has expired or been deleted.
   *
   * @param uid Unique name of file to download
   */
  def download(uid: String) = silhouette.UserAwareAction.async { implicit request =>
    requestService.getUniqueFileByName(uid).map { res =>
      res.headOption match {
        case Some((uf, r)) => {
          if (uf.isDeleted || uf.isFileExpired(constants.getInt("fileExpiration"))) {
            Ok(views.html.downloads.expired())
          } else {
            Ok(views.html.downloads.download(uid))
          }
        }
        case _ => {
          Logger.error(messagesApi("download.missing", uid))
          Redirect(pages.routes.ApplicationController.index)
        }
      }
    }
  }

  /**
   * Download and delete a request's data file.
   *
   * @param uniqueName: Secure name from download link to identify file.
   */
  def downloadFile(uniqueName: String) = Action.async {
    requestService.getUniqueFileByName(uniqueName).map(res => res.headOption match {
      case Some((uf, req)) => {
        val isFileExpired = uf.isFileExpired(constants.getInt("fileExpiration"))
        val filePath = new File(uf.fileLocation)
        val fileName = uf.fileName

        if (uf.isFileExpired(constants.getInt("fileExpiration"))) {
          Logger.debug(messagesApi("download.deleted", uf.fileLocation))
          Redirect(pages.routes.ApplicationController.index).flashing(("error" -> messagesApi("download.expired")))
        } else if (!filePath.exists || uf.isDeleted) {
          Logger.debug(messagesApi("download.missing", uf.fileLocation))
          Redirect(pages.routes.ApplicationController.index).flashing(("error" -> messagesApi("download.expired")))
        } else {
          val password = uf.password
          val fileToServe = TemporaryFile(filePath)
          if (password.isDefined) {
            Mailer.sendPasswordEmail(req.email, password.get)
          }
          requestService.log(req.id, "---", messagesApi("download.downloaded"))
          requestService.setState(req.id, constants.DOWNLOADED)
          requestService.setDeleted(req.id)
          Ok.sendFile(fileToServe.file, onClose = () => { fileToServe.clean })
        }
      }
      case _ => {
        Logger.error(s"no file: $uniqueName")
        Redirect(pages.routes.ApplicationController.index).flashing(("error" -> messagesApi("download.expired")))
      }
    })
  }

  /**
   *  Form to edit (close) request that has been downloaded.
   *
   * @param rid The request's id
   */
  def editDownloaded(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    implicit val user = request.identity
    requestService.load(rid).map(req => {
      req.headOption match {
        case Some(theRequest) => {
          if (theRequest.status >= constants.CLOSED) {
            redirectToCurrentState(theRequest)
          } else {
            Ok(views.html.request.editDownloaded(theRequest))
          }
        }
        case _ => Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("request.not.found"))
      }
    })
  }

  /**
   *  View log of request activity
   *
   * @param rid The request's id
   */
  def viewLog(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.getLog(rid).map {
      case (lg, req) => {
        Ok(views.html.request.viewLog(
          request.identity,
          req.head,
          lg))
      }
    }
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
  def handleEditRequest(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    implicit val user = request.identity
    NewRequestForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.request.editRequest(rid, formWithErrors))),
      req => {
        if (rid < 0) { // todo: change this to Option
          val fullReq = req.copy(status = constants.NEWREQUEST)
          requestService.insert(fullReq).map(newId => {
            requestService.log(
              newId,
              request.identity.userID.toString,
              fullReq.logString)
            Redirect(pages.routes.ApplicationController.requests(false))
          })
        } else {
          val fullReq = req.copy(id = rid, status = constants.NEWREQUEST)
          requestService.log(rid, request.identity.userID.toString, messagesApi("request.edited", fullReq.logString))
          requestService.update(rid, fullReq).map(_ => Redirect(pages.routes.ApplicationController.requests(false)))
        }
      }
    )
  }

  /**
   * Form handler to edit request's requirements.
   *
   * @param rid The request's id
   */
  def handleRequirements(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    RequirementForm.form.bindFromRequest.fold(
      formWithErrors => { // The form is only checkboxes, so if there's an error there's nothing we can correct
        Future.successful(Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("form.error")))
      },
      requirementData => {
        val rrs = requirementData.reqs.map(new RequestRequirement(rid, _))
        requestService.deleteRequirements(rid)
        requestService.getRequirementTitles(requirementData.reqs).map(reqText =>
          requestService.log(rid, request.identity.userID.toString, messagesApi("request.requirements.edited", reqText.mkString("\n")))
        )
        requestService.setState(rid, constants.AWAITINGREQUIREMENTS)
        requestService.addRequirements(rrs).map(_ => Redirect(pages.routes.ApplicationController.requests(false)))
      }
    )
  }

  /**
   * Form handler for request requirement progress updating.
   *
   * @param rid The request's id
   */
  def handleProgress(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    RequirementForm.form.bindFromRequest.fold(
      formWithErrors => { // The form is only checkboxes, so if there's an error there's nothing we can correct
        Future.successful(Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi("form.error")))
      },
      requirementData => {
        val complete = requirementData.reqs.map(_.toInt)

        // Update db for all complete and incomplete requirements for this request
        requestService.updateProgress(rid, complete)

        // Log requirement progress
        requestService.getRequirementProgress(rid).map {
          case (_, prog) =>
            {
              val completeList = prog.filter(_._2).map(_._3).mkString("\n")
              val incompleteList = prog.filter(!_._2).map(_._3).mkString("\n")
              // Update request state based on whether any requirements are left incomplete
              requestService.setState(rid, if (incompleteList.isEmpty) constants.READYTOSEND else constants.AWAITINGREQUIREMENTS)
              requestService.log(rid, request.identity.userID.toString, messagesApi("request.progress", completeList, incompleteList))
              Redirect(pages.routes.ApplicationController.requests(false))
            }
        }
      }
    )
    /*    val complete = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt).toList

    // Update db for all complete and incomplete requirements for this request
    requestService.updateProgress(rid, complete)

    // Log requirement progress
    requestService.getRequirementProgress(rid).map {
      case (_, prog) => {
        val completeList = prog.filter(_._2).map(_._3).mkString("\n")
        val incompleteList = prog.filter(!_._2).map(_._3).mkString("\n")
        // Update request state based on whether any requirements are left incomplete
        requestService.setState(rid, if (incompleteList.isEmpty) constants.READYTOSEND else constants.AWAITINGREQUIREMENTS)
        requestService.log(rid, request.identity.userID.toString, messagesApi("request.progress", completeList, incompleteList))
        Redirect(pages.routes.ApplicationController.requests(false))
      }
    }*/

  }

  /**
   * Form handler for request data upload form.  Builds xlsx file and emails researcher with secure download link.
   *
   * @param rid The request's id
   */
  def handleFileUpload(rid: Int) = silhouette.SecuredAction(parse.multipartFormData) { implicit request =>
    val logEntry = new StringBuilder()

    val params = request.body.asFormUrlEncoded

    request.body.file("dataFile").map { dataFile =>
      try {
        val filesPath = Paths.get(constants.getString("outputDir"), rid.toString).toString
        val csvPath = Paths.get(filesPath, constants.getString("outputCsv")).toString
        val csvFile = new File(csvPath)
        val xlsxPath = Paths.get(filesPath, constants.getString("outputXlsx")).toString
        val filesDir = new File(filesPath)
        filesDir.mkdirs()
        dataFile.ref.moveTo(csvFile)

        val csvContents = CsvReader.read(csvPath)
        csvFile.delete()

        val uniqueFile = fileUtils.buildFile(rid, xlsxPath, csvContents, logEntry,
          params.contains("fingerprint"),
          params.contains("watermark"),
          params.contains("signature"),
          params.contains("encrypt"))

        filesDir.delete()

        requestService.insert(uniqueFile)

        logEntry.append(s"File: ${uniqueFile.fileLocation}\n")
        if (params.contains("notes")) {
          logEntry.append(s"---Notes---\n${params("notes").head}\n")
        }
        logEntry.append("---PHI---\n")
        params.filter(_._1.startsWith("phi")).map(p => logEntry.append(s"${p._2.head}\n"))
        if (params.contains("other") && (params.get("other") != None)) {
          logEntry.append(s"Other phi: ${params.get("other").get.head}\n")
        }
        sendDownloadEmail(rid, uniqueFile.uniqueName, request.identity)
        requestService.log(rid, request.identity.userID.toString, s"Send download link:\n${logEntry.toString().trim()}")
        requestService.setState(rid, constants.AWAITINGDOWNLOAD)
        Redirect(pages.routes.ApplicationController.requests(false)).flashing("success" -> messagesApi("download.mailed"))
      } catch {
        case e: Exception => Redirect(pages.routes.ApplicationController.requests(false)).flashing("error" -> messagesApi(e.getMessage))
      }
    }.getOrElse {
      Redirect(pages.routes.ApplicationController.sendFile(rid)).flashing("error" -> messagesApi("error.chooseFile"))
    }

  }

  /**
   * Form handler for withdrawing a request's uploaded data file.
   *
   * @param rid The request's id
   */
  def handleWithdrawFile(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.getUniqueFiles(rid).map {
      case (uf, req) => {
        uf.foreach(_.delete)
        requestService.setDeleted(rid)
        requestService.setState(rid, constants.READYTOSEND)
        uf.foreach(f =>
          requestService.log(rid, request.identity.userID.toString, messagesApi("request.withdrawn", f.fileLocation))
        )
        Redirect(pages.routes.ApplicationController.requests(false))
      }
      case _ => {
        Logger.error(messagesApi("request.no.file", rid))
        Redirect(pages.routes.ApplicationController.requests(false)).flashing(("error" -> messagesApi("request.no.file", rid)))
      }
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
  private def sendDownloadEmail(rid: Int, uniqueName: String, uid: User): Unit = {
    requestService.load(rid).map(dr => {
      dr.headOption match {
        case Some(theRequest) => {
          val url = new URL(TextUtils.formatText("%s/downloads/%s", constants.getString("serverName"), uniqueName))
          val mailTxt = Mailer.sendDownloadEmail(theRequest.email, url.toString)
          requestService.log(rid, uid.userID.toString, s"Mail sent to ${theRequest.email}:\n$mailTxt")
        }
        case _ => Unit
      }
    })
  }

  /**
   * Form handler for closing a completed request.
   *
   * @param rid The request's id
   */
  def handleClose(rid: Int) = silhouette.SecuredAction.async { implicit request =>
    requestService.log(rid, request.identity.userID.toString, messagesApi("request.close"))
    requestService.setState(rid, constants.CLOSED)
    Future.successful(Redirect(pages.routes.ApplicationController.requests(false)))
  }

}
