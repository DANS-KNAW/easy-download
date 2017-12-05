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
  private val dateAvailableMilis: Long = new DateTime(dateAvailable).getMillis

  // TODO apply json type hints in AuthInfoComponent to change type of arguments
  private val visibleToValue = RightsFor.withName(visibleTo)
  private val accessibleToValue = RightsFor.withName(accessibleTo)

  def visibleTo(user: Option[User]): Try[Unit] = {
    (user, visibleToValue) match {
      case (None, ANONYMOUS) => Success(())
      case (None, _) => Failure(new FileNotFoundException(itemId))
      case (Some(_), _) => Failure(new NotImplementedError())
    }
  }

  def accessibleTo(user: Option[User]): Try[Unit] = {
    (user, accessibleToValue) match {
      case (None, ANONYMOUS) => Success(())
      case (None, _) => Failure(NotAccessibleException(s"download not allowed of: $itemId"))
      case (Some(_), _) => Failure(new NotImplementedError())
    }
  }

  def noEmbargo: Try[Unit] = {
    if (dateAvailableMilis <= DateTime.now.getMillis) Success(())
    else Failure(NotAccessibleException(s"download becomes available on $dateAvailableMilis [$itemId]"))
  }
}
