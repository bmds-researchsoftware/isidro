package forms.pages

import play.api.data.Form
import play.api.data.Forms._

import models.DataRequest

/**
 * The form which handles new requests
 */
object NewRequestForm {

  val form = Form(
    mapping(
      "id" -> ignored(0),
      "email" -> email,
      "title" -> nonEmptyText(maxLength = 254),
      "description" -> nonEmptyText(maxLength = 1024),
      "status" -> ignored(0),
      "pi" -> nonEmptyText(maxLength = 254),
      "phone" -> text(maxLength = 64),
      "cphs" -> text(maxLength = 64))(DataRequest.apply _)(DataRequest.unapply _))
}
