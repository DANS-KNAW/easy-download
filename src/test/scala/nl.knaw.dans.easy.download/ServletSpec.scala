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

import java.io.OutputStream
import java.nio.file.Paths
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

  private class Wiring extends ApplicationWiring(new Configuration("", new PropertiesConfiguration() {
    // need a constructor without arguments for the mock, the constructor needs a valid property
    addProperty("bag-store.url", "http://localhost:20110/")
  }))
  private val uuid = UUID.randomUUID()
  private val wiring = mock[Wiring] // mocking at a low level to test the chain of error handling
  addServlet(new EasyAuthInfoServlet(new EasyAuthInfoApp(wiring)), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Auth Info Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return file" in {
    wiring.bagStore.copyStream _ expects (*,*,*) once() returning Success("EASY-File:someone")

    get(s"$uuid/some.file") {
      status shouldBe OK_200
      body shouldBe
        s"""{
           |  "itemId":"$uuid/some.file",
           |  "owner":"someone",
           |  "accessibleTo":"KNOWN",
           |  "visibleTo":"KNOWN"
           |}""".stripMargin
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