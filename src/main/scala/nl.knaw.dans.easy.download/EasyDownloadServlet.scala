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
import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.easy.download.components.{ FileItem, User }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet._
import nl.knaw.dans.lib.string._
import org.eclipse.jetty.http.HttpStatus._
import org.scalatra._
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import scalaj.http.HttpResponse

import scala.util.{ Failure, Success, Try }

class EasyDownloadServlet(app: EasyDownloadApp) extends ScalatraServlet
  with ServletLogger
  with PlainLogFormatter
  with LogResponseBodyOnError
  with DebugEnhancedLogging {
  logger.info("File Download Servlet running...")

  private val naan = app.configuration.properties.getString("ark.name-assigning-authority-number")

  get("/") {
    contentType = "text/plain"
    Ok(s"EASY Download Service running v${ app.configuration.version }.")
  }

  get(s"/ark:/$naan/:uuid/*") {
    val authRequest = new BasicAuthRequest(request)
    val userName = { Option(authRequest.username).getOrElse("ANONYMOUS") }
    logger.info(s"file download requested by $userName for $params")

    (getUUID, getPath) match {
      case (Success(uuid), Success(Some(path))) => download(authRequest, userName, uuid, path)
      case (Success(_), Success(None)) => MethodNotAllowed("file path is empty")
      case (Failure(t), _) => NotFound(t.getMessage) // invalid uuid
      case (_, Failure(t)) => BadRequest(t.getMessage) // invalid path
    }
  }

  private def getUUID = {
    correctHyphenation(params("uuid")).toUUID.toTry
  }

  private def correctHyphenation(uuid: String): String = {
    // In ARK identifiers hyphens are considered to be insignificant, that is why we here
    // add the hyphens to a uuid string that doesn't contain any hyphens and whose length is 32 characters
    // (lenghth of a valid UUID without hyphens).
    if (!uuid.contains("-") && uuid.length == 32)
      s"${ uuid.slice(0, 8) }-${ uuid.slice(8, 12) }-${ uuid.slice(12, 16) }-${ uuid.slice(16, 20) }-${ uuid.slice(20, 32) }"
    else
      uuid
  }

  private def getPath = Try {
    multiParams("splat").find(!_.trim.isEmpty).map(Paths.get(_))
  }

  private def download(authRequest: BasicAuthRequest, userName: String, uuid: UUID, path: Path) = {
    // When fileItem is a Failure, it is transformed into a None for getUser(...); in that case authentication is guaranteed to be performed.
    // If authentication succeeds, the original error caused by fileItem surfaces from app.downloadFile(...);
    // if authentication fails, priority is given to that error above the error in fileItem.
    val fileItem = app.getFileItem(uuid, path)
    getUser(authRequest, userName, fileItem.toOption) match {
      case Success(user) => respond(uuid, path, fileItem, app.downloadFile(uuid, path, fileItem, user, () => response.outputStream))
      case Failure(InvalidUserPasswordException(_, _)) => Unauthorized()
      case Failure(AuthenticationNotAvailableException(_)) => ServiceUnavailable("Authentication service not available, try anonymous download")
      case Failure(AuthenticationTypeNotSupportedException(_)) => BadRequest("Only anonymous download or basic authentication supported")
      case Failure(t) =>
        logger.error(s"not expected exception", t)
        InternalServerError("not expected exception")
    }
  }

  private def getUser(authRequest: BasicAuthRequest, userName: String, fileItem: Option[FileItem]): Try[Option[User]] = {
    if (fileItem.nonEmpty && fileItem.get.isOpenAccess)
      Success(Some(User(userName, Seq.empty)))
    else
      app.authenticate(authRequest)
  }

  private def respond(uuid: UUID, path: Path, fileItem: Try[FileItem], copyResult: Try[Unit]) = {
    copyResult match {
      case Success(()) => sendOkResponse(fileItem)
      case Failure(HttpStatusException(message, HttpResponse(_, SERVICE_UNAVAILABLE_503, _))) => ServiceUnavailable(message)
      case Failure(HttpStatusException(message, HttpResponse(_, REQUEST_TIMEOUT_408, _))) => RequestTimeout(message)
      case Failure(HttpStatusException(_, HttpResponse(_, NOT_FOUND_404, _))) => NotFound(s"not found: $uuid/$path")
      case Failure(NotAccessibleException(message)) => Forbidden(message)
      case Failure(_: FileNotFoundException) => NotFound(s"not found: $uuid/$path") // in fact: not visible
      case Failure(t) =>
        logger.error(t.getMessage, t)
        InternalServerError("not expected exception")
    }
  }

  private def sendOkResponse(fileItem: Try[FileItem]) = {
    getLicenseLinkText(fileItem).foreach(response.addHeader("Link", _))
    Ok()
  }

  private def getLicenseLinkText(fileItem: Try[FileItem]): Option[String] = {
    fileItem.map(file => Some(s"""<${ file.licenseKey }>; rel="license"; title="${ file.licenseTitle }"""")).getOrElse(None)
  }
}
