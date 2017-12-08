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

import java.net.URI
import javax.naming.Context
import javax.naming.ldap.{ InitialLdapContext, LdapContext }

import nl.knaw.dans.easy.download.components.{ AuthenticationComponent, AuthorisationComponent, BagStoreComponent, HttpWorkerComponent }

import scala.util.Try

/**
 * Initializes and wires together the components of this application.
 */
trait ApplicationWiring extends HttpWorkerComponent
  with AuthorisationComponent
  with AuthenticationComponent
  with BagStoreComponent {

  /**
   * the application configuration
   */
  val configuration: Configuration
  override val http: HttpWorker = new HttpWorker {}

  override val bagStore: BagStore = new BagStore {
    override val baseUri: URI = new URI(configuration.properties.getString("bag-store.url"))
  }
  override val authorisation: Authorisation = new Authorisation {
    override val baseUri: URI = new URI(configuration.properties.getString("auth-info.url"))
  }

  override val authentication: Authentication = new Authentication {
    override val ldapUsersEntry: String = configuration.properties.getString("ldap.users-entry")
    override val ldapProviderUrl: String = configuration.properties.getString("ldap.provider.url")
    override val adminLdapContext: Try[LdapContext] = Try {
      val env = new java.util.Hashtable[String, String] {
        put(Context.SECURITY_AUTHENTICATION, "simple")
        put(Context.SECURITY_PRINCIPAL, configuration.properties.getString("ldap.securityPrincipal"))
        put(Context.SECURITY_CREDENTIALS, configuration.properties.getString("ldap.securityCredentials"))
        put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        put(Context.PROVIDER_URL, ldapProviderUrl)
      }
      new InitialLdapContext(env, null)
    }
    logger.info(s"ldapContext = $adminLdapContext") // TODO how to fail at service startup?
  }
}