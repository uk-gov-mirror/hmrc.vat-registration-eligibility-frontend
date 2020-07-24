/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import utils.RadioOption

sealed trait CompletionCapacity

object CompletionCapacity {

  def multipleOfficers(officers : Seq[Officer], noneOfThese : Boolean = true): Seq[RadioOption] = officers.map {
    officer =>
      val officerName = List(officer.name.forename, Some(officer.name.surname)).flatten.mkString(" ")
      RadioOption(s"completionCapacity-${officer.generateId}", officer.generateId, officerName)
  } ++ (if (noneOfThese) Seq(RadioOption("completionCapacity-noneOfThese", "noneOfThese", "completionCapacity.noneOfThese")) else Seq())

  def singleOfficer(officer: Officer): Seq[RadioOption] = {
    List(
      RadioOption(s"completionCapacity-yes", officer.generateId, "site.yes"),
      RadioOption("completionCapacity-no", "noneOfThese", "site.no")
    )
  }
}
