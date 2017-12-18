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
import javax.naming.Context
import javax.naming.directory.{ Attribute, SearchControls, SearchResult }
import javax.naming.ldap.{ InitialLdapContext, LdapContext }

import nl.knaw.dans.easy.download.{ AuthenticationTypeNotSupportedException, InvalidUserPasswordException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import resource.managed

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

trait AuthenticationComponent extends DebugEnhancedLogging {

  val authentication: Authentication

  trait Authentication {
    val ldapUsersEntry: String
    val ldapProviderUrl: String

    def authenticate(authRequest: BasicAuthRequest): Try[Option[User]] = {

      (authRequest.providesAuth, authRequest.isBasicAuth) match {
        case (true, true) => getUser(authRequest.username, authRequest.password).map(Some(_))
        case (true, _) => Failure(AuthenticationTypeNotSupportedException(new Exception("Supporting only basic authentication")))
        case (_, _) => Success(None)
      }
    }

    private def getUser(userName: String, password: String): Try[User] = {
      // inner functions reuse the arguments

      logger.info(s"looking for user [$userName]")

      def findUserWithItsOwnCredentials = {
        managed(
          new InitialLdapContext(
            new util.Hashtable[String, String]() {
              put(Context.PROVIDER_URL, ldapProviderUrl)
              put(Context.SECURITY_AUTHENTICATION, "simple")
              put(Context.SECURITY_PRINCIPAL, s"uid=$userName, $ldapUsersEntry")
              put(Context.SECURITY_CREDENTIALS, password)
              put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
            },
            null
          )
        ).map(_.search(
          ldapUsersEntry,
          s"(&(objectClass=easyUser)(uid=$userName))",
          new SearchControls() {setSearchScope(SearchControls.SUBTREE_SCOPE) }
        )).tried
      }

      def toTuples(a: Attribute) = {
        (a.getID, a.getAll.asScala.toArray.map(_.toString).toSeq)
      }

      def oneAndOnlyUser(list: List[SearchResult]) = {
        if (list.isEmpty)
          Failure(InvalidUserPasswordException(userName, new Exception("not found")))
        else Success(list.head)
      }

      def userIsActive(map: Map[String, Seq[String]]) = {
        val values = map.getOrElse("dansState", Seq.empty)
        logger.info(s"state of $userName: $values")
        if (values.contains("ACTIVE")) Success(())
        else Failure(InvalidUserPasswordException(userName, new Exception("not active")))
      }

      for {
        searchResult <- findUserWithItsOwnCredentials
        first <- oneAndOnlyUser(searchResult.asScala.toList)
        userAttributes = first.getAttributes.getAll.asScala.toArray.map(toTuples).toMap
        _ <- userIsActive(userAttributes)
        roles = userAttributes.getOrElse("easyRoles", Seq.empty)
      } yield User(userName,
        isArchivist = roles.contains("ARCHIVIST"),
        isAdmin = roles.contains("ADMIN"),
        groups = userAttributes.getOrElse("easyGroups", Seq.empty)
      )
    }
  }
}
