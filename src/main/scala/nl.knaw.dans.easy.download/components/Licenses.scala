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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration

import scala.collection.JavaConverters._
import scala.util.Try

class Licenses(licenses: PropertiesConfiguration) extends DebugEnhancedLogging {

  private val OPEN_ACCESS_LICENSE = "http://creativecommons.org/publicdomain/zero/1.0"
  private val DANS_LICENSE = "http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSGeneralconditionsofuseUKDEF.pdf"

  def getLicenseLinkText(file: FileItem): Try[Option[String]] = Try {
    val licenseKey = if (file.isOpenAccess) OPEN_ACCESS_LICENSE
                     else DANS_LICENSE

    Option(licenses.getString(licenseKey))
      .map(licenseTitle => s"""<$licenseKey>; rel="license"; title="$licenseTitle"""")
      .orElse(licenseNotFound(licenseKey))
  }

  private def licenseNotFound(licenseKey: String): Option[String] = {
    logger.error("No license found with key: " + licenseKey)
    None
  }
}
