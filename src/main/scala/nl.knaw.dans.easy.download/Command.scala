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

import java.io.{ File, FilenameFilter }
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
  val easyBasicDigitalObjects = new File("https://github.com/DANS-KNAW/easy-dtap/blob/master/provisioning/roles/easy-fcrepo/files/basic-digital-objects/")

  getDisciplines(easyBasicDigitalObjects)
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

  private def getDisciplines(digitalObjects: File): Try[DisciplinesMap] = Try {
    implicit var disciplines: DisciplinesMap = mutable.Map[String, (String, String)]()

    getEasyDisciplineFiles(digitalObjects).foreach(file =>
      addDiscipline(file)
        .doIfFailure({ case e => throw new Exception(s"Downloading easy-discipline xml-file ${ file.getName } failed", e) })
    )
    disciplines
  }

  private def getEasyDisciplineFiles(digitalObjects: File): Array[File] = {
    digitalObjects.listFiles(new FilenameFilter {
      def accept(dir: File, name: String): Boolean = name.startsWith("easy-discipline:") && !name.endsWith("root.xml")
    })
  }

  private def addDiscipline(file: File): Try[Unit] = {
    for {
      foxml <- loadXml(new URL(file.getAbsolutePath))
    } yield addToDisciplinesMap(foxml)
  }

  private def addToDisciplinesMap(xml: Elem)(disciplines: DisciplinesMap): Unit = {
    val oicode = (xml \\ "OICode").headOption
    val identifier = (xml \\ "identifier").headOption
    val title = (xml \\ "title").headOption
    oicode.foreach(o =>
      identifier.foreach(i =>
        title.foreach(t =>
          disciplines += (o.text -> (i.text, t.text)))))
  }

  private def loadXml(url: URL): Try[Elem] = {
    for {
      response <- Try { Http(url.toString).timeout(connTimeout, readTimeout).asString }
      _ <- if (response.isSuccess) Success(())
           else Failure(HttpStatusException(url.toString, response))
    } yield XML.loadString(response.body)
  }
}
