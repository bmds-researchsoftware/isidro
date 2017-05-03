package utils

import models.User
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.mails
import scala.language.implicitConversions
import slick.driver.JdbcProfile

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def welcome(user: User, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(user.email.head)(
      subject = Messages("mail.welcome.subject"),
      bodyHtml = mails.welcome(
        user.firstName.head,
        link),
      bodyText = mails.welcomeTxt(user.firstName.head, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

  def sendDownloadEmail(email: String, link: String)(implicit ms: MailService, m: Messages) = {
    val mailTxt = mails.downloadLinkTxt(link)
    ms.sendEmailAsync(email)(
      subject = Messages("mail.download.subject"),
      bodyHtml = mails.downloadLink(link),
      bodyText = mailTxt
    )
    mailTxt
  }

  def sendPasswordEmail(email: String, password: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("mail.download.subject"),
      bodyHtml = mails.password(password),
      bodyText = mails.passwordTxt(password)
    )
  }
}
