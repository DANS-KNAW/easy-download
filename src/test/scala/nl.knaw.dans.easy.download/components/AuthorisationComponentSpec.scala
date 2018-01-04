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

import java.net.URI
import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.easy.download.TestSupportFixture
import nl.knaw.dans.easy.download.components.RightsFor._
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class AuthorisationComponentSpec extends TestSupportFixture with MockFactory {
  private class TestWiring extends AuthorisationComponent
    with HttpWorkerComponent {
    override val http: HttpWorker = mock[HttpWorker]
    override val authorisation: Authorisation = new Authorisation {
      override val baseUri: URI = new URI("http://localhost:20170/")
    }
  }
  private val wiring = new TestWiring
  private val uuid = UUID.randomUUID()

  private def expectAuthInfoRequest(path: Path) = {
    (wiring.http.getHttpAsString(_: URI)) expects wiring.authorisation.baseUri.resolve(s"$uuid/$path") once()
  }

  "getAuthInfo" should "parse the service response" in {
    val path = Paths.get("some.file")
    expectAuthInfoRequest(path) returning Success(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
    )
    wiring.authorisation.getFileItem(uuid, path) should matchPattern {
      case Success(FileItem(_, "someone", _, KNOWN, ANONYMOUS)) =>
    }
  }

  it should "complain about invalid service response" in {
    val path = Paths.get("some.file")
    expectAuthInfoRequest(path) returning Success(
      s"""{"nonsense":"value"}"""
    )
    inside(wiring.authorisation.getFileItem(uuid, path)) {
      case Failure(t) => t.getMessage shouldBe
        """parse error [class org.json4s.package$MappingException: No usable value for itemId
          |Did not find value which can be converted into java.lang.String] for: {"nonsense":"value"}""".stripMargin
    }
  }
}
