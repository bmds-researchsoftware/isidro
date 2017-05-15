import org.scalatest._
import java.awt.Robot
import java.awt.event.KeyEvent
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

  "Signing in" should "fail with invalid credentials" in {
    go to (host + "signIn")
    emailField("email").value = "bademail@no.edu"
    pwdField("password").value = "invalid"
    submit()
    pageTitle should be("Sign in")
  }

  it should "go to home page with good credentials" in {
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

  "All existing requests" should "should be closed" in {
    while (find("requestrow") != None) {
      click on "requestrow"
      val u = currentUrl.split("edit")(0) + "edit5"
      go to (u)
      pageTitle should startWith("Downloaded Request: ")
      click on "close"
      pageTitle should be("Incoming Requests")
    }
  }

  "New Request" should "fail with a bad email address" in {
    click on linkText("New Request")
    pageTitle should be("New Request")
    textField("title").value = "Selenium Test"
    textArea("description").value = "This test was automatically added by selenium."
    emailField("email").value = "test.requestexample.com"
    textField("phone").value = "800-555-1212"
    textField("pi").value = "Dr Jones"
    textField("cphs").value = "nn123"
    submit()
    pageTitle should be("Update Request")
  }

  it should "work with good data" in {
    emailField("email").value = "test.request@example.com"
    submit()
    pageTitle should be("Incoming Requests")
  }

  "Request List" should "show the new request" in {
    click on "requestrow"
    pageTitle should be("Edit Request Requirements")
    click on "submit"
    click on "requestrow"
    pageTitle should be("Edit Requirement Progress")
    click on "submit"
    click on "requestrow"
    pageTitle should be("Send File")
    checkbox("encrypt").clear()
    checkbox("signature").clear()
    checkbox("phi11").select()
    click on "dataFile"
    pressKeys("/home/rdj/docs/phi/orgs.csv")
    submit()
    /*val robot = new Robot()
    "/home/rdj/docs/phi/orgs.csv".foreach(c =>
      robot.keyPress(c.toInt)
    )
    robot.keyRelease(KeyEvent.VK_ENTER)*/
    //click on "submit"
  }

}
