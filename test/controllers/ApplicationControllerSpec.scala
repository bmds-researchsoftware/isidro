package controllers

import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.test._
import controllers.pages.ApplicationController
import forms.pages.{NewRequestForm, RequirementForm, RequirementListData}
import models.services.RequestService
import models.{DataRequest, User}
import net.codingwell.scalaguice.ScalaModule
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{AnyContentAsEmpty, MultipartFormData, Result}
import play.api.test._
import utils.auth.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import edu.dartmouth.geisel.isidro.read.CsvReader

/**
  * Test case for the [[ApplicationController]] class.
  */
class ApplicationControllerSpec extends PlaySpecification with Mockito {
  sequential

  private val REDIRECT_TO_LOGIN: String = "redirect to login page if user is unauthorized"
  private val REDIRECT_TO_REQUESTS = "redirect to request page on successful submission"
  private val HTML_CONTENT_TYPE: String = "text/html"
  private val CSRF_BYPASS_HEADER = Seq("X-Requested-With"->"1","Csrf-Token"->"nocheck")

  "The `index` action" should {
    "redirect to login page" in new Context {
      new WithApplication(application) {
        val Some(result) = route(app, FakeRequest(pages.routes.ApplicationController.index()))
        val Some(newResult) = redirectResult(app, result)

        status(newResult) must be equalTo OK
        contentType(newResult) must beSome(HTML_CONTENT_TYPE)
        contentAsString(newResult) must contain("Sign in to ISIDRO")
      }
    }
  }

  "The `signOut` action" should {
    "redirect to home page if user is authorized" in new Context {
      new WithApplication(application) {

        val Some(result) = route(app,
          FakeRequest(pages.routes.ApplicationController.signOut()).withAuthenticator[DefaultEnv](identity.loginInfo))

        val Some(finalResult) = redirectResultWithAuth(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome(HTML_CONTENT_TYPE)
        contentAsString(finalResult) must contain("ISIDRO - Home")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.signOut()))
      }
    }
  }

  "The `newRequest` action" should {
    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.newRequest()).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("New Request")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.newRequest()))
      }
    }
  }

  "The `requests` action" should {
    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.requests(false))
          .withAuthenticator[DefaultEnv](LoginInfo("invalid", "invalid")))
      }
    }
  }

  "return 200 requests page if user is authorized" in new Context {
    new WithApplication(application) {
      val Some(result) = route(app, FakeRequest(pages.routes.ApplicationController.requests(false))
        .withAuthenticator[DefaultEnv](identity.loginInfo)
      )

      status(result) must beEqualTo(OK)
      contentType(result) must beSome(HTML_CONTENT_TYPE)
      contentAsString(result) must contain("Requests")
    }
  }


  "The `editRequest` action" should {
    "redirect to New Request page" in new Context {
      new WithApplication(application) {
        val request = FakeRequest(pages.routes.ApplicationController.editRequest(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("Update Request")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.editRequest(1)))
      }
    }
  }

  "The `editRequirements` action" should {
    "redirect to Edit Requirements Page" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editRequirements(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("Edit Request Requirements")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.editRequirements(1)))
      }
    }
  }

  "The `editProgress` action" should {
    "redirect to Edit Progress Page" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editProgress(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("Edit Requirement Progress")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.editProgress(1)))
      }
    }
  }

  "The `sendFile` action" should {
    "redirect to Send File page" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.sendFile(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("Send File")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.sendFile(1)))
      }
    }
  }

  "The `editAwaitingDownload` action" should {
    "redirect to Edit Awaiting Download page" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editAwaitingDownload(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("Awaiting Download")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.editAwaitingDownload(1)))
      }
    }
  }

  "The `viewLog` action" should {
    "redirect to View Log page" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.viewLog(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)

        status(result) must be equalTo OK
        contentType(result) must beSome(HTML_CONTENT_TYPE)
        contentAsString(result) must contain("View Log")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(pages.routes.ApplicationController.viewLog(1)))
      }
    }
  }

  "The `handleNewRequest` POST action" should {
    REDIRECT_TO_REQUESTS in new Context {
      new WithApplication(application) {
        val data = NewRequestForm.form.fill(mockInsertRequest).data.toSeq
        val request = FakeRequest(POST, pages.routes.ApplicationController.handleNewRequest().url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(data:_*).withHeaders(CSRF_BYPASS_HEADER:_*)
        val Some(result) = route(app, request)
        val Some(subfinalResult) = redirectResult(app, result)
        val Some(finalResult) = redirectResult(app, subfinalResult)

        val requestsFuture = requestService.retrieve()
        val testResult = for (r <- Await.result(requestsFuture, 30 seconds) if r.id == mockInsertRequest.id) yield r
        testResult must not beNull
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(POST, pages.routes.ApplicationController.handleNewRequest().url))
      }
    }
  }

  "The `handleEditRequest` POST action" should {
    REDIRECT_TO_REQUESTS in new Context {
      new WithApplication(application) {
        val requestForm = mockRequest.copy(phone = "9876543210")
        val data = NewRequestForm.form.fill(requestForm).data.toSeq
        val request = FakeRequest(POST, pages.routes.ApplicationController.handleEditRequest(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(data:_*).withHeaders(CSRF_BYPASS_HEADER:_*)
        val Some(result) = route(app, request)
        val Some(subfinalResult) = redirectResult(app, result)
        val Some(finalResult) = redirectResult(app, subfinalResult)

        val requestsFuture = requestService.retrieve()
        val testResult = for (r <- Await.result(requestsFuture, 30 seconds) if r.id == mockRequest.id && r.phone == "9876543210") yield r
        testResult must not beNull
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(POST, pages.routes.ApplicationController.handleEditRequest(1).url))
      }
    }
  }

  "The `handleRequirements` POST action" should {
    REDIRECT_TO_REQUESTS in new Context {
      new WithApplication(application) {
        val data = RequirementForm.form.fill(RequirementListData.apply(List("1", "2"))).data
        val request = FakeRequest(POST, pages.routes.ApplicationController.handleRequirements(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(data.toSeq:_*).withHeaders(CSRF_BYPASS_HEADER:_*)
        val Some(result) = route(app, request)
        val Some(subfinalResult) = redirectResult(app, result)
        val Some(finalResult) = redirectResult(app, subfinalResult)


        val(_,requirementsMap) = Await.result(requestService.getRequirements(mockRequest.id), 30 seconds)
        val(_,requirements) = requirementsMap
        val requirementsTitles = Await.result(requestService.getRequirementTitles(requirements), 30 seconds)
        requirementsTitles must contain("End-User Licensing Agreement")
        requirementsTitles must contain("Data Use Agreement")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(POST, pages.routes.ApplicationController.handleRequirements(1).url))
      }
    }
  }

  "The `handleProgress` POST action" should {
    REDIRECT_TO_REQUESTS in new Context {
      new WithApplication(application) {
        route(app, FakeRequest(POST, pages.routes.ApplicationController.handleRequirements(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(
            RequirementForm.form.fill(RequirementListData.apply(List("1", "2"))).data.toSeq:_*)
          .withHeaders(CSRF_BYPASS_HEADER:_*))
        val data = RequirementForm.form.fill(RequirementListData.apply(List("2"))).data
        val request = FakeRequest(POST, pages.routes.ApplicationController.handleProgress(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(data.toSeq:_*).withHeaders(CSRF_BYPASS_HEADER:_*)
        val Some(result) = route(app, request)
        val Some(subfinalResult) = redirectResult(app, result)
        val Some(finalResult) = redirectResult(app, subfinalResult)


        val (_,requirementsMap) = Await.result(requestService.getRequirementProgress(mockRequest.id), 30 seconds)
        for ((_, status, name) <- requirementsMap if name == "Data Use Agreement") yield status must beTrue
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(POST, pages.routes.ApplicationController.handleProgress(1).url))
      }
    }
  }

  "The `handleFileUpload` POST action" should {
    REDIRECT_TO_REQUESTS in new Context {
      new WithApplication(application) {
        val dataSequence = RequirementForm.form.fill(RequirementListData.apply(List("1", "2"))).data.toSeq
        route(app, FakeRequest(POST, pages.routes.ApplicationController.handleRequirements(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(dataSequence:_*)
          .withHeaders(CSRF_BYPASS_HEADER:_*))
        route(app, FakeRequest(POST, pages.routes.ApplicationController.handleProgress(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withFormUrlEncodedBody(dataSequence:_*).withHeaders(CSRF_BYPASS_HEADER:_*))

        val tempFile = TemporaryFile("temp.csv")
        Files.copy(Paths.get(getClass.getResource("/json.csv").toURI), tempFile.file.toPath, StandardCopyOption.REPLACE_EXISTING)
        val part = FilePart[TemporaryFile](key = "dataFile", filename = "temp.csv", contentType = Some("file"), ref = tempFile)
        val files = Seq(part)
        val dataParts = Map[String, Seq[String]](("params", Seq("fingerprint","watermark","signature","encrypt")))

        import util.MultipartFormDataWritable._
        val request = FakeRequest(POST, pages.routes.ApplicationController.handleFileUpload(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
          .withMultipartFormDataBody(MultipartFormData[TemporaryFile](
            dataParts = dataParts, files = files, badParts = Nil))
          .withHeaders(CSRF_BYPASS_HEADER:_*)
        route(app, request)

        val (uploadedFiles,_)= Await.result(requestService.getUniqueFiles(mockRequest.id), 30 seconds)
        uploadedFiles must not beEmpty

      }
    }

    "must be forbidden when unauthorized" in new Context {
      new WithApplication(application) {
        val request = FakeRequest(POST, pages.routes.ApplicationController.handleFileUpload(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(finalResult) = route(app, request)

        status(finalResult) must be equalTo FORBIDDEN
      }
    }
  }
  "The `handleWithdrawFile` POST action" should {
    REDIRECT_TO_REQUESTS in new Context {
      new WithApplication(application) {
        val temp = Seq("fingerprint","watermark","signature","encrypt")
        val uploadData = (temp zip temp)
        val tempFile = TemporaryFile("temp.csv")
        Files.copy(Paths.get(getClass.getResource("/json.csv").toURI), tempFile.file.toPath, StandardCopyOption.REPLACE_EXISTING)
        val part = FilePart[TemporaryFile](key = "dataFile", filename = "the.file", contentType = Some("text/csv"), ref = tempFile)
        val formData = MultipartFormData(dataParts = Map(), files = Seq(part), badParts = Seq())
        route(app, FakeRequest(POST, pages.routes.ApplicationController.handleFileUpload(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo).withMultipartFormDataBody(formData)
          .withFormUrlEncodedBody(uploadData:_*).withHeaders(CSRF_BYPASS_HEADER:_*))

        val request = FakeRequest(POST, pages.routes.ApplicationController.handleWithdrawFile(mockRequest.id).url)
          .withAuthenticator[DefaultEnv](identity.loginInfo).withHeaders(CSRF_BYPASS_HEADER:_*)
        val Some(result) = route(app, request)
        val Some(subfinalResult) = redirectResult(app, result)
        val Some(finalResult) = redirectResult(app, subfinalResult)


        val (files,_) = Await.result(requestService.getUniqueFiles(mockRequest.id), 30 seconds)
        files must beEmpty
        //val(_,requirements) = requirementsMap
//        val requirementsTitles = Await.result(requestService.getRequirementTitles(requirements), 30 seconds)
//        requirementsTitles must contain("End-User Licensing Agreement")
//        requirementsTitles must contain("Data Use Agreement")
      }
    }

    REDIRECT_TO_LOGIN in new Context {
      new WithApplication(application) {
        redirectLoginOnUnauthorized(app, FakeRequest(POST, pages.routes.ApplicationController.handleWithdrawFile(1).url))
      }
    }
  }
  /**
    * The context.
    */
  trait Context extends Scope {

    /**
      * A fake Guice module.
      */
    class FakeModule extends AbstractModule with ScalaModule {
      def configure(): Unit = {
        bind[Environment[DefaultEnv]].toInstance(env)
      }
    }

    def redirectUrl(someRoute: Future[Result]): String = redirectLocation(someRoute) match {
      case Some(s: String) => s
      case _ => ""
    }

    def redirectResult(app: Application, result: Future[Result]): Option[Future[Result]] = {
      val nextUrl = redirectUrl(result)
      route(app, FakeRequest(GET, nextUrl))
    }

    def redirectResultWithAuth(app: Application, result: Future[Result]): Option[Future[Result]] = {
      val nextUrl = redirectUrl(result)
      route(app, FakeRequest(GET, nextUrl).withAuthenticator[DefaultEnv](identity.loginInfo))
    }

    def redirectLoginOnUnauthorized(app: Application, request: FakeRequest[AnyContentAsEmpty.type]): MatchResult[String] = {
      val Some(result) = route(app, request)
      val Some(finalResult) = redirectResult(app, result)

      status(finalResult) must be equalTo OK
      contentType(finalResult) must beSome(HTML_CONTENT_TYPE)
      contentAsString(finalResult) must contain("Sign in to ISIDRO")
    }

    /**
      * An identity.
      */
    val identity = User(
      userID = UUID.randomUUID(),
      loginInfo = LoginInfo("facebook", "user@facebook.com"),
      firstName = None,
      lastName = None,
      fullName = None,
      email = None,
      avatarURL = None,
      activated = true
    )

    val mockRequest = DataRequest(
      id = 1,
      email = "user@facebook.com",
      title = "title",
      description = "desc",
      status = 0,
      pi = "pi",
      phone = "0123456789",
      cphs = "cphs")
    val mockInsertRequest = DataRequest(
      id = 2,
      email = "someotherdude@example.com",
      title = "title",
      description = "desc",
      status = 0,
      pi = "pi",
      phone = "0123456789",
      cphs = "cphs")

    /**
      * A Silhouette fake environment.
      */
    implicit val env: Environment[DefaultEnv] = new FakeEnvironment[DefaultEnv](Seq(identity.loginInfo -> identity))

    /**
      * The application.
      */
    lazy val application: Application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
      .build()
    lazy val requestService: RequestService = application.injector.instanceOf[RequestService]
    requestService.insert(mockRequest)
  }

}
