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

import java.io.FileNotFoundException
import java.util.UUID

import nl.knaw.dans.easy.download.components.RightsFor._
import nl.knaw.dans.easy.download._
import org.joda.time.DateTime

import scala.util.{ Failure, Success }

class FileItemSpec extends TestSupportFixture {

  "hasDownloadPermissionFor" should "allow owner" in {
    FileItem(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = new DateTime("4016-12-15"),
      accessibleTo = RESTRICTED_REQUEST,
      visibleTo = RESTRICTED_REQUEST,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("someone"))) shouldBe Success(())
  }

  it should "allow known" in {
    FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = KNOWN,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("somebody"))) shouldBe Success(())
  }

  it should "allow metadata for owner" in {
    FileItem(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = KNOWN,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("someone"))) shouldBe Success(())
  }

  it should "announce availability after login" in {
    FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(None) should matchPattern {
      case Failure(NotAccessibleException("Please login to download: uuid/data/file.txt")) =>
    }
  }

  it should "announce availability if under embargo" in {
    FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("4016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }

  it should "refuse to user without group" in {
    FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download not allowed of: uuid/data/file.txt")) =>
    }
  }

  it should "invisible for user without group" in {
    FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = RESTRICTED_GROUP,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("somebody"))) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/data/file.txt" =>
    }
  }

  it should "announce availability if under embargo for group" in {
    FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("4016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt"
    ).availableFor(Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }

  private val uuid = UUID.randomUUID()

  "fromJson" should "stumble over invalid accessibleTo" in {
    val input =
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"invalidValue",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT",
         |  "licenseTitle":"MIT.txt"
         |}""".stripMargin.toOneLiner
    inside(FileItem.fromJson(input)) {
      case Failure(t) =>
        t.getMessage should startWith ("parse error")
        t.getMessage should endWith (s" for: $input")
        t.getMessage should include ("accessibleTo")
        t.getMessage should include ("invalidValue")
        t.getMessage should include ("http://opensource.org/licenses/MIT")
        t.getMessage should include ("MIT.txt")
    }
  }

  it should "stumble over invalid date" in {
    val input =
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"today",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT",
         |  "licenseTitle":"MIT.txt"
         |}""".stripMargin.toOneLiner
    inside(FileItem.fromJson(input)) {
      case Failure(t) =>
        t.getMessage should startWith ("parse error")
        t.getMessage should endWith (s" for: $input")
        t.getMessage should include ("dateAvailable")
        t.getMessage should include ("today")
        t.getMessage should include ("http://opensource.org/licenses/MIT")
        t.getMessage should include ("MIT.txt")
    }
  }

  it should "convert different order of fields" in {
    inside(FileItem.fromJson(
      s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT",
         |  "licenseTitle":"MIT.txt"
         |}""".stripMargin
    )) { case Success(_) =>}
    inside(FileItem.fromJson(
      s"""{
         |  "accessibleTo":"KNOWN",
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "licenseTitle":"MIT.txt",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT"
         |}""".stripMargin
    )) { case Success(_) =>}
  }
}
