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

import nl.knaw.dans.easy.download.{ NotAccessibleException, TestSupportFixture }
import nl.knaw.dans.easy.download.components.RightsFor._

import scala.util.{ Failure, Success }

class FileItemSpec extends TestSupportFixture {

  "hasDownloadPermissionFor" should "allow archivist" in {
    FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_REQUEST.toString,
      visibleTo = RESTRICTED_REQUEST.toString
    ).hasDownloadPermissionFor(Some(User("archie", isArchivist = true))) shouldBe Success(())
  }

  it should "allow owner" in {
    FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_REQUEST.toString,
      visibleTo = RESTRICTED_REQUEST.toString
    ).hasDownloadPermissionFor(Some(User("someone"))) shouldBe Success(())
  }

  it should "allow known" in {
    FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) shouldBe Success(())
  }

  it should "reject metadata" in {
    FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/file.txt" =>
    }
  }

  it should "allow metadata for owner" in {
    FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("someone"))) shouldBe Success(())
  }

  it should "refuse metadata for others" in {
    FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/file.txt" =>
    }
  }

  it should "announce availability after login" in {
    FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = ANONYMOUS.toString
    ).hasDownloadPermissionFor(None) should matchPattern {
      case Failure(NotAccessibleException("Please login to download: uuid/data/file.txt")) =>
    }
  }

  it should "announce availability if under embargo" in {
    FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }

  it should "refuse to user without group" in {
    FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = RESTRICTED_GROUP.toString,
      visibleTo = ANONYMOUS.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download not allowed of: uuid/data/file.txt")) =>
    }
  }

  it should "invisible for user without group" in {
    FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = RESTRICTED_GROUP.toString,
      visibleTo = RESTRICTED_GROUP.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/data/file.txt" =>
    }
  }

  it should "announce availability if under embargo for group" in {
    FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_GROUP.toString,
      visibleTo = RESTRICTED_GROUP.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }
}
