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

import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.easy.download.TestSupportFixture
import org.json4s.MappingException
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class AuthInfoComponentSpec extends TestSupportFixture with MockFactory {
  private class TestWiring extends AuthInfoComponent
    with HttpWorkerComponent {
    override val http: HttpWorker = mock[HttpWorker]
    override val authInfo: AuthInfo = new AuthInfo {
      override val baseUri: URI = new URI("http://localhost:20170/")
    }
  }
  private val wiring = new TestWiring
  private val uuid = UUID.randomUUID()

  private def expectAutInfoRequest(path: Path) = {
    (wiring.http.getHttpAsString(_: URI)) expects wiring.authInfo.baseUri.resolve(s"$uuid/$path") once()
  }

  "getOutInfo" should "parse the service response" in {
    val path = Paths.get("some.file")
    expectAutInfoRequest(path) returning Success(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
    )
    wiring.authInfo.getFileItem(uuid, path) shouldBe a[Success[_]]
  }

  it should "complain about invalid service response" in {
    val path = Paths.get("some.file")
    expectAutInfoRequest(path) returning Success(
      s"""{"nonsense":"value"}"""
    )
    inside(wiring.authInfo.getFileItem(uuid, path)) {
      case Failure(t) => t should have message
        """parse error [No usable value for itemId
          |Did not find value which can be converted into java.lang.String] for: {"nonsense":"value"}""".stripMargin
    }
  }

  it should "stumble over invalid accessibleTo" in {
    val path = Paths.get("some.file")
    expectAutInfoRequest(path) returning Success(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"invalidValue",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
    )
    inside(wiring.authInfo.getFileItem(uuid, path)) {
      case Failure(t) =>
        t.getMessage shouldBe
          s"""parse error [unknown error] for: {
             |  "itemId":"$uuid/some.file",
             |  "owner":"someone",
             |  "dateAvailable":"1992-07-30",
             |  "accessibleTo":"invalidValue",
             |  "visibleTo":"ANONYMOUS"
             |}""".stripMargin
        // json type hints might result in clearer error messages, see TODO in FileItemAuthInfo
        t.getCause shouldBe a[MappingException]
        t.getCause.getCause shouldBe a[InvocationTargetException]
        t.getCause.getCause.getCause shouldBe a[NoSuchElementException]
        t.getCause.getCause.getCause.getMessage shouldBe "No value found for 'invalidValue'"
    }
  }
}
