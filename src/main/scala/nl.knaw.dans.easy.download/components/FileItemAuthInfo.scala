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

import nl.knaw.dans.easy.download.components.RightsFor._
import org.joda.time.DateTime

case class FileItemAuthInfo(itemId: String,
                            owner: String,
                            dateAvailable: String,
                            accessibleTo: String,
                            visibleTo: String
                           ) {
  private val dateAvailableMillis: Long = new DateTime(dateAvailable).getMillis

  // TODO json type hints in AuthInfoComponent to replace argument type String by RightsFor
  val visibleToValue = RightsFor.withName(visibleTo)
  val accessibleToValue = RightsFor.withName(accessibleTo)

  def isDataFile: Boolean = itemId.matches("[^/]+/data/.*") // "[^/]+" matches the uuid of the bag
  def hasEmbargo: Boolean = dateAvailableMillis > DateTime.now.getMillis
  def isVisible(user: User): Boolean = {
    user match {
      case UnauthenticatedUser => visibleToValue == ANONYMOUS
      case _ => visibleToValue == ANONYMOUS || visibleToValue == KNOWN
    }
  }
}
