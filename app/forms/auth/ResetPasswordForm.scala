package forms.auth

import play.api.data.Forms._
import play.api.data._

/**
 * The `Reset Password` form.
 */
object ResetPasswordForm {

  /**
   * A play framework form.
   */
  val form = Form(
    "password" -> tuple(
      "main" -> nonEmptyText,
      "confirm" -> nonEmptyText
    ).verifying(
        "Passwords don't match", password => password._1 == password._2
      ).transform[String](
          password => password._1,
          password => ("", "")
        )
  )
}
