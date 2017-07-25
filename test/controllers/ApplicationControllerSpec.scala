package controllers

import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import controllers.pages.ApplicationController
import models.User
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Result
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
 * Test case for the [[ApplicationController]] class.
 */
class ApplicationControllerSpec extends PlaySpecification with Mockito {
  sequential

  "The `index` action" should {
    "redirect to login page" in new Context {
      new WithApplication(application) {
        val Some(result) = route(app, FakeRequest(pages.routes.ApplicationController.index))
        val Some(newResult) = redirectResult(app, result)

        status(newResult) must be equalTo OK
        contentType(newResult) must beSome("text/html")
        contentAsString(newResult) must contain("Sign in to ISIDRO")
      }
    }
  }

  "The `signOut` action" should {
    "redirect to home page if user is authorized" in new Context {
      new WithApplication(application) {

        val Some(result) = route(app,
          FakeRequest(pages.routes.ApplicationController.signOut).withAuthenticator[DefaultEnv](identity.loginInfo))

        val Some(finalResult) = redirectResultWithAuth(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("ISIDRO - Home")
      }
    }

    "redirect to login page if user is unauthorized" in new Context {
      new WithApplication(application) {
        val Some(result) = route(app,
          FakeRequest(pages.routes.ApplicationController.signOut))
        val Some(finalResult) = redirectResult(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("Sign in to ISIDRO")
      }
    }
  }

    "The `newRequest` action" should {
      "return 200 new request page if user is authorized" in new Context {
        new WithApplication(application) {

          val request = FakeRequest(pages.routes.ApplicationController.newRequest).withAuthenticator[DefaultEnv](identity.loginInfo)
          val Some(result) = route(app, request)

          status(result) must be equalTo OK
          contentType(result) must beSome("text/html")
          contentAsString(result) must contain("New Request")
        }
      }

      "redirect to login page if user is unauthorized" in new Context {
        new WithApplication(application) {

          val request = FakeRequest(pages.routes.ApplicationController.newRequest)
          val Some(result) = route(app, request)
          val Some(finalResult) = redirectResult(app, result)

          status(finalResult) must be equalTo OK
          contentType(finalResult) must beSome("text/html")
          contentAsString(finalResult) must contain("Sign in to ISIDRO")
        }
      }
    }

  "The `requests` action" should {
    "redirect to login page if user is unauthorized" in new Context {
      new WithApplication(application) {
        val Some(result) = route(app, FakeRequest(pages.routes.ApplicationController.requests(false))
          .withAuthenticator[DefaultEnv](LoginInfo("invalid", "invalid"))
        )

        val Some(finalResult) = redirectResult(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("Sign in to ISIDRO")
      }
    }
  }

    "return 200 requests page if user is authorized" in new Context {
      new WithApplication(application) {
        val Some(result) = route(app, FakeRequest(pages.routes.ApplicationController.requests(false))
          .withAuthenticator[DefaultEnv](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
        contentType(result) must beSome("text/html")
        contentAsString(result) must contain("Requests")
      }
    }


  "The `editRequest` action" should {
    "return 200 new request page if user is authorized" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editRequest(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)
        val Some(finalResult) = redirectResultWithAuth(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("New Request")
      }
    }

    "return 200 login page if user is unauthorized" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editRequest(1))
        val Some(result) = route(app, request)
        val Some(finalResult) = redirectResult(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("Sign in to ISIDRO")
      }
    }
  }

  "The `editRequirements` action" should {
    "return 200 new request page if user is authorized" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editRequirements(1)).withAuthenticator[DefaultEnv](identity.loginInfo)
        val Some(result) = route(app, request)
        val Some(finalResult) = redirectResultWithAuth(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("Edit Requirements") or contain("View Log")
      }
    }

    "return 200 login page if user is unauthorized" in new Context {
      new WithApplication(application) {

        val request = FakeRequest(pages.routes.ApplicationController.editRequirements(1))
        val Some(result) = route(app, request)
        val Some(finalResult) = redirectResult(app, result)

        status(finalResult) must be equalTo OK
        contentType(finalResult) must beSome("text/html")
        contentAsString(finalResult) must contain("Sign in to ISIDRO")
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
      def configure() = {
        bind[Environment[DefaultEnv]].toInstance(env)
      }
    }

     def redirectUrl(someRoute: Future[Result]) = redirectLocation(someRoute) match {
      case Some(s: String) => s
      case _ => ""
    }

    def redirectResult(app: Application, result: Future[Result]) = {
      val nextUrl = redirectUrl(result)
      route(app, FakeRequest(GET, nextUrl))
    }

     def redirectResultWithAuth(app: Application, result: Future[Result]) = {
      val nextUrl = redirectUrl(result)
      route(app, FakeRequest(GET, nextUrl).withAuthenticator[DefaultEnv](identity.loginInfo))
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

    /**
     * A Silhouette fake environment.
     */
    implicit val env: Environment[DefaultEnv] = new FakeEnvironment[DefaultEnv](Seq(identity.loginInfo -> identity))

    /**
     * The application.
     */
    lazy val application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
      .build()
  }
}
