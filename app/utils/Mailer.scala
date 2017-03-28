package utils

import models.User
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.mails
import slick.driver.JdbcProfile

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def welcome(user: User, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(user.email)(
      subject = Messages("mail.welcome.subject"),
      bodyHtml = mails.welcome(user.firstName, link),
      bodyText = mails.welcomeTxt(user.firstName, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

  def sendDownloadEmail(email: String, link: String)(implicit ms: MailService, m: Messages)={
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
