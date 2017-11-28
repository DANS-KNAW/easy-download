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

import java.nio.file.Path

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node }

case class FileRights(accessibleTo: String, visibleTo: String)
class FileItems(ddm: => Elem, filesXml: Elem) extends DebugEnhancedLogging {

  private val fileItems = filesXml \ "file"

  private val none = "NONE"
  private val known = "KNOWN"
  private val anonymous = "ANONYMOUS"
  private val restrictedGroup = "RESTRICTED_GROUP"
  private val restrictedRequest = "RESTRICTED_REQUEST"

  // see ddm.xsd EasyAccessCategoryType
  private lazy val datasetAccessibleTo = (ddm \ "profile" \ "accessRights").text match {
    // @formatter:off
    case "OPEN_ACCESS"                      => Some(anonymous)
    case "OPEN_ACCESS_FOR_REGISTERED_USERS" => Some(known)
    case "GROUP_ACCESS"                     => Some(restrictedGroup)
    case "REQUEST_PERMISSION"               => Some(restrictedRequest)
    case "NO_ACCESS"                        => Some(none)
    case _                                  => None
    // @formatter:off
  }
  private val allowedValues = Seq(anonymous, known, restrictedGroup, restrictedRequest, none)

  def rightsOf(path: Path): Try[Option[FileRights]] = {
    fileItems
      .find(_
        .attribute("filepath")
        .map(_.text)
        .contains(path.toString)
      ).map(rightsAsJson) match {
      case Some(Success(v)) => Success(Some(v))
      case Some(Failure(t)) => Failure(t)
      case None => Success(None)
    }
  }

  private def rightsAsJson(item: Node): Try[FileRights] = {
    lazy val dctermsAccessRight = getValue(item, "accessRights", datasetAccessibleTo).map(_.toUpperCase)
    val accessibleTo = getValue(item, "accessibleToRights", dctermsAccessRight)
    val visibleTo = getValue(item, "visibleToRights", Some(anonymous))
    if(accessibleTo.isEmpty || visibleTo.isEmpty)
      Failure(new Exception("<visibleToRights> not found in files.xml nor <ddm:accessRights> in dataset.xml"))
    else if (!allowedValues.contains(accessibleTo.getOrElse("?"))) // dcterms content not validated by XSD
      Failure(new Exception(s"<accessibleToRights> not found in files.xml and <dcterms:accessRights> [${accessibleTo.getOrElse("?")}] should be one of $allowedValues"))
    else Success(FileRights(accessibleTo.getOrElse("?"), visibleTo.getOrElse("?")))
  }

  private def getValue(item: Node, tag: String, default: => Option[String]): Option[String] = {
    (item \ tag).headOption.map(_.text) match {
      case Some(s) => Some(s)
      case None => default
    }
  }
}
