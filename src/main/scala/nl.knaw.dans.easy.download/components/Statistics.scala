/*
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

import java.io.File
import java.util.UUID

import com.typesafe.scalalogging.Logger
import javax.servlet.http.HttpServletRequest
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.slf4j.LoggerFactory

import scala.util.Try
import scala.xml.Elem

case class Statistics(request: HttpServletRequest, bagId: UUID, fileItem: FileItem, userInfo: Option[User], ddm: Elem) extends DebugEnhancedLogging {

  private val statisticsLogger = Logger(LoggerFactory.getLogger("easy-statistics"))
  private val disciplines = EasyDisciplines.disciplines

  def logDownload: Try[Unit] = Try {
    val user = userInfo.getOrElse(User("Anonymous", Seq.empty))
    statisticsLogger.info(getLogEventString(user))
  }

  def getLogEventString(user: User): String = {
    val (subDiscipline, subDisciplineLabel) = getSubDiscipline
    s"""- DOWNLOAD_FILE_REQUEST ; ${ getUserId(user) } ; roles: $getUserRoles ; groups: ${ getUserGroups(user) } ; $getIpAddress ;
       | dataset(DATASET_ID: "$getDatasetId") ; file(FILE_NAME(0): "$getFileName") ;
       | discipline(SUB_DISCIPLINE_ID: "${ subDiscipline }" ; TOP_DISCIPLINE_LABEL: "$getTopDisciplineLabel" ;
       | SUB_DISCIPLINE_LABEL: "${ subDisciplineLabel }" ; TOP_DISCIPLINE_ID: "$getTopDiscipline")
       |""".stripMargin.replace("\n", "")
  }

  private def getUserId(user: User): String = {
    user.id
  }

  def getUserRoles: String = {
    "(USER)"
  }

  private def getUserGroups(user: User): String = {
    user.groups.mkString("(", ",", ")")
  }

  private def getIpAddress: String = {
    if (request != null) {
      val forwardedFor = request.getHeader("X-FORWARDED-FOR")
      if (forwardedFor != null && forwardedFor.trim.nonEmpty) forwardedFor
      else if (request.getRemoteAddr != null) request.getRemoteAddr
      else ""
    }
    else ""
  }

  private def getDatasetId = {
    val identifier = (ddm \ "dcmiMetadata" \ "identifier").find(_.text.startsWith("easy-dataset"))
    identifier.map(_.text).getOrElse(datasetIdentifierNotFound)
  }

  private def datasetIdentifierNotFound: String = {
    logger.warn(s"Easy dataset-id <ddm:identifier> not found in $bagId/dataset.xml")
    "-"
  }

  private def getFileName: String = {
    new File(fileItem.itemId).getName
  }

  private def getSubDiscipline: (String, String) = {
    (ddm \ "profile" \ "audience")
      .headOption
      .map(audience => disciplines.getOrElse(audience.text, audienceNotFound(audience.text)))
      .getOrElse(("", ""))
  }

  private def audienceNotFound(audience: String): (String, String) = {
    logger.error(s"Audience $audience not found in the Map of Easy disciplines")
    ("", "")
  }

  private def getTopDiscipline: String = {
    "easy-discipline:root"
  }

  private def getTopDisciplineLabel: String = {
    ""
  }
}
