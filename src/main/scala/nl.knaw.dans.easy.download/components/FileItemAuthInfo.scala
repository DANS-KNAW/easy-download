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
package nl.knaw.dans.easy.download.components

import java.io.FileNotFoundException

import nl.knaw.dans.easy.download.NotAccessibleException
import nl.knaw.dans.easy.download.components.RightsFor._
import org.joda.time.DateTime

import scala.util.{ Failure, Success, Try }

case class FileItemAuthInfo(itemId: String,
                            owner: String,
                            dateAvailable: String,
                            accessibleTo: String,
                            visibleTo: String
                           ) {
  private val dateAvailableMillis: Long = new DateTime(dateAvailable).getMillis

  // TODO json type hints in AuthInfoComponent to replace argument type String by RightsFor
  private val visibleToValue = RightsFor.withName(visibleTo)
  private val accessibleToValue = RightsFor.withName(accessibleTo)

  def hasDownloadPermissionFor(user: AbstractUser): Try[Unit] = {
    for {
      _ <- visibleTo(user)
      _ <- accessibleTo(user)
    } yield ()
  }

  private def visibleTo(user: AbstractUser): Try[Unit] = {
    user match {
      case ArchivistUser(_, _) | AdminUser(_, _) => Success(())
      case AuthenticatedUser(id, _) if id == owner => Success(())
      case _ if !isDataFile => Failure(new FileNotFoundException(itemId))
      case _ if hasEmbargo => Failure(NotAccessibleException(s"Download becomes available on $dateAvailable [$itemId]"))
      case usr if isVisible(usr) => Success(())
      case _ => Failure(new FileNotFoundException(itemId))
    }
  }

  private def isDataFile: Boolean = itemId.matches("[^/]+/data/.*") // "[^/]+" matches the uuid of the bag
  private def hasEmbargo: Boolean = dateAvailableMillis > DateTime.now.getMillis
  private def isVisible(user: AbstractUser): Boolean = {
    user match {
      case UnauthenticatedUser => visibleToValue == ANONYMOUS
      case _ => visibleToValue == ANONYMOUS || visibleToValue == KNOWN
    }
  }

  private def accessibleTo(user: AbstractUser): Try[Unit] = {
    user match {
      case ArchivistUser(_, _) | AdminUser(_, _) => Success(())
      case AuthenticatedUser(id, _) if id == owner => Success(())
      case _ if hasEmbargo => Failure(NotAccessibleException(s"Download becomes available on $dateAvailable [$itemId]"))
      case _ if accessibleToValue == ANONYMOUS => Success(())
      case UnauthenticatedUser if accessibleToValue == KNOWN => Failure(NotAccessibleException(s"Please login to download: $itemId"))
      case _ if accessibleToValue == KNOWN => Success(())
      case _ => Failure(NotAccessibleException(s"Download not allowed of: $itemId")) // might require group/permission
    }
  }
}
