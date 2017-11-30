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

import java.net.{ URI, URLEncoder }
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.download.{ HttpStatusException, OutputStreamProvider }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.http.HttpStatus._

import scala.util.{ Failure, Success, Try }
import scalaj.http.{ Http, HttpResponse }

trait BagStoreComponent extends DebugEnhancedLogging {

  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

    private def copyStreamHttp(uri: String): OutputStreamProvider => Try[Unit] = { outputStreamProducer =>
      val response = Http(uri).method("GET").exec {
        case (OK_200, _, is) => IOUtils.copyLarge(is, outputStreamProducer())
        case _ => // do nothing
      }
      if (response.isSuccess) Success(())
      else Failure(HttpStatusException(s"Could not download $uri", HttpResponse(response.statusLine, response.code, response.headers)))
    }

    def copyStream(bagId: UUID, path: Path): OutputStreamProvider => Try[Unit] = { outputStreamProducer =>
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        uri <- Try(baseUri.resolve(s"bags/$bagId/$f"))
        _ <- copyStreamHttp(uri.toString)(outputStreamProducer)
      } yield ()
    }
  }
}
