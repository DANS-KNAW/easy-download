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

import nl.knaw.dans.easy.download.{ OutputStreamProvider, escapePath }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

trait BagStoreComponent extends DebugEnhancedLogging {
  this: HttpWorkerComponent =>

  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

    // name isn't really fitting for the context this function is in; shouldn't it be something like `downloadFile`
    // question: can this `path` also be a directory/folder? e.g. can you download a whole folder at once?
    // question: can I use `path` to get places I'm not supposed to be at? like `bag/../../../other-bag-store/`
    def copyStream(bagId: UUID, path: Path): OutputStreamProvider => Try[Unit] = { outputStreamProducer =>
      for {
        f <- Try(escapePath(path))
        uri = baseUri.resolve(s"bags/$bagId/$f")
        _ <- http.copyHttpStream(uri)(outputStreamProducer)
      } yield ()
    }
  }
}
