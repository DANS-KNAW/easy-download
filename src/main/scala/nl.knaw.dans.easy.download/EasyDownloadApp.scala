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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Success, Try }

trait EasyDownloadApp extends AutoCloseable
  with DebugEnhancedLogging with ApplicationWiring {


  def copyStream(bagId: UUID, path: Path, outputStreamProducer: () => OutputStream): Try[Unit] = {
    bagStore.copyStream(bagId, path)(outputStreamProducer)
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