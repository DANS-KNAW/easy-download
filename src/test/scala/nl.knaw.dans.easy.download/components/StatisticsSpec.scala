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

import java.util.UUID

import nl.knaw.dans.easy.download.TestSupportFixture
import nl.knaw.dans.easy.download.components.RightsFor.KNOWN
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory

import scala.xml.XML

class StatisticsSpec extends TestSupportFixture with MockFactory {
  private val bagId = UUID.randomUUID()
  private val fileItem = FileItem(
    itemId = "uuid/data/file.txt",
    owner = "someone",
    dateAvailable = new DateTime("2016-12-15"),
    accessibleTo = KNOWN,
    visibleTo = KNOWN,
    licenseKey = "http://opensource.org/licenses/MIT",
    licenseTitle = "MIT.txt",
  )
  private val user_1 = User("user_1", Seq.empty)
  private val user_2 = User("user_2", Seq("a", "b"))
  private val ddm = XML.loadFile("src/test/resources/dataset-xml/dataset.xml")
  private val statistics_1 = Statistics(null, bagId, fileItem, Option(user_1), ddm)
  private val statistics_2 = Statistics(null, bagId, fileItem, Option(user_2), ddm)

  "getLogEventString" should "return correct log event line" in {
    statistics_1.getLogEventString(user_1) shouldBe "- DOWNLOAD_FILE_REQUEST ; user_1 ; roles: (USER) ; groups: () ;  ; dataset(DATASET_ID: \"easy-dataset:17\") ; file(FILE_NAME(0): \"file.txt\") ; discipline(SUB_DISCIPLINE_ID: \"easy-discipline:1\" ; TOP_DISCIPLINE_LABEL: \"\" ; SUB_DISCIPLINE_LABEL: \"Humanities\" ; TOP_DISCIPLINE_ID: \"easy-discipline:root\")"
  }

  it should "return a string containing 'SUB_DISCIPLINE_ID: easy-discipline:1'" in {
    statistics_1.getLogEventString(user_1) should include regex "SUB_DISCIPLINE_ID: \"easy-discipline:1\""
  }

  it should "return a string containing 'SUB_DISCIPLINE_LABEL: Humanities'" in {
    statistics_1.getLogEventString(user_1) should include regex "SUB_DISCIPLINE_LABEL: \"Humanities\""
  }

  it should "return a string containing 'groups: (a,b)'" in {
    statistics_2.getLogEventString(user_2) should include regex "groups: \\(a,b\\)"
  }
}
