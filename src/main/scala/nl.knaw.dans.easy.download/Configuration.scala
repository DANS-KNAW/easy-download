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

import java.nio.file.{ Files, Path, Paths }

import nl.knaw.dans.easy.download.components.Licenses
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration
import resource.managed

import scala.io.Source

case class Configuration(version: String, properties: PropertiesConfiguration, licenses: Licenses)

object Configuration extends DebugEnhancedLogging {

  def apply(home: Path): Configuration = {
    logger.info(s"home: $home")
    val cfgPath = Seq(
      Paths.get(s"/etc/opt/dans.knaw.nl/easy-download/"),
      home.resolve("cfg"))
      .find(Files.exists(_))
      .getOrElse { throw new IllegalStateException("No configuration directory found") }
    logger.info(s"cfgPath: $cfgPath")

    new Configuration(
      version = managed(Source.fromFile(home.resolve("bin/version").toFile)).acquireAndGet(_.mkString).stripLineEnd,
      properties = new PropertiesConfiguration() {
        setDelimiterParsingDisabled(true)
        load(cfgPath.resolve("application.properties").toFile)
      },
      licenses = new Licenses(new PropertiesConfiguration(cfgPath.resolve("lic/licenses.properties").toFile))
    )
  }
}
