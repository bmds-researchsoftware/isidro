package controllers

import models._
import tables._
import utils.silhouette._
import play.api.data._
import play.api.data.Forms._
import play.api._
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
import java.nio.file.Paths
import org.apache.poi.poifs.crypt.CipherAlgorithm;

import edu.dartmouth.geisel.isidro.checksum.ExcelChecksum
import edu.dartmouth.geisel.isidro.encrypt.ExcelEncrypt
import edu.dartmouth.geisel.isidro.read.CsvReader
import edu.dartmouth.geisel.isidro.read.DataIntegrityMismatchException
import edu.dartmouth.geisel.isidro.signature.ExcelSignature
import edu.dartmouth.geisel.isidro.watermark.ExcelWatermark

import edu.dartmouth.isidro.util.RandomUtils;
import utils.Constants


class Application @Inject() (val env: AuthenticationEnvironment, val messagesApi: MessagesApi) extends AuthenticationController with I18nSupport with DataRequestTable with RequirementTable with RequestRequirementTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val dataRequests = TableQuery[DataRequests]
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

  def myRequests = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.userId === request.identity.id)result).map(req =>
      Ok(views.html.requests(req.toList)))
  }
  
  def newRequest = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.newRequest(newRequestForm)))
  }

  def requests = SecuredAction(WithService("master")).async { implicit request =>
    db.run(dataRequests.result).map(req =>
      Ok(views.html.brokerRequests(req.toList)))
  }

  def closedRequests = SecuredAction(WithService("master")).async { implicit request =>
    db.run(dataRequests.filter(_.status === 7).result).map(req =>
      Ok(views.html.brokerRequests(req.toList, true)))
  }

  def request(rid: Int, state: Int) = SecuredAction.async { implicit request =>
    val q = for {
      r <- dataRequests if r.id === rid
    } yield(r)

    db.run(q.result).map(req => {
      Ok(views.html.request(req.headOption, state))
    })
  }

  def editRequest(rid: Int, state: Int) = SecuredAction(WithService("master")).async { implicit request =>
    val q = for {
      r <- dataRequests if r.id === rid
    } yield(r)

    if (state==1) {
      val q3 = for {
        rr <- requestRequirements if rr.request === rid
      } yield (rr.requirement)

      db.run(q.result.zip(requirements.sortBy(r => r.order).result.zip(q3.result))).map(req => {
        req._1.headOption match {
          case Some(theRequest) => {
            Ok(views.html.editRequirements(theRequest, req._2._1.toList, req._2._2.toList))
          }
          case _ => Redirect(routes.Application.requests)
        }
      })
    } else if (state==2) {
      val q3 = for {
        rr <- requestRequirements if rr.request === rid
        r <- requirements if r.id === rr.requirement
      } yield (rr.requirement, rr.completed, r.title)

      db.run(q.result.zip(q3.result)).map(req => {
        req._1.headOption match {
          case Some(theRequest) => {
            Ok(views.html.trackRequirements(theRequest, req._2.toList))
          }
          case _ => Redirect(routes.Application.requests)
        }
      })
    } else if (state==3) {
      db.run(q.result).map(req => {
        req.headOption match {
          case Some(theRequest) => {
            Ok(views.html.sendFile(theRequest))
          }
          case _ => Redirect(routes.Application.requests)
        }
      })
    }
    else if (state==1) Future.successful(Redirect(routes.Application.requests))
    else Future.successful(Redirect(routes.Application.requests))
  }

  def handleProgress(rid: Int) = SecuredAction { implicit request =>
    val complete = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt).toList

    // Update db for all incomplete requirements for this request
    val qIncomplete = for {
      c <- requestRequirements if c.request === rid if !c.requirement.inSetBind(complete)
    } yield c.completed
    db.run(qIncomplete.update(false)).map(rows => {
      // Update request state based on whether any requirements are left incomplete
      db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(if (rows>0) 1 else 2))
    })

    // Update db for all complete requirements for this request
    val qComplete = for {
      c <- requestRequirements if c.request === rid if c.requirement inSetBind complete
    } yield c.completed
    db.run(qComplete.update(true))

    Redirect(routes.Application.requests)
  }

  def handleRequirements(rid: Int) = SecuredAction.async { implicit request =>
    val reqs = request.body.asFormUrlEncoded.head.map(_._1).filter(_.startsWith("rq")).map(_.substring(2).toInt)
    val rrs = reqs.map(new RequestRequirement(rid, _))
    Await.result(db.run(requestRequirements.filter(_.request === rid).delete), Duration.Inf)
    db.run(dataRequests.filter(_.id === rid).map(x => (x.status)).update(1))
    db.run(requestRequirements ++= rrs).map(_ => Redirect(routes.Application.requests))
  }

  def handleNewRequest = SecuredAction.async { implicit request =>
    newRequestForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.newRequest(formWithErrors))),
      req => {
        val fullReq = req.copy(userId = request.identity.id)
        db.run(dataRequests += fullReq).map(_ => Redirect(routes.Application.requests))
      }
    )
  }


  def handleFile(id: Int) = SecuredAction(parse.multipartFormData) { implicit request =>
    val logEntry = new StringBuilder()
    /*    val phi = request.body.asFormUrlEncoded.head.filter(_._1.startsWith("phi")).map(_._2.mkString)
    for (p <- phi) println(s"phi: $p")*/

    val params = request.body.asFormUrlEncoded.map(_._1).toList

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
      filesDir.mkdirs

      println("move")
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
        println(s"FileOutputStream: xlsxPath")
        val os = new FileOutputStream(xlsxPath)
        workbook.write(os)
      } catch {
        case _:Throwable => println("error")
      }
      if (params.contains("signature")) {
        ExcelSignature.sign(Constants.inputSignature, Constants.inputPassword, xlsxPath);
        logEntry.append("Signed\n");
      }

      if (params.contains("encrypt")) {
        val password = RandomUtils.generateRandomBase32NumberString(64);
        ExcelEncrypt.encrypt(xlsxPath, password, CipherAlgorithm.valueOf(Constants.encryptionAlgorithm));
        logEntry.append("Encrypted\n");
      }

      println(logEntry.toString)
      Ok("File uploaded")
    }.getOrElse {
      Redirect(routes.Application.editRequest(id, 3)).flashing(
        "error" -> "Missing file")
    }

    Redirect(routes.Application.editRequest(id, 3)).flashing(
      "error" -> "Missing file")

//    Redirect(routes.Application.requests)
  }

}
