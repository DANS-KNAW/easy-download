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

import java.io.File
import java.net.URL
import java.util.UUID

import javax.servlet.http.HttpServletRequest
import nl.knaw.dans.easy.download.HttpStatusException
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.slf4j.MDC
import scalaj.http.Http

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

case class Statistics(request: HttpServletRequest, bagId: UUID, fileItem: FileItem, userInfo: Option[User], ddm: Elem, disciplines: Map[String, String]) extends DebugEnhancedLogging {

  def logDownload: Try[Unit] = Try {
    val user = userInfo.getOrElse(User("Anonymous", Seq.empty))
    logDownloadEvent(getLogEventString(user))
  }

  private def getLogEventString(user: User): String = {
    s"- DOWNLOAD_FILE_REQUEST ; ${getUserId(user)} ; $getUserRoles ; ${getUserGroups(user)} ; $getIpAddress ; " +
    s"dataset(DATASET_ID: \"$getDatasetId\") ; file(FILE_NAME(0): \"$getFileName\"" +
    s"discipline(SUB_DISCIPLINE_ID: \"$getSubDiscipline\" ; TOP_DISCIPLINE_LABEL: \"$getTopDisciplineLabel\" ; " +
    s"SUB_DISCIPLINE_LABEL: \"${getSubDisciplineLabel(getSubDiscipline)} ; TOP_DISCIPLINE_ID: \"$getTopDiscipline\")"
  }

  private def logDownloadEvent(logEventString: String): Unit = {
    // This causes logger to write into easy-statistics log-file
    MDC.put("logFile", "statistics")
    logger.info(logEventString)
    MDC.remove("logFile")
  }

  private def getUserId(user: User): String = {
    user.id
  }

  def getUserRoles: String = {
    "(USER)"
  }

  private def getUserGroups(user: User): String = {
    if (user.groups.nonEmpty)
      user.groups.mkString("(", ",", ")")
    else
      "()"
  }

  private def getIpAddress: String = {
    val forwardedFor = request.getHeader("X-FORWARDED-FOR")
    if (forwardedFor != null && forwardedFor.trim.nonEmpty) forwardedFor
    else if (request.getRemoteAddr != null) request.getRemoteAddr
    else ""
  }

  private def getDatasetId = {
    val identifier = (ddm \ "identifier").find(_.text.startsWith("easy-dataset"))
    if (identifier.isDefined) identifier.get.text
    else datasetIdentifierNotFound
  }

  private def datasetIdentifierNotFound: String = {
    logger.error(s"Easy dataset-id <ddm:identifier> not found in $bagId/dataset.xml")
    "-"
  }

  private def getFileName: String = {
    new File(fileItem.itemId).getName
  }

  private def getSubDiscipline: String = {
    val subDiscipline = (ddm \ "audience").headOption
    if (subDiscipline.isDefined) subDiscipline.get.text
    else ""
  }

  private def getSubDisciplineLabel(subDiscipline: String): String = {
    disciplines.
  }

  private def getTopDiscipline: String = {
    "easy-discipline:root"
  }

  private def getTopDisciplineLabel: String = {
    ""
  }
}