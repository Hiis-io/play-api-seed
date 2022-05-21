package controllers

import auth.DefaultEnv
import com.mohiva.play.silhouette.api.Silhouette
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.rest.Todo
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.TodoService
import utils.Logger

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Abanda Ludovic on 19/05/2022.
 */

@Api(value = "Todo")
class SampleController @Inject()(controllerComponents: ControllerComponents,
                                 silhouette: Silhouette[DefaultEnv],
                                 todoService: TodoService)(implicit ex: ExecutionContext)
  extends AbstractController(controllerComponents) with Logger {

  @ApiOperation(value = "Add new Todo to user Todos", response = classOf[Todo])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "Todo",
        required = true,
        dataType = "models.rest.Todo",
        paramType = "body")))
  def addTodo() = silhouette.SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[Todo].map { todo =>
      todoService.save(todo.copy(isCompleted = false, userID = Some(request.identity.id.get), id = Some(UUID.randomUUID().toString)))
        .flatMap(inserted => Future.successful(Ok(Json.toJson(inserted))))
    }.recoverTotal(_ =>
      Future.successful(BadRequest))
    Future.successful(Ok)
  }

  @ApiOperation(value = "Add new Todo to user Todos", response = classOf[Todo])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "id",
        required = true,
        dataType = "String",
        paramType = "path")))
  def getTodo(id: String) = silhouette.SecuredAction.async { implicit request =>
    todoService.get(id).flatMap {
      case Some(todo) if todo.userID.get == request.identity.id.get => Future.successful(Ok(Json.toJson(todo)))
      case _ => Future.successful(NotFound)
    }
  }


  @ApiOperation(value = "Delete a user Todo", response = classOf[Todo])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "id",
        required = true,
        dataType = "String",
        paramType = "path")))
  def deleteTodo(id: String) = silhouette.SecuredAction.async { implicit request =>
    todoService.delete(id, request.identity.id.get).flatMap(_ => Future.successful(Ok))
  }

  @ApiOperation(value = "Update user Todo", response = classOf[Todo])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header"),
      new ApiImplicitParam(
        value = "Todo",
        required = true,
        dataType = "models.rest.Todo",
        paramType = "body")))
  def updateTodo() = silhouette.SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[Todo].map { todo =>
      todoService.update(todo.copy(userID = Some(request.identity.id.get))).flatMap(updated => Future.successful(Ok(Json.toJson(updated))))
    }.recoverTotal(_ =>
      Future.successful(BadRequest))
  }

  @ApiOperation(value = "Get all user Todos", response = classOf[Array[Todo]])
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "X-Auth-Token",
        value = "User access token",
        required = true,
        dataType = "string",
        paramType = "header")))
  def getTodos() = silhouette.SecuredAction.async { implicit request =>
    todoService.getUserTodos(request.identity.id.get).flatMap(todos => Future.successful(Ok(Json.toJson(todos))))
  }
}
