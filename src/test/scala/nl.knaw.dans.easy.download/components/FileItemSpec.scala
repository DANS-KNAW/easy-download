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

import nl.knaw.dans.easy.download.TestSupportFixture
import nl.knaw.dans.easy.download.components.RightsFor._

import scala.util.Success

class FileItemSpec extends TestSupportFixture {

  "hasDownloadPermissionFor" should "allow archivist" in {
    FileItemAuthInfo("uuid/file.txt", "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_REQUEST.toString,
      visibleTo = RESTRICTED_REQUEST.toString
    ).hasDownloadPermissionFor(Some(User("archie", isArchivist = true))
    ) shouldBe Success(())
  }

  it should "allow owner" in {
    FileItemAuthInfo("uuid/file.txt", "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = RESTRICTED_REQUEST.toString,
      visibleTo = RESTRICTED_REQUEST.toString
    ).hasDownloadPermissionFor(Some(User("someone"))
    ) shouldBe Success(())
  }

  it should "allow known" in {
    FileItemAuthInfo("uuid/data/file.txt", "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))
    ) shouldBe Success(())
  }

  it should "reject metadata" in {
    FileItemAuthInfo("uuid/file.txt", "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))
    ) shouldNot be(Success(())) // TODO verify more details
  }

  it should "allow metadata for owner" in {
    FileItemAuthInfo("uuid/file.txt", "someone",
      dateAvailable = "2016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("someone"))
    ) shouldBe Success(())
  }

  it should "reject if under embargo" in {
    FileItemAuthInfo("uuid/data/file.txt", "someone",
      dateAvailable = "4016-12-15",
      accessibleTo = KNOWN.toString,
      visibleTo = KNOWN.toString
    ).hasDownloadPermissionFor(Some(User("somebody"))
    ) shouldNot be(Success(())) // TODO verify more details
  }
}
