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
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "status" -> ignored(0),
      "pi" -> nonEmptyText,
      "phone" -> text,
      "cphs" -> text)(DataRequest.apply _)(DataRequest.unapply _))
}
