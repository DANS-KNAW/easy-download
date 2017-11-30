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
import resource._

trait BagStoreComponent extends DebugEnhancedLogging {
  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

    private def httpException(message: String, code: Int) = {
      val headers = Map[String, String]("Status" -> s"$code")
      Failure(HttpStatusException(message, HttpResponse("", code, headers)))
    }

    import java.net.{ HttpURLConnection, URL }
    private def copyStreamURL(url: String): (() => OutputStream) => Try[Unit] = { outputStreamProducer =>
      val connection = Try((new URL(url)).openConnection.asInstanceOf[HttpURLConnection])
      connection match {
        case Success(con) =>
          con.setConnectTimeout(5000)
          con.setReadTimeout(5000)
          con.setRequestMethod("GET")
          val inputStream = con.getInputStream
          val code = con.getResponseCode
          if (code == 200) {
            //val outputStream = response.getOutputStream
            IOUtils.copyLarge(inputStream, outputStreamProducer())
            con.disconnect()
            Success(())
          }
          else {
            httpException("Bag this was not a success!", 555)//code)
          }
        case Failure(e)
            => httpException("Bag this was not a success2!", 666)
      }
    }

    def test(uri: String): (() => OutputStream) => Try[Unit] = { outputStreamProducer =>
      //      Try(Http(uri.toString).method("GET").execute((is: InputStream) => IOUtils.copy(is, outputStream)))

      // It seems that as soon as we write something (or even 'touch') the outputstream,
      // scalatra won't allow any change, like setting the status code to what we get from our respons

      // Note maybe first ask for it, and only consume if it's OK?
      // but the HEAD request is not supported by our bag store!
      // so taste a litle byte first before eating the whole pie
      val trial = Http(uri).method("GET").execute( is => is.read())
      if (!trial.isSuccess) {
        httpException(s"Bag this was not a success, - ${ trial.statusLine }", trial.code)
      } else {

        val response = Http(uri).method("GET").execute(IOUtils.copyLarge(_, outputStreamProducer()))
        if (response.isSuccess) {
          //Success(outputStream.write(response.body))
          logger.info("Ok, it works")
          Success(())
        }
        else {
          logger.info(s"Failed with: ${ response.code }")
          //Failure(HttpStatusException("Bag this was not a success!", response.copy(body = s"this is a ${ response.code }")))
          httpException("Bag this was not a success!", response.code)
        }

      }
    }

//    def test2(uri: String): OutputStream => Unit = { outputStream =>
//      val response = Http(uri).method("GET").asBytes
//      if (response.isSuccess)
//        outputStream.write(response.body)
//      else
//        Failure(HttpStatusException("Bag this was not a success!", response.copy(body = new String(response.body))))
//    }

    def copyStream(bagId: UUID, path: Path): (() => OutputStream) => Try[Unit] = { outputStreamProducer =>
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        uri <- Try(baseUri.resolve(s"stores/pdbs/bags/$bagId/$f")) // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
//        _ <- Try(Http(uri.toString).method("GET").execute((is: InputStream) => IOUtils.copy(is, outputStream)))
        _ <- {
          // below the Richard fix
          // otherwise I get 500 with a log warning:
          // WARN Error Processing URI: /e04c0475-ca0c-45ec-8fee-81db75e0d38f/bag-infoxxx.txt - (java.lang.IllegalStateException) STREAM
//          managed(new ByteArrayOutputStream())
//            .map(os => test(uri.toString)(os).map(_ => os.writeTo(outputStreamProducer())))
//            .tried.flatten

          test(uri.toString)(outputStreamProducer)
//          test(uri.toString)(outputStream)
        }
      } yield ()
    }
  }
}
