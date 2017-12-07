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
import java.nio.file.{ Path, Paths }
import java.util.UUID

import org.apache.commons.configuration.PropertiesConfiguration
import org.eclipse.jetty.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.Success

class ServletSpec extends TestSupportFixture with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private val uuid = UUID.randomUUID()
  private val naan = "123456"
  private val app = new EasyDownloadApp {
    // mocking at a low level to test the chain of error handling

    override val http: HttpWorker = mock[HttpWorker]
    override val authentication: Authentication = mock[Authentication]
    override lazy val configuration: Configuration = new Configuration("", new PropertiesConfiguration() {
      setDelimiterParsingDisabled(true)
      addProperty("bag-store.url", "http://localhost:20110/")
      addProperty("auth-info.url", "http://localhost:20170/")
      addProperty("ark.name-assigning-authority-number", naan)
    })
  }
  addServlet(new EasyDownloadServlet(app), "/*")

  private def expectDownloadStream(path: Path) = {
    (app.http.copyHttpStream(_: URI)) expects new URI(s"http://localhost:20110/bags/$uuid/$path") once()
  }

  private def expectAuthInfo(path: Path) = {
    (app.http.getHttpAsString(_: URI)) expects new URI(s"http://localhost:20170/$uuid/$path") once()
  }

  private def expectAuthentication() = {
    (app.authentication.authenticate(_: BasicAuthRequest)) expects * once()
  }

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Download Service running..."
      status shouldBe OK_200
    }
  }

  s"get ark:/$naan/:uuid/*" should "return file" in {
    val path = Paths.get("data/some.file")
    expectAuthentication() returning Success(None)
    expectDownloadStream(path) returning (os => {
      os().write(s"content of $uuid/$path ")
      Success(())
    })
    expectAuthInfo(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"ANONYMOUS",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      body shouldBe s"content of $uuid/$path "
      status shouldBe OK_200
    }
  }


  it should "report invalid authInfo results" in {
    val path = Paths.get("some.file")
    expectAuthInfo(path) returning Success(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"invaidValue",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
    )
    expectAuthentication() returning Success(None)
    get(s"ark:/$naan/$uuid/some.file") {
      // logged message shown in AuthInfoSpec
      body shouldBe s"not expected exception"
      status shouldBe INTERNAL_SERVER_ERROR_500
    }
  }

  it should "report invisible as not found" in {
    val path = Paths.get("some.file")
    expectAuthentication() returning Success(None)
    expectAuthInfo(path) returning Success(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"KNOWN"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/some.file") {
      body shouldBe s"not found: $uuid/$path"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "forbid download for not authenticated user" in {
    val path = Paths.get("data/some.file")
    expectAuthentication() returning Success(None)
    expectAuthInfo(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      body shouldBe s"Please login to download: $uuid/$path"
      status shouldBe FORBIDDEN_403
    }
  }

  it should "report invalid uuid" in {
    expectAuthentication() returning Success(None)
    get(s"ark:/$naan/1-2-3-4-5-6/some.file") {
      body shouldBe "Invalid UUID string: 1-2-3-4-5-6"
      status shouldBe BAD_REQUEST_400
    }
  }

  it should "report missing path" in {
    expectAuthentication() returning Success(None)
    get(s"ark:/$naan/$uuid/") {
      body shouldBe "file path is empty"
      status shouldBe BAD_REQUEST_400
    }
  }

  it should "report wrong naan" in {
    get(s"ark:/$naan$naan/$uuid/") {
      body shouldBe
        s"""Requesting "GET /ark:/$naan$naan/$uuid/" on servlet "" but only have: <ul><li>GET /</li><li>GET /ark:/$naan/:uuid/*</li></ul>
           |""".stripMargin
      status shouldBe NOT_FOUND_404
    }
  }
}