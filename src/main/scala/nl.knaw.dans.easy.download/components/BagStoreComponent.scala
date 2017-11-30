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


import java.io.{ ByteArrayOutputStream, InputStream, OutputStream }
import java.net.{ URI, URLEncoder }
import java.nio.file.Path
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

import nl.knaw.dans.easy.download.HttpStatusException
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils

import scala.util.{ Failure, Success, Try }
import scalaj.http.{ Http, HttpResponse }

trait BagStoreComponent extends DebugEnhancedLogging {
  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

    private def httpException(message: String, code: Int) = {
      val headers = Map[String, String]("Status" -> s"$code")
      Failure(HttpStatusException(message, HttpResponse("", code, headers)))
    }

    private def copyStreamHttp(uri: String): (() => OutputStream) => Try[Unit] = { outputStreamProducer =>
      // It seems that as soon as we write something (or even 'touch') the outputstream,
      // Scalatra won't allow any change, like setting the status code to what we get from our respons

      // Note maybe first ask for it, and only consume if it's OK?
      // but the HEAD request is not supported by our bag store!
      // so taste a litle byte first before eating the whole pie
      val trial = Http(uri).method("GET").execute( is => is.read())
      if (!trial.isSuccess) {
        logger.error(s"Failed to start download $uri with: ${ trial.code }")
        httpException(s"Failed to start download $uri - ${ trial.statusLine }", trial.code)
      } else {
        val response = Http(uri).method("GET").execute(IOUtils.copyLarge(_, outputStreamProducer()))
        if (response.isSuccess) {
          logger.info(s"Downloaded $uri")
          Success(())
        }
        else {
          logger.error(s"Failed downloading $uri with: ${ response.code }")
          // Note we get 500 with a log warning like:
          // WARN Error Processing URI: /e04c0475-ca0c-45ec-8fee-81db75e0d38f/bag-infoxxx.txt - (java.lang.IllegalStateException) STREAM
          httpException(s"Failed to download $uri - ${ response.statusLine }", response.code)
        }

      }
    }

    def copyStream(bagId: UUID, path: Path): (() => OutputStream) => Try[Unit] = { outputStreamProducer =>
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        uri <- Try(baseUri.resolve(s"stores/pdbs/bags/$bagId/$f")) // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
        _ <- copyStreamHttp(uri.toString)(outputStreamProducer)
      } yield ()
    }

  }
}
