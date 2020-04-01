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

import java.io.{ FileNotFoundException, OutputStream }
import java.nio.file.Path
import java.util.UUID

import javax.servlet.http.HttpServletRequest
import nl.knaw.dans.easy.download.components.{ FileItem, RightsFor, Statistics, User }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404
import org.joda.time.format.DateTimeFormat
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import scalaj.http.HttpResponse

import scala.util.{ Failure, Success, Try }
import scala.xml.Elem

trait EasyDownloadApp extends DebugEnhancedLogging with ApplicationWiring {

  def authenticate(authRequest: BasicAuthRequest): Try[Option[User]] = authentication.authenticate(authRequest)

  /**
   * @param request  Http request
   * @param bagId    uuid of a bag
   * @param path     path of an item in files.xml of the bag
   * @param fileItem Fileitem object
   * @param user     User object
   */
  def downloadFile(request: HttpServletRequest,
                   bagId: UUID,
                   path: Path,
                   fileItem: FileItem,
                   user: Option[User],
                   outputStreamProducer: () => OutputStream
                  ): Try[Unit] = {
    for {
      _ <- hasDownloadPermissionFor(bagId, fileItem, user)
      _ <- bagStore.copyStream(bagId, path)(outputStreamProducer).recoverWith {
        case HttpStatusException(_, HttpResponse(_, NOT_FOUND_404, _)) =>
          Failure(new Exception(s"invalid bag, file downloadable but not found: $path"))
      }
      ddm <- getDDM(bagId)
      _ <- Statistics(request, bagId, fileItem, user, ddm).logDownload
    } yield ()
  }

  def hasDownloadPermissionFor(bagId: UUID, fileItem: FileItem, user: Option[User]): Try[Unit] = {
    val itemId = fileItem.itemId

    lazy val userIsKnown: Boolean = user.isDefined
    lazy val userHasPermission: Option[Boolean] = {
      user.map(u => {
        permissionRequest.userHasPermission(u.id, bagId)
          .doIfFailure {
            case e => logger.error(s"An error occurred while checking whether user '${ u.id }' has permission to download file '${ fileItem.itemId }': ${ e.getMessage }", e)
          }
          .getOrElse(false)
      })
    }
    lazy val userInGroup: Option[Boolean] = {
      user.map(u => u.groups contains "Archaeology")
    }

    def embagroFailure = {
      val date = DateTimeFormat.forPattern("yyyy-MM-dd").print(fileItem.dateAvailable)
      Failure(NotAccessibleException(s"Download becomes available on $date [${ fileItem.itemId }]"))
    }

    def pleaseLoginFailure = Failure(NotAccessibleException(s"Please login to download: $itemId"))

    def downloadNotAllowedFailure = Failure(NotAccessibleException(s"Download not allowed of: $itemId"))

    def isVisibleTo(user: Option[User]): Boolean = {
      fileItem.visibleTo == RightsFor.ANONYMOUS ||
        (fileItem.visibleTo == RightsFor.KNOWN && userIsKnown) ||
        (fileItem.visibleTo == RightsFor.RESTRICTED_GROUP && userInGroup.getOrElse(false)) ||
        (fileItem.visibleTo == RightsFor.RESTRICTED_REQUEST && userHasPermission.getOrElse(false))
    }

    if (user.exists(_.id == fileItem.owner)) Success(())
    else if (!isVisibleTo(user)) Failure(new FileNotFoundException(itemId))
    else if (fileItem.dateAvailable.isAfterNow) embagroFailure
    else if (fileItem.accessibleTo == RightsFor.ANONYMOUS) Success(())
    else if (fileItem.accessibleTo == RightsFor.KNOWN) {
      if (userIsKnown) Success(())
      else pleaseLoginFailure
    }
    else if (fileItem.accessibleTo == RightsFor.RESTRICTED_GROUP) {
      userInGroup
        .map {
          case true => Success(())
          case false => downloadNotAllowedFailure
        }
        .getOrElse(pleaseLoginFailure)
    }
    else if (fileItem.accessibleTo == RightsFor.RESTRICTED_REQUEST) {
      userHasPermission
        .map {
          case true => Success(())
          case false => downloadNotAllowedFailure
        }
        .getOrElse(pleaseLoginFailure)
    }
    else downloadNotAllowedFailure
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
