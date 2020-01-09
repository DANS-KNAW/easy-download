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

import nl.knaw.dans.lib.encode.PathEncoding
import org.apache.commons.configuration.PropertiesConfiguration
import org.eclipse.jetty.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.auth.strategy.BasicAuthStrategy.BasicAuthRequest
import org.scalatra.test.EmbeddedJettyContainer
import org.scalatra.test.scalatest.ScalatraSuite

import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }
import scala.xml.XML

class ServletSpec extends TestSupportFixture with EmbeddedJettyContainer
  with ScalatraSuite
  with MockFactory {

  private val uuid = UUID.randomUUID()
  private val naan = "123456"
  private val datasetXml = XML.loadFile("src/test/resources/dataset-xml/dataset.xml")
  private val app: EasyDownloadApp = new EasyDownloadApp {
    // mocking at a low level to test the chain of error handling
    override val http: HttpWorker = mock[HttpWorker]
    override val bagStore: BagStore = mock[BagStore]
    override val authentication: Authentication = mock[Authentication]
    override lazy val configuration: Configuration = new Configuration("1.0.0",
      new PropertiesConfiguration() {
        addProperty("bag-store.url", "http://localhost:20110/")
        addProperty("bag-store.connection-timeout-ms", 600000)
        addProperty("bag-store.read-timeout-ms", 600000)
        addProperty("auth-info.url", "http://localhost:20170/")
        addProperty("ark.name-assigning-authority-number", naan)
      })
  }
  addServlet(new EasyDownloadServlet(app), "/*")

  private def expectDownloadStream(uuid: UUID, path: Path, n: Int = 1) = {
    (app.bagStore.copyStream(_: UUID, _: Path)) expects(*, *) repeat n
  }

  private def expectLoadDdm(uuid: UUID) = {
    (app.bagStore.loadDDM(_: UUID)) expects *
  }

  private def expectAuthorisation(path: Path, n: Int = 1) = {
    (app.http.getHttpAsString(_: URI)) expects new URI(s"http://localhost:20170/$uuid/${ path.escapePath }") repeat n
  }

  private def expectAuthentication(n: Int = 1) = {
    (app.authentication.authenticate(_: BasicAuthRequest)) expects * repeat n
  }

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Download Service running v1.0.0."
      status shouldBe OK_200
    }
  }

  s"get ark:/$naan/:uuid/*" should "return file" in {
    val path = Paths.get("data/some.file")
    expectAuthentication(0)
    expectDownloadStream(uuid, path) returning (os => {
      os().write(s"content of $uuid/$path ")
      Success(())
    })
    expectLoadDdm(uuid) returning Try(datasetXml)
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"ANONYMOUS",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://creativecommons.org/publicdomain/zero/1.0",
         |  "licenseTitle":"CC0-1.0.html"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      body shouldBe s"content of $uuid/$path "
      status shouldBe OK_200
    }
  }

  it should "return file when it is Open Access even when the user is not authenticated" in {
    val path = Paths.get("data/some.file")
    expectAuthentication(0)
    expectDownloadStream(uuid, path) returning (os => {
      os().write(s"content of $uuid/$path ")
      Success(())
    })
    expectLoadDdm(uuid) returning Try(datasetXml)
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"ANONYMOUS",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://creativecommons.org/publicdomain/zero/1.0",
         |  "licenseTitle":"CC0-1.0.html"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      body shouldBe s"content of $uuid/$path "
      status shouldBe OK_200
    }
  }

  it should "return UN_AUTHORIZED error when the file is not Open Access and when the user is not authenticated" in {
    val path = Paths.get("data/some.file")
    expectAuthentication() returning Failure(InvalidUserPasswordException("user", new Exception("invalid password", new Throwable)))
    expectDownloadStream(uuid, path, 0)
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT",
         |  "licenseTitle":"MIT.txt"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      status shouldBe UNAUTHORIZED_401
    }
  }

  it should "return in header a Link with the license key and license title received from Authorization" in {
    val path = Paths.get("data/some.file")
    expectAuthentication(0)
    expectDownloadStream(uuid, path) returning (os => {
      os().write(s"content of $uuid/$path ")
      Success(())
    })
    expectLoadDdm(uuid) returning Try(datasetXml)
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"ANONYMOUS",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://creativecommons.org/publicdomain/zero/1.0",
         |  "licenseTitle":"CC0-1.0.html"
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      val licenseLinkText = """<http://creativecommons.org/publicdomain/zero/1.0>; rel="license"; title="CC0-1.0.html""""
      header("Link") shouldBe licenseLinkText
      status shouldBe OK_200
    }
  }

  it should "report invalid authorisation results" in {
    val path = Paths.get("some.file")
    val expectedHttpResponse =
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"invalidValue",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"",
         |  "licenseTitle":""
         |}""".stripMargin
    expectAuthorisation(path) returning Success(expectedHttpResponse)
    expectAuthentication() returning Success(None)
    get(s"ark:/$naan/$uuid/some.file") {
      // logged message shown in AuthorisationSpec
      body shouldBe s"not expected exception"
      status shouldBe INTERNAL_SERVER_ERROR_500
    }
  }

  it should "report invisible as not found" in {
    val path = Paths.get("some.file")
    expectAuthentication() returning Success(None)
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"KNOWN",
         |  "licenseKey":"",
         |  "licenseTitle":""
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
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"",
         |  "licenseTitle":""
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuid/$path") {
      body shouldBe s"Please login to download: $uuid/$path"
      status shouldBe FORBIDDEN_403
    }
  }

  it should "report invalid uuid" in {
    expectAuthentication(0)
    get(s"ark:/$naan/1-2-3-4-5-6/some.file") {
      body shouldBe "String '1-2-3-4-5-6' is not a UUID"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "accept uuid without hyphens" in {
    val uuidWithoutHyphens = uuid.toString.replace("-", "")
    val path = Paths.get("data/some.file")
    expectAuthentication(0)
    expectDownloadStream(uuid, path) returning (os => {
      os().write(s"content of $uuid/$path ")
      Success(())
    })
    expectLoadDdm(uuid) returning Try(datasetXml)
    expectAuthorisation(path) returning Success(
      s"""{
         |  "itemId":"$uuid/$path",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"ANONYMOUS",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"",
         |  "licenseTitle":""
         |}""".stripMargin
    )
    get(s"ark:/$naan/$uuidWithoutHyphens/$path") {
      body shouldBe s"content of $uuid/$path "
      status shouldBe OK_200
    }
  }

  it should "not accept uuid with hyphens in the wrong places" in {
    val uuidWithoutHyphens = uuid.toString.replace("-", "")
    val uuidWithHyphenInWrongPlace = s"${ uuidWithoutHyphens.slice(0, 6) }-${ uuidWithoutHyphens.slice(6, 32) }"
    val path = Paths.get("data/some.file")

    expectAuthentication(0)
    (app.http.copyHttpStream(_: URI)) expects * never()
    (app.http.getHttpAsString(_: URI)) expects * never()

    get(s"ark:/$naan/$uuidWithHyphenInWrongPlace/$path") {
      body shouldBe s"String '$uuidWithHyphenInWrongPlace' is not a UUID"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "report missing path" in {
    expectAuthentication(0)
    get(s"ark:/$naan/$uuid/") {
      body shouldBe "file path is empty"
      status shouldBe METHOD_NOT_ALLOWED_405
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
