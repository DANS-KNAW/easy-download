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

import java.net.URL
import java.nio.file.Paths

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.Http

import scala.collection.mutable
import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val connTimeout: Int = configuration.properties.getInt("bag-store.connection-timeout-ms")
  val readTimeout: Int = configuration.properties.getInt("bag-store.read-timeout-ms")
  val NARCIS_CLASSIFICATION = new URL("https://www.narcis.nl/content/classification/narcis-classification.rdf")

  getDisciplines(NARCIS_CLASSIFICATION)
    .doIfSuccess(discipl => {
      val app = EasyDownloadApp(configuration, discipl.toMap)
      runSubcommand(app)
        .doIfSuccess(msg => println(s"OK: $msg"))
        .doIfFailure { case e => logger.error(e.getMessage, e) }
        .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }
    })
    .doIfFailure { case e => logger.error(e.getMessage, e) }

  private def runSubcommand(app: EasyDownloadApp): Try[FeedBackMessage] = {
    commandLine.subcommand
      .collect {
        //      case subcommand1 @ subcommand.subcommand1 => // handle subcommand1
        //      case None => // handle command line without subcommands
        case commandLine.runService => runAsService(app)
      }
      .getOrElse(Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }")))
  }

  private def runAsService(app: EasyDownloadApp): Try[FeedBackMessage] = Try {
    val service = new EasyDownloadService(configuration.properties.getInt("daemon.http.port"), app)
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        service.stop()
        service.destroy()
      }
    })

    service.start()
    Thread.currentThread.join()
    "Service terminated normally."
  }

  def getDisciplines(url: URL): Try[mutable.Map[String, String]] = {
    for {
      rdf <- loadXml(url)
    } yield getDisciplinesMap(rdf)
  }

  private def getDisciplinesMap(rdf: Elem): mutable.Map[String, String] = {
    var disciplines = mutable.Map[String, String]()
    val descriptions = rdf \ "Description"
    descriptions.foreach(description =>
      (description \ "prefLabel").foreach(label =>
        label.attribute("xml:lang").foreach(attribute =>
          if (attribute.text == "en") disciplines += (getClassificationId((description \ "@rdf:about").text) -> label.text))))
    disciplines
  }

  private def getClassificationId(s: String): String = {
    s.slice(s.lastIndexOf("/"), s.length)
  }

  private def loadXml(url: URL): Try[Elem] = {
    for {
      response <- Try { Http(url.toString).timeout(connTimeout, readTimeout).asString }
      _ <- if (response.isSuccess) Success(())
           else Failure(HttpStatusException(url.toString, response))
    } yield XML.loadString(response.body)
  }
}
