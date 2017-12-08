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

// TODO rename to ~~FileItemAuthInfoSpec~~ UserSpec
class FileItemSpec extends TestSupportFixture {

  // TODO replace `shouldBe Success(())` with `shouldBe a[Success[_]]`, that's enough in this case

  "hasDownloadPermissionFor" should "allow archivist" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_REQUEST.toString,
      visibleTo = RESTRICTED_REQUEST.toString
    )
    val user = ArchivistUser("archie")
    user.hasDownloadPermissionFor(fileItem) shouldBe Success(())
  }

  // TODO what about admin? never tested, I also saw that in FileItemAuthInfo!

  it should "allow owner" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_REQUEST.toString,
      visibleTo = RESTRICTED_REQUEST.toString
    )
    val user = AuthenticatedUser("someone")
    user.hasDownloadPermissionFor(fileItem) shouldBe Success(())
  }

  it should "allow known" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) shouldBe Success(())
  }

  it should "reject metadata" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/file.txt" =>
    }
  }

  it should "allow metadata for owner" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    )
    val user = AuthenticatedUser("someone")
    user.hasDownloadPermissionFor(fileItem) shouldBe Success(())
  }

  it should "refuse metadata for others" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/file.txt" =>
    }
  }

  it should "announce availability after login" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = ANONYMOUS.toString
    )
    val user = UnauthenticatedUser
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(NotAccessibleException("Please login to download: uuid/data/file.txt")) =>
    }
  }

  it should "announce availability if under embargo" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }

  it should "refuse to user without group" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = RESTRICTED_GROUP.toString,
      visibleTo = ANONYMOUS.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(NotAccessibleException("Download not allowed of: uuid/data/file.txt")) =>
    }
  }

  it should "invisible for user without group" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = RESTRICTED_GROUP.toString,
      visibleTo = RESTRICTED_GROUP.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/data/file.txt" =>
    }
  }

  it should "announce availability if under embargo for group" in {
    val fileItem = FileItemAuthInfo(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_GROUP.toString,
      visibleTo = RESTRICTED_GROUP.toString
    )
    val user = AuthenticatedUser("somebody")
    user.hasDownloadPermissionFor(fileItem) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }
}
