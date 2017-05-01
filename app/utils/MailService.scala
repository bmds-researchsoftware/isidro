package utils

import javax.inject.Inject
import com.google.inject.ImplementedBy
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.Configuration
import scala.concurrent.duration._
import scala.language.postfixOps
import java.net.URL

@ImplementedBy(classOf[MailServiceImpl])
trait MailService {
  def sendEmailAsync(recipients: String*)(subject: String, bodyHtml: String, bodyText: String): Unit
  def sendEmail(recipients: String*)(subject: String, bodyHtml: String, bodyText: String): Unit
}

class MailServiceImpl @Inject() (mailerClient: MailerClient, val conf: Configuration) extends MailService with ConfigSupport {

  lazy val from = confRequiredString("play.mailer.from")

  def sendEmailAsync(recipients: String*)(subject: String, bodyHtml: String, bodyText: String): Unit = {
    //Akka.system.scheduler.scheduleOnce(100 milliseconds) {
    //sendEmail(recipients: _*)(subject, bodyHtml, bodyText)
    //}
    println(s"send email async ${recipients.head}")
    sendEmail(recipients.head)(subject, bodyHtml, bodyText)
  }
  def sendEmail(recipients: String*)(subject: String, bodyHtml: String, bodyText: String): Unit = {
    println(s"send email $recipients\n\nbodyhtml\n$bodyHtml\n\nbodytext:\n$bodyText")
    mailerClient.send(Email(subject, from, recipients, Some(bodyText), Some(bodyHtml)))
  }

}
