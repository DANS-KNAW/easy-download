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

import java.net.{ URI, URL }
import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.easy.download.{ HttpStatusException, OutputStreamProvider }
import nl.knaw.dans.lib.encode.PathEncoding
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.Http

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

trait BagStoreComponent extends DebugEnhancedLogging {
  this: HttpWorkerComponent =>

  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI
    val connTimeout: Int
    val readTimeout: Int

    def copyStream(bagId: UUID, path: Path): OutputStreamProvider => Try[Unit] = { outputStreamProducer =>
      for {
        f <- Try(path.escapePath)
        uri <- Try(baseUri.resolve(s"bags/$bagId/$f"))
        _ <- http.copyHttpStream(uri)(outputStreamProducer)
      } yield ()
    }

    def loadDDM(bagId: UUID): Try[Elem] = {
      logger.info(s"[$bagId] retrieving ddm.xml")
      toURL(bagId, "metadata/dataset.xml").flatMap(loadXml)
    }

    private def loadXml(url: URL): Try[Elem] = {
      for {
        response <- Try { Http(url.toString).timeout(connTimeout, readTimeout).asString }
        _ <- if (response.isSuccess) Success(())
             else Failure(HttpStatusException(url.toString, response))
      } yield XML.loadString(response.body)
    }

    private def toURL(bagId: UUID, path: String): Try[URL] = Try {
      val escapedPath = Paths.get(path).escapePath
      baseUri.resolve(s"bags/$bagId/$escapedPath").toURL
    }
  }
}
