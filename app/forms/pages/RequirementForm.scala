package forms.pages

import play.api.data.Form
import play.api.data.Forms._

import models.DataRequest

/**
 * The form which handles a list of requirements
 * val requirementForm = Form(mapping(
 * "rq" -> list(text)
 * )(RequirementListData.apply)(RequirementListData.unapply _))
 *
 */

case class RequirementListData(rawReqs: List[String]) {
  def reqs = rawReqs.map(_.toInt)
}

object RequirementForm {

  val form = Form(
    mapping(
      "rq" -> list(text))(RequirementListData.apply _)(RequirementListData.unapply _))
}
