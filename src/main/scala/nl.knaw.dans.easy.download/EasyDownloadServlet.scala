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
import nl.knaw.dans.lib.error._
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
    def downloadFile(item: FileItem)(user: Option[User]): ActionResult = {
      app.downloadFile(request, uuid, path, item, user, () => response.outputStream)
        .map(_ => sendOkResponse(item))
        .getOrRecover(recoverAuthInfoOrDownload(uuid, path))
    }

    app.getFileItem(uuid, path) match {
      case Success(item) if item.isOpenAccess =>
        downloadFile(item)(Some(User(userName)))
      case Success(item) =>
        app.authenticate(authRequest)
          .map(downloadFile(item))
          .getOrRecover(recoverAuthenticate)
      case Failure(authInfoException) =>
        app.authenticate(authRequest)
          .map(_ => recoverAuthInfoOrDownload(uuid, path)(authInfoException))
          .getOrRecover(recoverAuthenticate)
    }
  }

  private def recoverAuthenticate(e: Throwable): ActionResult = e match {
    case InvalidUserPasswordException(_, _) => Unauthorized()
    case InvalidNameAuthenticationException(_) => Unauthorized()
    case NoPasswordAuthenticationException(_) => Unauthorized()
    case AuthenticationNotAvailableException(_) => ServiceUnavailable("Authentication service not available, try anonymous download")
    case AuthenticationTypeNotSupportedException(_) => BadRequest("Only anonymous download or basic authentication supported")
    case t =>
      logger.error(s"not expected exception", t)
      InternalServerError("not expected exception")
  }

  // overlap between errors from easy-auth-info and easy-bag-store is covered in this one function
  private def recoverAuthInfoOrDownload(uuid: UUID, path: Path)(e: Throwable): ActionResult = e match {
    case ServiceNotAvailableException(e) => ServiceUnavailable(e.getMessage)
    case AuthorisationNotAvailableException(e) => ServiceUnavailable(e.getMessage)
    case HttpStatusException(message, HttpResponse(_, SERVICE_UNAVAILABLE_503, _)) => ServiceUnavailable(message)
    case HttpStatusException(message, HttpResponse(_, REQUEST_TIMEOUT_408, _)) => RequestTimeout(message)
    case HttpStatusException(_, HttpResponse(_, NOT_FOUND_404, _)) => NotFound(s"not found: $uuid/$path")
    case NotAccessibleException(message) => Forbidden(message)
    case _: FileNotFoundException => NotFound(s"not found: $uuid/$path") // in fact: not visible
    case t =>
      logger.error(t.getMessage, t)
      InternalServerError("not expected exception")
  }

  private def sendOkResponse(fileItem: FileItem) = {
    response.addHeader("Link", getLicenseLinkText(fileItem))
    Ok()
  }

  private def getLicenseLinkText(fileItem: FileItem): String = {
    s"""<${ fileItem.licenseKey }>; rel="license"; title="${ fileItem.licenseTitle }""""
  }
}
