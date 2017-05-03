import org.scalatest._
import play.api.test.Helpers._
import play.api.test._
import org.scalatest.matchers._
import org.scalatest.selenium._
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.WebElement
import org.scalatest.concurrent._

class IsidroSpec extends FlatSpec with Matchers with HtmlUnit {
  val host = "http://localhost:9000/"

  "The sign in page" should "have the correct title" in {
    go to (host + "signIn")
    pageTitle should be("Sign in")
  }

  "Signing in with a bad login" should "be an error" in {
    go to (host + "signIn")
    emailField("email").value = "bademail@no.edu"
    pwdField("password").value = "invalid"
    submit()
    pageTitle should be("Sign in")
  }

  "Signing in with a good login" should "go to home page" in {
    go to (host + "signIn")
    emailField("email").value = "test.user@example.com"
    pwdField("password").value = "xyzzy123"
    submit()
    pageTitle should be("ISIDRO - Home")
  }

  "Requests" should "list requests" in {
    click on linkText("Requests")
    pageTitle should be("Incoming Requests")
  }

}
