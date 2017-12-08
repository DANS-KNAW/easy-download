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

import scala.util.{ Failure, Success, Try }

// TODO not sure what the `groups` is used for; for now I just kept it in and gave them all an empty Seq
sealed abstract class AbstractUser(id: String, groups: Seq[String]) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit]

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit]
}

case object UnauthenticatedUser extends AbstractUser("", Seq.empty) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (!fileItem.isDataFile)
      Failure(new FileNotFoundException(fileItem.itemId))
    else if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.visibleToValue == ANONYMOUS)
      Success(())
    else
      Failure(new FileNotFoundException(fileItem.itemId))
  }

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.accessibleToValue == ANONYMOUS)
      Success(())
    else if (fileItem.accessibleToValue == KNOWN)
      Failure(NotAccessibleException(s"Please login to download: ${ fileItem.itemId }"))
    else
      Failure(NotAccessibleException(s"Download not allowed of: ${ fileItem.itemId }")) // might require group/permission
  }
}

case class AuthenticatedUser(id: String, groups: Seq[String] = Seq.empty) extends AbstractUser(id, groups) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (id == fileItem.owner)
      Success(())
    else if (!fileItem.isDataFile)
      Failure(new FileNotFoundException(fileItem.itemId))
    else if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.visibleToValue == ANONYMOUS || fileItem.visibleToValue == KNOWN)
      Success(())
    else
      Failure(new FileNotFoundException(fileItem.itemId))
  }

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (id == fileItem.owner)
      Success(())
    else if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.accessibleToValue == ANONYMOUS || fileItem.accessibleToValue == KNOWN)
      Success(())
    else
      Failure(NotAccessibleException(s"Download not allowed of: ${ fileItem.itemId }")) // might require group/permission
  }
}

case class ArchivistUser(id: String, groups: Seq[String] = Seq.empty) extends AbstractUser(id, groups) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = Success(())

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = Success(())
}

case class AdminUser(id: String, groups: Seq[String] = Seq.empty) extends AbstractUser(id, groups) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = Success(())

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = Success(())
}
