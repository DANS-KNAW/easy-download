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

import java.util
import javax.naming.directory.{ SearchControls, SearchResult }
import javax.naming.ldap.{ InitialLdapContext, LdapContext }
import javax.naming.{ AuthenticationException, Context, NamingEnumeration }

import nl.knaw.dans.easy.download.{ AuthenticationNotAvailableException, AuthenticationTypeNotSupportedException, InvalidUserPasswordException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

trait AuthenticationComponent extends DebugEnhancedLogging {

  val authentication: Authentication

  trait Authentication {
    val ldapContext: Try[LdapContext]
    val ldapUsersEntry: String
    val ldapProviderUrl: String

    def authenticate(authRequest: BasicAuthRequest): Try[User] = {
      (authRequest.providesAuth, authRequest.isBasicAuth) match {
        case (true, true) => getUser(authRequest.username, authRequest.password)
        case (true, _) => Failure(AuthenticationTypeNotSupportedException(new Exception("Supporting only basic authentication")))
        case (_, _) => Success(AnonymousUser)
      }
    }

    private def getUser(userName: String, password: String): Try[User] = {
      logger.info(s"looking for user [$userName]")

      // TODO all these inner functions make the overall structure somewhat harder to read.
      // make private functions from them
      def toUser(searchResult: SearchResult): User = {
        def getAttrs(key: String): Seq[String] = {
          Option(searchResult.getAttributes.get(key))
            .map(_.getAll.asScala.toSeq.map(_.toString))
            .getOrElse(Seq.empty)
        }

        val roles = getAttrs("easyRoles")
        val groups = getAttrs("easyGroups")

        if (roles contains "ADMIN")
          AdminUser(userName, groups)
        else if (roles contains "ARCHIVIST")
          ArchivistUser(userName, groups)
        else
          KnownUser(userName, groups)
      }

      // TODO I don't really understand what's going on here. Maybe add one or two lines of explanation?
      // TODO why do you return the `LdapContext` here, but then discard it in it's single use?
      def validPassword: Try[InitialLdapContext] = Try {
        val env = new util.Hashtable[String, String]() {
          put(Context.PROVIDER_URL, ldapProviderUrl)
          put(Context.SECURITY_AUTHENTICATION, "simple")
          put(Context.SECURITY_PRINCIPAL, s"uid=$userName, $ldapUsersEntry")
          put(Context.SECURITY_CREDENTIALS, password)
          put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        }
        new InitialLdapContext(env, null)
        // TODO can we get attributes from this context?
      }.recoverWith {
        case t: AuthenticationException => Failure(InvalidUserPasswordException(userName, new Exception("invalid password", t)))
        case t => Failure(t)
      }

      def findUser(userAttributes: NamingEnumeration[SearchResult]): Try[User] = {
        userAttributes.asScala.toList.headOption match {
          case Some(sr) => Success(toUser(sr))
          case None => Failure(InvalidUserPasswordException(userName, new Exception("not found")))
        }
      }

      val searchFilter = s"(&(objectClass=easyUser)(uid=$userName))"
      val searchControls = new SearchControls() {
        setSearchScope(SearchControls.SUBTREE_SCOPE)
      }
      val user = for {
        context <- ldapContext
        _ <- validPassword
        userAttributes = context.search(ldapUsersEntry, searchFilter, searchControls)
        user <- findUser(userAttributes)
      } yield user

      user.recoverWith {
        case t: InvalidUserPasswordException => Failure(t)
        case t => Failure(AuthenticationNotAvailableException(t))
      }
    }
  }
}
