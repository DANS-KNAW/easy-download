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

import javax.naming.ldap.LdapContext

import nl.knaw.dans.easy.download.TestSupportFixture
import org.scalamock.scalatest.MockFactory

import scala.util.{ Success, Try }

class AuthenticationComponentSpec extends TestSupportFixture with MockFactory {
  private val mockedLdpContext: LdapContext = mock[LdapContext]
  private class TestWiring extends AuthenticationComponent {
    override val authentication: Authentication = new Authentication {
      override val ldapUsersEntry: String = ""
      override val ldapProviderUrl: String = "http://"
    }
  }
  private val wiring = new TestWiring

  "authentication" should "parse the service response" ignore {
    // TODO needs also mock of 'new InitialLdapContext', first rewrite class under test with a single call
  }
}
