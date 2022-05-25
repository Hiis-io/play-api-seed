package services

import models.rest.Todo
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

/**
 * Created by Abanda Ludovic on 19/05/2022.
 */

class TodoServiceImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit ex: ExecutionContext) extends TodoService {

  def todos = reactiveMongoApi.database.map(_.collection[JSONCollection]("todos"))

  /**
   * Save a user _Todo_ object in database
   *
   * @param todo the todo object
   * @return Future[_Todo_]
   */
  override def save(todo: Todo): Future[Todo] = todos.flatMap(_.insert.one(todo))
    .flatMap(_ => Future.successful(todo))

  /**
   * Delete a _Todo_ object in database
   *
   * @param id
   * @return Future[True] if operation was successful else Future[False]
   */
  override def delete(id: String, userID: String): Future[Boolean] = todos.flatMap(_.delete.one(Json.obj("userID" -> userID, "id" -> id)))
    .flatMap(_ => Future.successful(true))

  /**
   * Update a _Todo_ object in database
   *
   * @param todo
   * @return Future[True] if operation was successful else Future[False]
   */
  override def update(todo: Todo): Future[Todo] = todos.flatMap(_.update.one(Json.obj("id" -> todo.id), todo))
    .flatMap(_ => Future.successful(todo))

  /**
   * Get a particular _Todo_ object from database
   *
   * @param id
   * @return Future[Some(_Todo_)] if found or Future[None] otherwise
   */
  override def get(id: String): Future[Option[Todo]] = todos.flatMap(_.find(Json.obj("id" -> id), projection = Option.empty[JsObject])
    .one[Todo])

  /**
   * Get the list of user Todos from the database
   * TODO Add pagination
   *
   * @param userID
   * @return Future[List[_Todo_]] if user Todos were found or Future[List.empty] otherwise
   */
override def getUserTodos(userID: String): Future[List[Todo]] = todos.flatMap(_.find(Json.obj("userID" -> userID), projection = Option.empty[JsObject])
  .cursor[Todo](ReadPreference.Primary).collect[List](Int.MaxValue, Cursor.FailOnError[List[Todo]]()))
}
