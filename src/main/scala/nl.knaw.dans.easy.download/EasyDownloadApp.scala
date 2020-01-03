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

import java.io.OutputStream
import java.nio.file.Path
import java.util.UUID

import javax.servlet.http.HttpServletRequest
import nl.knaw.dans.easy.download.components.{ FileItem, LogEvent, User }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import scalaj.http.HttpResponse

import scala.util.{ Failure, Try }
import scala.xml.Elem

trait EasyDownloadApp extends DebugEnhancedLogging with ApplicationWiring {

  def authenticate(authRequest: BasicAuthRequest): Try[Option[User]] = authentication.authenticate(authRequest)

  /**
   * @param request Http request
   * @param bagId uuid of a bag
   * @param path  path of an item in files.xml of the bag
   * @param fileItem Fileitem object
   * @param user User object
   */
  def downloadFile(request: HttpServletRequest,
                   bagId: UUID,
                   path: Path,
                   fileItem: Try[FileItem],
                   user: Option[User],
                   outputStreamProducer: () => OutputStream
                  ): Try[Unit] = {
    for {
      fileItem <- fileItem
      _ <- fileItem.availableFor(user)
      _ <- bagStore.copyStream(bagId, path)(outputStreamProducer).recoverWith {
        case HttpStatusException(_, HttpResponse(_, NOT_FOUND_404, _)) =>
          Failure(new Exception(s"invalid bag, file downloadable but not found: $path"))
      }
      ddm <- getDDM(bagId)
      _ <- LogEvent(request, bagId, fileItem, user, ddm).logDownloadEvent
    } yield ()
  }

  def getFileItem(bagId: UUID, path: Path): Try[FileItem] = {
    authorisation.getFileItem(bagId, path)
  }

  def getDDM(bagId: UUID): Try[Elem] = {
    bagStore.loadDDM(bagId)
  }
}

object EasyDownloadApp {

  def apply(conf: Configuration): EasyDownloadApp = new EasyDownloadApp {
    override lazy val configuration: Configuration = conf
  }
}
