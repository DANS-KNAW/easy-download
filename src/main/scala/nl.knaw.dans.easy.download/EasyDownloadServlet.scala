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

import java.io.{ InputStream, OutputStream }
import java.net.{ HttpURLConnection, URL }
import java.util.{ Date, UUID }

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scalaj.http.Http
//import nl.knaw.dans.easy.bagstore.server
import org.apache.http.HttpStatus._
import org.scalatra._

import scala.util.Try
import scalaj.http.HttpRequest
import scalaj.http.HttpResponse

import java.io.File
import java.net.URL
import sys.process._

import scala.language.postfixOps

class EasyDownloadServlet(app: EasyDownloadApp) extends ScalatraServlet with DebugEnhancedLogging {
  logger.info("File index Servlet running...")

  def fileDownloader(url: String, filename: String) = {
    new URL(url) #> new File(filename) !!
  }

  def downloadFile(token: String, fileToDownload: String) {
    try {
      val src = scala.io.Source.fromURL("http://ir.dcs.gla.ac.uk/resources/linguistic_utils/stop_words" + fileToDownload)
      val out = new java.io.FileWriter("/testingUpload1.txt")
      out.write(src.mkString)
      out.close
    } catch {
      case e: java.io.IOException => "error occured"
    }
  }

//  get("/") {
//    contentType = "text/plain"
//    Ok("EASY Download Service running...")
//  }



//  val result10 = Http("http://test.dans.knaw.nl/")
//    .method("GET")
//    .header("Accept-Ranges", "bytes")
//    .header("Range", "100-200")
//    .timeout(connTimeoutMs = 2000, readTimeoutMs = 5000)
//    .asString


    get("/") {
//      contentType = "text/plain"
//      import scala.io.Source
//      val html = Source.fromURL("http://google.com")
//      val s = html.mkString
//      Ok("Google..." + s)

//      val url = "http://test.dans.knaw.nl:20110/stores/pdbs/bags"
//      val request: HttpRequest = Http(url)
//      Http(url).execute(in => {
//        org.scalatra.util.io.copy(in, response.getOutputStream)
//      })


      // get the contents of the twitter page (ScalaJ)
//      val response: HttpResponse[String] = Http("http://test.dans.knaw.nl:20110/stores/pdbs/bags")


//    val response: HttpResponse[String] = Http("http://google.com")
//      .timeout(connTimeoutMs = 2000, readTimeoutMs = 5000)
//      .asString
//        val html = response.body
//        Ok("EASY..." + html)
      //      println(html)


//      fileDownloader("http://ir.dcs.gla.ac.uk/resources/linguistic_utils/stop_words", "/Users/alexanderatamas/stop-words-en.txt")

//      val url = new URL("http://download.thinkbroadband.com/1GB.zip")

      val url = new URL("http://ipv4.download.thinkbroadband.com/5MB.zip")
      val conn = url.openConnection
      conn.setRequestProperty("Accept","text/json")
      conn.setIfModifiedSince(new Date().getTime - 1000*60*30)
      url #> new File("/Users/alexanderatamas/1gb.zip") !!

    }

  get("/test1/") {

//    val url = "http://test.dans.knaw.nl:20110/stores/pdbs/bags"
//    val url = "http://192.168.33.32:20110/stores/pdbs/bags"
//       val url = "http://localhost:20110/stores/pdbs/bags"
//
//    val request: HttpRequest = Http(url)
//    Http(url).execute(in => {
//      org.scalatra.util.io.copy(in, response.getOutputStream)
//      //copyStream(in, response.getOutputStream)
//
//    })


    fileDownloader("http://ir.dcs.gla.ac.uk/resources/linguistic_utils/stop_words", "stop-words-en.txt")


  }


  private def copyStream(input: InputStream, output: OutputStream) {
    val buffer = Array.ofDim[Byte](10*1024)
    var bytesRead: Int = 0
    while (bytesRead != -1) {
      bytesRead = input.read(buffer)
      if (bytesRead > 0) output.write(buffer, 0, bytesRead)
    }
  }


  get("/test2/") {

//    val response: HttpResponse[String] = Http("http://test.dans.knaw.nl:20110/stores/pdbs/bags")
    val response: HttpResponse[String] = Http("http://localhost:20110/stores/pdbs/bags")
      .timeout(connTimeoutMs = 2000, readTimeoutMs = 5000)
      .asString
    val html = response.body
    Ok(html)
  }


  get("/test3/") {

    val response: HttpResponse[String] = Http("http://test.dans.knaw.nl:20160/")
      .timeout(connTimeoutMs = 2000, readTimeoutMs = 5000)
      .asString
    val html = response.body
    Ok("EASY..." + html)
  }

  get("/test4/") {

    val response: HttpResponse[String] = Http("http://ukr.net")
      .timeout(connTimeoutMs = 2000, readTimeoutMs = 5000)
      .asString
    val html = response.body
    Ok("EASY..." + html)
  }

  get("/test5/") {

    val url = "https://dans.knaw.nl/en/about/organisation-and-policy/legal-information/Toelichting_licentie_overeenkomst_UK_5.3.pdf"
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(5000)
    connection.setRequestMethod("GET")
    connection.setInstanceFollowRedirects(true)
    val inputStream = connection.getInputStream

    inputStream

  }

  get("/test6/") {

//    val url = "https://dans.knaw.nl/en/about/organisation-and-policy/legal-information/Toelichting_licentie_overeenkomst_UK_5.3.pdf"
//    val url = "http://ipv4.download.thinkbroadband.com/5MB.zip"
    //val url = "http://localhost:20110/bags/40594b6d-8378-4260-b96b-13b57beadf7c/metadata/bag-info.txt"
    val url = "http://localhost:20110/stores/pdbs/bags/40594b6d-8378-4260-b96b-13b57beadf7c"
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(5000)
    connection.setRequestMethod("GET")
    connection.setInstanceFollowRedirects(true)
    val inputStream = connection.getInputStream

    //val content = io.Source.fromInputStream(inputStream).mkString
    //if (inputStream != null) inputStream.close
    //content

    org.scalatra.util.io.copy(inputStream, response.getOutputStream, 64*1024)



    //copyStream(inputStream, response.getOutputStream)

  }

  get("/test7/") {

//    val url = "https://dans.knaw.nl/en/about/organisation-and-policy/legal-information/Toelichting_licentie_overeenkomst_UK_5.3.pdf"
    val url = "http://localhost:20110/bags/40594b6d-8378-4260-b96b-13b57beadf7c/bag-info.txt"
    //val url = "http://ipv4.download.thinkbroadband.com/5MB.zip"
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(5000)
    connection.setRequestMethod("GET")
    connection.setInstanceFollowRedirects(true)
    val inputStream = connection.getInputStream

    //val content = io.Source.fromInputStream(inputStream).mkString
    //if (inputStream != null) inputStream.close
    //content



    copyStream(inputStream, response.getOutputStream)


  }



//  get("/ark:/73189//:uuid/*") {
//    val uuidStr = params("uuid")
//    val pathSeq =  multiParams("splat")
//
//    val storeId = "pdbs" // for testing
//
////    val url = s"http://localhost:20110/stores/$storeId/bags/$uuidStr/$pathSeq"
//
//
////    val url = "http://localhost:20110/bags/40594b6d-8378-4260-b96b-13b57beadf7c/bag-info.txt"
//
//    val url = "http://localhost:20110/stores/pdbs/bags"
//
//    val request: HttpRequest = Http(url)
//
//
//
//    Http(url).execute(in => {
//      org.scalatra.util.io.copy(in, response.getOutputStream)
//    })
//
//  }


//  get("/") {
//
//    contentType = "text/plain"
//    val (includeActive, includeInactive) = includedStates(params.get("state"))
//    respond(app.getAllBags())

//    contentType = "text/plain"
//    val (includeActive, includeInactive) = includedStates(params.get("state"))
//    bagStores.enumBags(includeActive, includeInactive)
//      .map(bagIds => Ok(bagIds.mkString("\n")))
//      .getOrRecover(e => {
//        logger.error("Unexpected type of failure", e)
//        InternalServerError(s"[${ new DateTime() }] Unexpected type of failure. Please consult the logs")
//      })



//  }

  private def respond(result: Try[String]): ActionResult = {
    val msgPrefix = "Log files should show which actions succeeded. Finally failed with: "
    result.map(Ok(_))
      .doIfFailure { case e => logger.error(e.getMessage, e) }
      .getOrRecover {
        case HttpStatusException(message, r: HttpResponse[String]) if r.code == SC_NOT_FOUND => NotFound(message)
        case HttpStatusException(message, r: HttpResponse[String]) if r.code == SC_SERVICE_UNAVAILABLE => ServiceUnavailable(message)
        case HttpStatusException(message, r: HttpResponse[String]) if r.code == SC_REQUEST_TIMEOUT => RequestTimeout(message)
        case MixedResultsException(_, HttpStatusException(message, r: HttpResponse[String])) if r.code == SC_NOT_FOUND => NotFound(msgPrefix + message)
        case MixedResultsException(_, HttpStatusException(message, r: HttpResponse[String])) if r.code == SC_SERVICE_UNAVAILABLE => ServiceUnavailable(msgPrefix + message)
        case MixedResultsException(_, HttpStatusException(message, r: HttpResponse[String])) if r.code == SC_REQUEST_TIMEOUT => RequestTimeout(msgPrefix + message)
        case e => InternalServerError(e.getMessage)
      }
  }

  private def getUUID = {
    Try { UUID.fromString(params("uuid")) }
  }
}