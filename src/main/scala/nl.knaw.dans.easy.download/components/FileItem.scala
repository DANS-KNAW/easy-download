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
import java.text.SimpleDateFormat

import nl.knaw.dans.easy.download.NotAccessibleException
import nl.knaw.dans.easy.download.components.RightsFor._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json4s._
import org.json4s.ext.{ EnumNameSerializer, JodaTimeSerializers }
import org.json4s.native.JsonMethods.parse

import scala.util.{ Failure, Success, Try }

/**
 * @param itemId uuid of the bag + path of payload item from files.xml
 * @param owner  depositor of the bag
 */
case class FileItem(itemId: String,
                    owner: String,
                    dateAvailable: DateTime,
                    accessibleTo: RightsFor.Value,
                    visibleTo: RightsFor.Value
                   ) {
  def availableFor(user: Option[User]): Try[Unit] = {
    if (isOwnedBy(user)) Success(())
    else if (!isVisibleTo(user)) Failure(new FileNotFoundException(itemId))
    else if (dateAvailable.isAfterNow) embagroFailure
    else if (accessibleTo == ANONYMOUS) Success(())
    else if (accessibleTo == KNOWN)
           if (user.isDefined) Success(())
           else Failure(NotAccessibleException(s"Please login to download: $itemId"))
    else Failure(NotAccessibleException(s"Download not allowed of: $itemId")) // might require group/permission
  }

  def isOpenAccess: Boolean = {
    accessibleTo == ANONYMOUS
  }

  private def isOwnedBy(user: Option[User]): Boolean = {
    user.exists(_.id == owner)
  }

  private def isVisibleTo(user: Option[User]): Boolean = {
    visibleTo == ANONYMOUS || (visibleTo == KNOWN && user.isDefined)
  }

  private def embagroFailure = {
    val date = DateTimeFormat.forPattern("yyyy-MM-dd").print(dateAvailable)
    Failure(NotAccessibleException(s"Download becomes available on $date [$itemId]"))
  }
}

object FileItem {
  private implicit val jsonFormats: Formats = new DefaultFormats {
    override protected def dateFormatter: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
  } + new EnumNameSerializer(RightsFor) ++ JodaTimeSerializers.all

  def fromJson(input: String): Try[FileItem] = {
    Try(parse(input).extract[FileItem]).recoverWith { case t =>
      Failure(new Exception(s"parse error [${ t.getClass }: ${ t.getMessage }] for: $input", t))
    }
  }
}
