package controllers

import models._
import tables._
import utils.silhouette._
import play.api.data._
import play.api.data.Forms._
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile
import play.api.i18n.{ MessagesApi, Messages, Lang, I18nSupport }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.Inject
import views.html.{ auth => viewsAuth }

class Application @Inject() (val env: AuthenticationEnvironment, val messagesApi: MessagesApi) extends AuthenticationController with I18nSupport with DataRequestTable with RequirementTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._

  val dataRequests = TableQuery[DataRequests]
  val requirements = TableQuery[Requirements]

  def index = UserAwareAction.async { implicit request =>
    Future.successful(Redirect(routes.Application.myRequests))
  }

  def myAccount = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.myAccount()))
  }

  val newRequestForm = Form(mapping(
    "id" -> ignored(0),
    "userId" -> ignored(0L),
    "title" -> nonEmptyText,
    "description" -> nonEmptyText)
    (DataRequest.apply _ )(DataRequest.unapply _))

  def myRequests = SecuredAction.async { implicit request =>
    db.run(dataRequests.filter(_.userId === request.identity.id)result).map(req =>
      Ok(views.html.requests(req.toList)))
  }
  
  def newRequest = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.newRequest(newRequestForm)))
  }

  // REQUIRED ROLES: master
  def requests = SecuredAction(WithService("master")).async { implicit request =>
    db.run(dataRequests.result).map(req =>
      Ok(views.html.brokerRequests(req.toList)))
  }
  
  

  def request(rid: Int) = SecuredAction.async { implicit request =>
    val q = for {
      r <- dataRequests if r.id === rid
      u <- r.user if u.id === request.identity.id
    } yield(r.id, r.title, u.firstName, u.lastName, u.id)

    db.run(q.result).map(req => {
      Ok(views.html.request(req.map(r => {
        DataReq.tupled(r);
      }).headOption))
    })
  }

  def brokerRequest(rid: Int) = SecuredAction(WithService("master")).async { implicit request =>
    val q = for {
      r <- dataRequests if r.id === rid
      u <- r.user
    } yield(r.id, r.title, u.firstName, u.lastName, u.id)

    val q2 = requirements

    db.run(q.result.zip(q2.sortBy(r => r.order).result)).map(req => {
      Ok(views.html.brokerRequest(req._1.map(r => DataReq.tupled(r)).headOption, req._2.toList))
    })
  }

  def handleNewRequest = SecuredAction.async { implicit request =>
    newRequestForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.newRequest(formWithErrors))),
      req => {
        println("success")
        println(req)
        println(req.title)
        val fullReq = req.copy(userId = request.identity.id)
        db.run(dataRequests += fullReq).map(_ => Redirect(routes.Application.myRequests))
      }
    )
  }
}
