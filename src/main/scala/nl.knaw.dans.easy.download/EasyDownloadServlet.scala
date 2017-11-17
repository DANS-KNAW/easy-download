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

import java.net.{ HttpURLConnection, URL }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra._

import scala.util.{ Failure, Success, Try }

class EasyDownloadServlet(app: EasyDownloadApp) extends ScalatraServlet with DebugEnhancedLogging {
  logger.info("File Download Servlet running...")

  private def copyStream(url: String): Unit = {
    val connection = Try((new URL(url)).openConnection.asInstanceOf[HttpURLConnection])

    connection match {
      case Success(con) =>
        con.setConnectTimeout(5000)
        con.setReadTimeout(5000)
        con.setRequestMethod("GET")
        con.setInstanceFollowRedirects(true)
        val inputStream = con.getInputStream
        val outputStream = response.getOutputStream
        val buffSize = 64 * 1024
        org.scalatra.util.io.copy(inputStream, outputStream, buffSize)
        outputStream.flush()
        outputStream.close()
        if(con != null) con.disconnect()
      case Failure(e) =>
        e match {
          case e: ClassCastException => Ok("Cannot cast the url to string")
          case e => logger.error("Error while downloading", e)
        }
    }
  }

  get("/") {
    val url = "http://localhost:20110/stores/pdbs/bags"
    copyStream(url)
  }

  get("/:uuid/*") {
    val uuidStr = params("uuid")
    multiParams("splat") match {
      case Seq(path) =>
        val url = s"http://localhost:20110/stores/pdbs/bags/$uuidStr/$path"
        copyStream(url)
    }
  }

  get("/:uuid/?") {
    val uuidStr = params("uuid")
    val url = s"http://localhost:20110/stores/pdbs/bags/$uuidStr"
    copyStream(url)
  }

}