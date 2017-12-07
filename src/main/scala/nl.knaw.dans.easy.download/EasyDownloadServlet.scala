/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.download

import java.io.FileNotFoundException
import java.nio.file.Paths
import java.util.UUID

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.http.HttpStatus.{ NOT_FOUND_404, REQUEST_TIMEOUT_408, SERVICE_UNAVAILABLE_503 }
import org.scalatra._
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest

import scala.util.{ Failure, Success, Try }
import scalaj.http.HttpResponse

class EasyDownloadServlet(app: EasyDownloadApp) extends ScalatraServlet with DebugEnhancedLogging {
  logger.info("File Download Servlet running...")

  private val naan = app.configuration.properties.getString("ark.name-assigning-authority-number")

  get("/") {
    contentType = "text/plain"
    Ok("EASY Download Service running...")
  }

  get(s"/ark:/$naan/:uuid/*") {
    (getUUID, getPath, getUser) match {
      case (Success(uuid), Success(Some(path)), Success(user)) => respond(s"$uuid/$path", app.copyStream(uuid, path, user, () => response.outputStream))
      case (Success(_), Success(None), _) => BadRequest("file path is empty")
      case (_, _, Failure(InvalidUserPasswordException(_, _))) => Unauthorized()
      case (_, _, Failure(AuthenticationNotAvailableException(_))) => ServiceUnavailable("Authentication service not available, try anonymous download")
      case (_, _, Failure(AuthenticationTypeNotSupportedException(_))) => BadRequest("Only anonymous download or basic authentication supported")
      case (Failure(t), _, _) => BadRequest(t.getMessage)
      case (_, Failure(t), _) => BadRequest(t.getMessage)
      case _ =>
        logger.error(s"not expected request: $params")
        InternalServerError("not expected exception")
    }
  }

  private def getUser = {
    app.authenticate(new BasicAuthRequest(request))
  }

  private def getUUID = Try {
    UUID.fromString(params("uuid"))
  }

  private def getPath = Try {
    multiParams("splat").find(!_.trim.isEmpty).map(Paths.get(_))
  }

  private def respond(path: String, copyResult: Try[Unit]) = {
    copyResult match {
      case Success(()) => Ok()
      case Failure(HttpStatusException(message, HttpResponse(_, SERVICE_UNAVAILABLE_503, _))) => ServiceUnavailable(message)
      case Failure(HttpStatusException(message, HttpResponse(_, REQUEST_TIMEOUT_408, _))) => RequestTimeout(message)
      case Failure(HttpStatusException(_, HttpResponse(_, NOT_FOUND_404, _))) => NotFound(s"not found: $path")
      case Failure(NotAccessibleException(message)) => Forbidden(message)
      case Failure(_: FileNotFoundException) => NotFound(s"not found: $path")// in fact: not visible
      case Failure(t) =>
        logger.error(t.getMessage, t)
        InternalServerError("not expected exception")
    }
  }
}