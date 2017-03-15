package controllers

import models._
import tables._
import utils.silhouette._
import play.api.data._
import play.api.data.Forms._
import play.api._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Action
import play.api.Play.current
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile
import play.api.i18n.{ MessagesApi, Messages, Lang, I18nSupport }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.Inject
import views.html.{ auth => viewsAuth }

import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.MalformedURLException
import javax.mail.MessagingException
import java.net.URL
import java.nio.file.Paths
import org.apache.poi.poifs.crypt.CipherAlgorithm
import edu.dartmouth.geisel.isidro.checksum.ExcelChecksum
import edu.dartmouth.geisel.isidro.encrypt.ExcelEncrypt
import edu.dartmouth.geisel.isidro.read.CsvReader
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException
import edu.dartmouth.geisel.isidro.signature.ExcelSignature
import edu.dartmouth.geisel.isidro.watermark.ExcelWatermark
import edu.dartmouth.isidro.util.TextUtils

import edu.dartmouth.isidro.util.RandomUtils
import utils.Constants
import utils.MailService
import utils.Mailer

class Isidro @Inject() (val env: AuthenticationEnvironment, val messagesApi: MessagesApi, val mailService: MailService) extends AuthenticationController with I18nSupport with DataRequestTable with RequirementTable with RequestRequirementTable with UniqueFileTable with HasDatabaseConfig[JdbcProfile] {
  implicit val ms = mailService
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val dataRequests = TableQuery[DataRequests]
  val uniqueFiles = TableQuery[UniqueFiles]
  val requestRequirements = TableQuery[RequestRequirements]
  val requirements = TableQuery[Requirements]

  val newRequestForm = Form(mapping(
    "id" -> ignored(0),
    "userId" -> ignored(0L),
    "email" -> nonEmptyText,
    "title" -> nonEmptyText,
    "description" -> nonEmptyText,
    "status" -> ignored(0),
    "pi" -> nonEmptyText,
    "phone" -> nonEmptyText,
    "cphs" -> nonEmptyText)
    (DataRequest.apply _ )(DataRequest.unapply _))

  def index = UserAwareAction { implicit request =>
    Ok(views.html.index())
  }

  def myAccount = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.myAccount()))
  }

  /* def myRequests = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.userId === request.identity.id)result).map(req =>
      Ok(views.html.requests(req.toList)))
  } */
  
  def newRequest = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.request.newRequest(newRequestForm)))
  }

  def requests = SecuredAction(WithService("master")).async { implicit request =>
    db.run(dataRequests.filter(_.status =!= Constants.closed).result).map(req =>
      Ok(views.html.brokerRequests(req.toList)))
  }

  def closedRequests = SecuredAction(WithService("master")).async { implicit request =>
    db.run(dataRequests.filter(_.status === Constants.closed).result).map(req =>
      Ok(views.html.brokerRequests(req.toList, true)))
  }

  /*def request(rid: Int, state: Int) = SecuredAction.async { implicit request =>
    val q = for {
      r <- dataRequests if r.id === rid
    } yield(r)

    db.run(q.result).map(req => {
      Ok(views.html.request(req.headOption, state))
    })
  }*/

  def editRequest(rid: Int, state: Int) = SecuredAction(WithService("master")).async { implicit request =>
    val q = for {
      r <- dataRequests if r.id === rid
    } yield(r)

    println(s"edit $state ${Constants.awaitingDownload}")
    if (state==Constants.editRequest) {
      db.run(q.result).map(req => {
        Ok(views.html.request.editRequest(rid, newRequestForm.fill(req.head)))
      })
    } else if (state==Constants.newRequest) {
      val q3 = for {
        rr <- requestRequirements if rr.request === rid
      } yield (rr.requirement)

      db.run(q.result.zip(requirements.sortBy(r => r.order).result.zip(q3.result))).map(req => {
        req._1.headOption match {
          case Some(theRequest) => {
            Ok(views.html.request.editRequirements(theRequest, req._2._1.toList, req._2._2.toList))
          }
          case _ => Redirect(routes.Isidro.requests)
        }
      })
    } else if (state==Constants.awaitingRequirements) {
      val q3 = for {
        rr <- requestRequirements if rr.request === rid
        r <- requirements if r.id === rr.requirement
      } yield (rr.requirement, rr.completed, r.title)

      db.run(q.result.zip(q3.result)).map(req => {
        req._1.headOption match {
          case Some(theRequest) => {
            Ok(views.html.request.trackRequirements(theRequest, req._2.toList))
          }
          case _ => Redirect(routes.Isidro.requests)
        }
      })
    } else if (state==Constants.readyToSend) {
      db.run(q.result).map(req => {
        req.headOption match {
          case Some(theRequest) => {
            Ok(views.html.request.sendFile(theRequest))
          }
          case _ => Redirect(routes.Isidro.requests)
        }
      })
    } else if (state==Constants.awaitingDownload) {
      db.run(q.result).map(req => {
        req.headOption match {
          case Some(theRequest) => {
            Ok(views.html.request.editAwaiting(theRequest))
          }
          case _ => Redirect(routes.Isidro.requests)
        }
      })
    } else if (state==Constants.downloaded) {
      db.run(q.result).map(req => {
        req.headOption match {
          case Some(theRequest) => {
            Ok(views.html.request.editDownloaded(theRequest))
          }
          case _ => Redirect(routes.Isidro.requests)
        }
      })
    }
    else Future.successful(Redirect(routes.Isidro.requests))
  }

  def handleProgress(rid: Int) = SecuredAction { implicit request =>
    val complete = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt).toList

    // Update db for all incomplete requirements for this request
    val qIncomplete = for {
      c <- requestRequirements if c.request === rid if !c.requirement.inSetBind(complete)
    } yield c.completed
    db.run(qIncomplete.update(false)).map(rows => {
      // Update request state based on whether any requirements are left incomplete
      db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(if (rows>0) Constants.awaitingRequirements else Constants.readyToSend))
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
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.awaitingRequirements))
    db.run(requestRequirements ++= rrs).map(_ => Redirect(routes.Isidro.requests))
  }

  def handleNewRequest = handleEditRequest(-1)

  def handleEditRequest(rid: Int) = SecuredAction.async { implicit request =>
    newRequestForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.request.newRequest(formWithErrors))),
      req => {
        if (rid < 0) { // todo: change this to Option
          val fullReq = req.copy(userId = request.identity.id, status = Constants.newRequest)
          db.run(dataRequests += fullReq).map(_ => Redirect(routes.Isidro.requests))
        } else {
          val fullReq = req.copy(id = rid, userId = request.identity.id, status = Constants.newRequest)
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
        db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.readyToSend))
        Redirect(routes.Isidro.requests)
      }
      case _ => {
        println(s"no file for request: $rid")
        Redirect(routes.Isidro.requests)
      }
    })
  }

  def handleClose(rid: Int) = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(Constants.closed)).map(_ => Redirect(routes.Isidro.requests))
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
        println(s"fileLocation: ${uf.fileLocation}")
        println(s"fileName: ${uf.fileName}")

        if (uf.isFileExpired(Constants.fileExpiration)) {
          println(s"""Data file "${uf.fileLocation}" expired and deleted.""")
          Redirect(routes.Isidro.index)
        } else if (!filePath.exists || uf.isDeleted) {
          println(s"""Data file "${uf.fileLocation}" does not exist on disk.""")
          Redirect(routes.Isidro.index)
        } else {
          val password = uf.password
          val fileToServe = TemporaryFile(filePath)
          if (password.isDefined) {
            Mailer.sendPasswordEmail(req.email, password.get)
          }
          db.run(dataRequests.filter(_.id === req.id).map(x => (x.status)).update(Constants.downloaded))
          Ok.sendFile(fileToServe.file, onClose = () => { fileToServe.clean })
        }
      }
      case _ => {
        println(s"no file: $uniqueName")
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
        println(s"Data file not found: $uid")
        Redirect(routes.Isidro.index)
      }
        
    }}
  }

  def handleFile(id: Int) = SecuredAction(parse.multipartFormData) { implicit request =>
    val logEntry = new StringBuilder()
    /*    val phi = request.body.asFormUrlEncoded.head.filter(_._1.startsWith("phi")).map(_._2.mkString)
    for (p <- phi) println(s"phi: $p")*/

    val params = request.body.asFormUrlEncoded/*.map(_._1).toList*/

    println("handlefile")
    request.body.file("dataFile").map { dataFile =>
      println("datfile")
      import java.io.File
      //      val filename = dataFile.filename
      //val contentType = dataFile.contentType
      val filesPath = Paths.get(Constants.outputDir, id.toString).toString
      val csvPath = Paths.get(filesPath, Constants.outputCsv).toString
      val csvFile = new File(csvPath)
      val xlsxPath = Paths.get(filesPath, Constants.outputXlsx).toString
      val filesDir = new File(filesPath)
      println(s"filesPath: $filesPath")
      filesDir.mkdirs

      println(s"move to $csvPath")
      dataFile.ref.moveTo(csvFile)

      val csvContents = CsvReader.read(csvPath)
      csvFile.delete()

      println("read")

      // CAUTION: fingerprint, watermark, signature, and encryption
      // order of operation important.
      val checksum = ExcelChecksum.checksum(csvContents)
      if (params.contains("fingerprint")) {
        logEntry.append("Fingerprint: " + checksum + "\n")
      }
      println("finger")
      try {
        val workbook = CsvReader.getExcelWorkbook(Constants.isidroWorksheetName, csvContents)
        if (params.contains("watermark")) {
          ExcelWatermark.watermark(workbook, Constants.isidroWorksheetName, new File(Constants.watermarkImagePath))
          logEntry.append("Watermark: " + Constants.watermarkImagePath + "\n")
        }
        println(s"FileOutputStream: $xlsxPath")
        val os = new FileOutputStream(xlsxPath)
        workbook.write(os)
      } catch {
        case _:Throwable => println("error")
      }
      if (params.contains("signature")) {
        ExcelSignature.sign(Constants.inputSignature, Constants.inputPassword, xlsxPath);
        logEntry.append("Signed\n");
      }

      val password:Option[String] = if (params.contains("encrypt")) {
        val pw = RandomUtils.generateRandomBase32NumberString(64);
        ExcelEncrypt.encrypt(xlsxPath, pw, CipherAlgorithm.valueOf(Constants.encryptionAlgorithm));
        logEntry.append("Encrypted\n");
        Some(pw)
      } else None

      val uniqueFile = UniqueFileServ.generateUniqueFile(xlsxPath, checksum, Constants.storeDir, id, password)
      new File(xlsxPath).delete
      filesDir.delete

      db.run(uniqueFiles += uniqueFile)

      logEntry.append(s"File: ${uniqueFile.fileLocation}\n")
      logEntry.append(s"Send to: {request.email}\n")
      if (params.contains("notes")) {
        logEntry.append(s"---Notes---\n${params("notes")}\n")
      }
      logEntry.append("---PHI---\n")
      params.filter(_._1.startsWith("phi")).map(p => logEntry.append(p._2 + "\n"))
      if (params.contains("other") && (params.get("other") != None)) {
        logEntry.append("Other phi: " + params.get("other") + "\n")
      }
      println(s"email: ${uniqueFile.uniqueName}")
      sendDownloadEmail(id, uniqueFile.uniqueName)
      //requestLogService.log(user, request, "Send download link:\n" + logEntry.toString().trim())
      println(s"log: ${logEntry.toString}")
      db.run(dataRequests.filter(_.id === id).map(x => (x.status)).update(Constants.awaitingDownload))
    }.getOrElse {
      Redirect(routes.Isidro.editRequest(id, 3)).flashing("error" -> "Missing file")
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
  private def sendDownloadEmail(rid: Int, uniqueName: String):Unit = {
    println("sdl")
    val q = for {
      d <- dataRequests if d.id === rid
    } yield(d)
    db.run(q.result).map(dr => {
      dr.headOption match {
        case Some(theRequest) => {
          println(s"rez ${theRequest.email}")
          val url = new URL(TextUtils.formatText("%s/downloads/%s", Constants.serverName, uniqueName))
          println(s"url: $url")
          Mailer.sendDownloadEmail(theRequest.email, url.toString)
        }
        case _ => Unit
      }
    })
  }
}
