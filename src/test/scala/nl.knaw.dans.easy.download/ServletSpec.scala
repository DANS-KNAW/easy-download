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

import java.nio.file.{ Path, Paths }
import java.util.UUID

import org.apache.commons.configuration.PropertiesConfiguration
import org.eclipse.jetty.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scalaj.http.HttpResponse

class ServletSpec extends TestSupportFixture with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private val wiring = new ApplicationWiring(new Configuration("", new PropertiesConfiguration() {
    addProperty("bag-store.url", "http://localhost:20110/")
  })) {
    // mocking at a low level to test the chain of error handling
    override val bagStore: BagStore = mock[BagStore]
  }
  private val uuid = UUID.randomUUID()
  addServlet(new EasyDownloadServlet(new EasyDownloadApp(wiring)), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Download Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return file" in {
    val path = Paths.get("some.file")
    (wiring.bagStore.copyStream(_: UUID, _: Path)) expects(uuid, path) once() returning (os => {
      os().write(s"content of $uuid/$path ")
      Success(())
    })

    get(s"$uuid/some.file") {
      status shouldBe OK_200
      body shouldBe
        s"content of $uuid/$path "
    }
  }

  it should "report invalid uuid" in {
    get("1-2-3-4-5-6/some.file") {
      body shouldBe "Invalid UUID string: 1-2-3-4-5-6"
      status shouldBe BAD_REQUEST_400
    }
  }

  it should "report missing path" in {
    get(s"$uuid/") {
      body shouldBe "file path is empty"
      status shouldBe BAD_REQUEST_400
    }
  }

  private def httpException(message: String, code: Int = 404) = {
    val headers = Map[String, String]("Status" -> s"$code")
    Failure(HttpStatusException(message, HttpResponse("", code, headers)))
  }
}