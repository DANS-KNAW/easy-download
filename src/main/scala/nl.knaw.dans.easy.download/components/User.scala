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
sealed abstract class User(id: String, groups: Seq[String]) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit]

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit]

  // remove this one, is split into the two subcalls.
  def hasDownloadPermissionFor(fileItem: FileItemAuthInfo): Try[Unit] = {
    for {
      _ <- canView(fileItem)
      _ <- canAccess(fileItem)
    } yield ()
  }
}

case object AnonymousUser extends User("", Seq.empty) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (!fileItem.isDataFile)
      Failure(new FileNotFoundException(fileItem.itemId))
    else if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.isVisibleTo(ANONYMOUS))
      Success(())
    else
      Failure(new FileNotFoundException(fileItem.itemId))
  }

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.isAccessibleTo(ANONYMOUS))
      Success(())
    else if (fileItem.isAccessibleTo(KNOWN))
      Failure(NotAccessibleException(s"Please login to download: ${ fileItem.itemId }"))
    else
      Failure(NotAccessibleException(s"Download not allowed of: ${ fileItem.itemId }")) // might require group/permission
  }
}

case class KnownUser(id: String, groups: Seq[String] = Seq.empty) extends User(id, groups) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (id == fileItem.owner)
      Success(())
    else if (!fileItem.isDataFile)
      Failure(new FileNotFoundException(fileItem.itemId))
    else if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.isVisibleTo(ANONYMOUS) || fileItem.isVisibleTo(KNOWN))
      Success(())
    else
      Failure(new FileNotFoundException(fileItem.itemId))
  }

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = {
    if (id == fileItem.owner)
      Success(())
    else if (fileItem.hasEmbargo)
      Failure(NotAccessibleException(s"Download becomes available on ${ fileItem.dateAvailable } [${ fileItem.itemId }]"))
    else if (fileItem.isAccessibleTo(ANONYMOUS) || fileItem.isAccessibleTo(KNOWN))
      Success(())
    else
      Failure(NotAccessibleException(s"Download not allowed of: ${ fileItem.itemId }")) // might require group/permission
  }
}

case class ArchivistUser(id: String, groups: Seq[String] = Seq.empty) extends User(id, groups) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = Success(())

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = Success(())
}

case class AdminUser(id: String, groups: Seq[String] = Seq.empty) extends User(id, groups) {
  def canView(fileItem: FileItemAuthInfo): Try[Unit] = Success(())

  def canAccess(fileItem: FileItemAuthInfo): Try[Unit] = Success(())
}
