/*
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
import javax.naming.directory.{ BasicAttributes, SearchControls, SearchResult }
import javax.naming.ldap.LdapContext
import javax.naming.{ AuthenticationException, NamingEnumeration }

import nl.knaw.dans.easy.download.{ AuthenticationNotAvailableException, AuthenticationTypeNotSupportedException, InvalidUserPasswordException, TestSupportFixture }
import org.scalamock.scalatest.MockFactory
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest

import scala.util.{ Failure, Success }

class AuthenticationComponentSpec extends TestSupportFixture with MockFactory {

  private val mockedLdpContext: LdapContext = mock[LdapContext]
  private val mockedAuthRequest: BasicAuthRequest = mock[BasicAuthRequest]
  private val mockedLdapSearchResults: NamingEnumeration[SearchResult] = mock[NamingEnumeration[SearchResult]]
  private val mockedLdapSearchResult: SearchResult = mock[SearchResult]

  private def expectLdapAttributes(attributes: BasicAttributes) = {
    (mockedLdpContext.search(_: String, _: String, _: SearchControls)) expects(*, *, *) returning mockedLdapSearchResults
    mockedLdpContext.close _ expects()

    mockedLdapSearchResults.hasMoreElements _ expects() returning true
    mockedLdapSearchResults.hasMoreElements _ expects() returning false
    mockedLdapSearchResults.nextElement _ expects() returning mockedLdapSearchResult
    mockedLdapSearchResult.getAttributes _ expects() returning attributes
  }

  private def expectAuthRequest(userName: String, password: String, basicAuthentication: Boolean = true): Any = {
    mockedAuthRequest.username _ expects() returning userName
    mockedAuthRequest.password _ expects() returning password
    mockedAuthRequest.providesAuth _ expects() returning true
    mockedAuthRequest.isBasicAuth _ expects() returning basicAuthentication
  }

  private class TestWiring extends AuthenticationComponent {
    override val authentication: Authentication = new Authentication {
      override val ldapUsersEntry: String = ""
      override val ldapProviderUrl: String = "http://"

      override def getContext(connectionProperties: util.Hashtable[String, String]): LdapContext = {
        mockedLdpContext
      }
    }
  }
  private val wiring = new TestWiring

  "authenticate" should "allow an active user" in {
    expectAuthRequest("someone", "somePassword")
    expectLdapAttributes(new BasicAttributes() {
      put("dansState", "ACTIVE")
    })

    wiring.authentication.authenticate(mockedAuthRequest) shouldBe Success(Some(User("someone")))
  }

  it should "not access ldap without authentication" in {
    mockedAuthRequest.providesAuth _ expects() returning false
    mockedAuthRequest.isBasicAuth _ expects() returning true

    wiring.authentication.authenticate(mockedAuthRequest) shouldBe Success(None)
  }

  it should "refuse a blocked user" in {
    expectAuthRequest("someone", "somePassword")
    expectLdapAttributes(new BasicAttributes() {
      put("dansState", "BLOCKED")
    })

    inside(wiring.authentication.authenticate(mockedAuthRequest)) {
      case Failure(InvalidUserPasswordException("someone", t)) => t.getMessage shouldBe "User [someone] found but not active"
    }
  }

  it should "report an invalid password" in {
    expectAuthRequest("someone", "somePassword")
    (mockedLdpContext.search(_: String, _: String, _: SearchControls)) expects(*, *, *) throwing new AuthenticationException()

    inside(wiring.authentication.authenticate(mockedAuthRequest)) {
      case Failure(InvalidUserPasswordException("someone", t)) => t.getMessage shouldBe "invalid password"
    }
  }

  it should "report invalid authentication type" in {
    mockedAuthRequest.providesAuth _ expects() returning true
    mockedAuthRequest.isBasicAuth _ expects() returning false

    inside(wiring.authentication.authenticate(mockedAuthRequest)) {
      case Failure(AuthenticationTypeNotSupportedException(t)) => t.getMessage shouldBe "Supporting only basic authentication"
    }
  }

  it should "report other ldap problems" in {
    expectAuthRequest("someone", "somePassword")
    (mockedLdpContext.search(_: String, _: String, _: SearchControls)) expects(*, *, *) throwing new Exception("whoops")

    inside(wiring.authentication.authenticate(mockedAuthRequest)) {
      case Failure(AuthenticationNotAvailableException(t)) => t.getMessage shouldBe "whoops"
    }
  }
}
