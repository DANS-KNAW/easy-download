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

import java.net.URI
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.download.escapePath
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.native.JsonMethods._
import org.json4s.{ DefaultFormats, _ }

import scala.util.{ Failure, Try }

// TODO 'Auth' stands for 'Authorization' I suppose? Maybe call it that.
// same for the `val authInfo: AuthInfo`. I would say call it `Authorization`/`AuthorizationComponent`.
// that is more consistent with what you do in `Authentication`.
// I am fine, however, with keeping `FileItemAuthInfo`, since the focus there is on the FileItem and
// the Info that is inside it.
trait AuthInfoComponent extends DebugEnhancedLogging {
  this: HttpWorkerComponent =>

  val authInfo: AuthInfo
  // TODO put this private implicit val in the inner-trait
  private implicit val jsonFormats: Formats = DefaultFormats

  trait AuthInfo {
    val baseUri: URI

    // getFileItemAuthInfo
    def getFileItem(bagId: UUID, path: Path): Try[FileItemAuthInfo] = {
      for {
        f <- Try(escapePath(path))
        uri = baseUri.resolve(s"$bagId/$f")
        jsonString <- http.getHttpAsString(uri)
        authInfo <- Try(parse(jsonString).extract[FileItemAuthInfo]).recoverWith {
          // TODO would a custom exception be useful here? especially to match on in later uses?
          case t => Failure(new Exception(s"parse error [${ t.getMessage }] for: $jsonString", t))
        }
      } yield authInfo
    }
  }
}
