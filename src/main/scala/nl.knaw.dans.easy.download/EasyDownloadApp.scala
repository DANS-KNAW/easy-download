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

import java.io.OutputStream
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.download.components.User
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest

import scala.util.{ Failure, Success, Try }
import scalaj.http.HttpResponse

trait EasyDownloadApp extends AutoCloseable
  with DebugEnhancedLogging with ApplicationWiring {

  def authenticate(authRequest: BasicAuthRequest): Try[Option[User]] = authentication.authenticate(authRequest)

  def copyStream(bagId: UUID, path: Path, outputStreamProducer: () => OutputStream): Try[Unit] = {
    for {
      authInfo <- authInfo.getOuthInfo(bagId, path)
      _ <- authInfo.visibleTo(None)
      _ <- authInfo.accessibleTo(None)
      _ <- authInfo.noEmbargo
      _ <- bagStore.copyStream(bagId, path)(outputStreamProducer).recoverWith {
        case HttpStatusException(message, HttpResponse(_, NOT_FOUND_404, _)) =>
          Failure(new Exception(s"invalid bag, file downloadable but not found: $path"))
      }
    } yield ()
  }

  def init(): Try[Unit] = {
    // Do any initialization of the application here. Typical examples are opening
    // databases or connecting to other services.
    Success(())
  }

  override def close(): Unit = {

  }
}

object EasyDownloadApp {

  def apply(conf: Configuration): EasyDownloadApp = new EasyDownloadApp {
    override lazy val configuration: Configuration = conf
  }
}