package controllers

import javax.inject.{Inject, Singleton}
import io.swagger.annotations.ApiOperation
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  @ApiOperation(value = "", hidden = true)
  def redirectDocs = Action { implicit request =>
    Redirect(
      url = "/assets/lib/swagger-ui/index.html",
      queryStringParams = Map("url" -> Seq("http://" + request.host + "/swagger.json")))
  }
}
