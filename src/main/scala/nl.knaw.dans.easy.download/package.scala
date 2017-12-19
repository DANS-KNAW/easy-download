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
package nl.knaw.dans.easy

import java.io.OutputStream
import java.nio.file.Path

import com.google.common.net.UrlEscapers
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }
import scalaj.http.HttpResponse

package object download extends DebugEnhancedLogging {
  private val pathEscaper = UrlEscapers.urlPathSegmentEscaper()

  type FeedBackMessage = String
  type OutputStreamProvider = () => OutputStream

  case class HttpStatusException(msg: String, response: HttpResponse[String])
    extends Exception(s"$msg - ${ response.statusLine }, details: ${ response.body }")

  case class NotAccessibleException(message: String)
    extends Exception(message)

  case class InvalidUserPasswordException(userName: String, cause: Throwable)
    extends Exception(s"invalid credentials for $userName") {
    logger.info(s"invalid credentials for $userName: ${ cause.getMessage }")
  }

  case class AuthenticationNotAvailableException(cause: Throwable)
    extends Exception(cause.getMessage, cause) {
    logger.info(cause.getMessage)
  }
  case class AuthenticationTypeNotSupportedException(cause: Throwable)
    extends Exception(cause.getMessage, cause) {
    logger.info(cause.getMessage)
  }

  def escapePath(path: Path): String = {
    path.asScala.map(_.toString).map(pathEscaper.escape).mkString("/")
  }

  implicit class TryExtensions2[T](val t: Try[T]) extends AnyVal {
    // TODO candidate for dans-scala-lib
    def unsafeGetOrThrow: T = {
      t match {
        case Success(value) => value
        case Failure(throwable) => throw throwable
      }
    }
  }
}

