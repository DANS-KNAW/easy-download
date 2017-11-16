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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration) extends DebugEnhancedLogging {

//  def getAllBags(): Try[FeedBackMessage] = {
//
//        contentType = "text/plain"
//        val (includeActive, includeInactive) = includedStates(params.get("state"))
//        bagStores.enumBags(includeActive, includeInactive)
//          .map(bagIds => Ok(bagIds.mkString("\n")))
//          .getOrRecover(e => {
//            logger.error("Unexpected type of failure", e)
//            InternalServerError(s"[${ new DateTime() }] Unexpected type of failure. Please consult the logs")
//          })
//  }
}