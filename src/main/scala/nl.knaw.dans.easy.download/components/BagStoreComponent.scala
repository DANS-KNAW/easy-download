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


import java.io.{ InputStream, OutputStream }
import java.net.{ URI, URLEncoder }
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils

import scala.util.Try
import scalaj.http.Http

trait BagStoreComponent extends DebugEnhancedLogging {
  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

//    copyStream(uuid, path, os)
//    copyStream(uuid, path)(os)
//    copyStream(uuid, path)(os)
//  def copyStream(bagId: UUID, path: Path, outputStream: OutputStream): Try[Unit]
//  def copyStream(bagId: UUID, path: Path)(outputStream: OutputStream): Try[Unit]
//  copyStream :: (UUID, Path) -> (OutputStream -> Try[Unit])
    def copyStream(bagId: UUID, path: Path): OutputStream => Try[Unit] = { outputStream =>
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        uri <- Try(baseUri.resolve(s"stores/pdbs/bags/$bagId/$f")) // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
        _ <- Try(Http(uri.toString).method("GET").execute((is: InputStream) => IOUtils.copy(is, outputStream)))
      } yield ()
    }
  }
}
