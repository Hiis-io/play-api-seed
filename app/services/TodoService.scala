package services

import models.rest.Todo

import scala.concurrent.Future

/**
 * Created by Abanda Ludovic on 19/05/2022.
 */

trait TodoService {
  /**
   * Save a user _Todo_ object in database
   *
   * @param todo
   * @return Future[True] if operation was successful else Future[False]
   */
  def save(todo: Todo): Future[Todo]

  /**
   * Delete a _Todo_ object in database
   *
   * @param id
   * @return Future[True] if operation was successful else Future[False]
   */
  def delete(id: String, userID: String): Future[Boolean]

  /**
   * Update a _Todo_ object in database
   *
   * @param todo
   * @return Future[True] if operation was successful else Future[False]
   */
  def update(todo: Todo): Future[Todo]

  /**
   * Get a particular _Todo_ object from database
   * @param id
   * @return Future[Some(_Todo_)] if found or Future[None] otherwise
   */
  def get(id: String): Future[Option[Todo]]

  /**
   * Get the list of user Todos from the database
   * @param userID
   * @return Future[List[_Todo_]] if user Todos were found or Future[List.empty] otherwise
   */
  def getUserTodos(userID: String): Future[List[Todo]]
}
