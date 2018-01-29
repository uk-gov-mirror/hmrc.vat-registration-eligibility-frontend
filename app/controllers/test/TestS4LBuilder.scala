/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.test

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import models.test.ThresholdTestSetup
import models.view._

class TestS4LBuilderImpl extends TestS4LBuilder

trait TestS4LBuilder {
  def thresholdFromData(data: ThresholdTestSetup): Threshold = {
    val taxableTurnover: Option[String] = data.taxableTurnoverChoice

    val overThresholdView: Option[OverThresholdView] = data.overThresholdSelection match {
      case Some("true") => Some(OverThresholdView(selection = true, Some(LocalDate.of(
        data.overThresholdYear.map(_.toInt).get,
        data.overThresholdMonth.map(_.toInt).get,
        1
      ).`with`(TemporalAdjusters.lastDayOfMonth()))))
      case Some("false") => Some(OverThresholdView(selection = false, None))
      case _ => None
    }

    val expectationOverThresholdView: Option[ExpectationOverThresholdView] = data.expectationOverThresholdSelection match {
      case Some("true") => Some(ExpectationOverThresholdView(selection = true, Some(LocalDate.of(
        data.expectationOverThresholdYear.map(_.toInt).get,
        data.expectationOverThresholdMonth.map(_.toInt).get,
        data.expectationOverThresholdDay.map(_.toInt).get
      ))))
      case Some("false") => Some(ExpectationOverThresholdView(selection = false, None))
      case _ => None
    }

    val voluntaryRegistration: Option[String] = data.voluntaryChoice
    val voluntaryRegistrationReason: Option[String] = data.voluntaryRegistrationReason

    Threshold(
      taxableTurnover = taxableTurnover.map(TaxableTurnover(_)),
      voluntaryRegistration = voluntaryRegistration.map(VoluntaryRegistration(_)),
      voluntaryRegistrationReason = voluntaryRegistrationReason.map(VoluntaryRegistrationReason(_)),
      overThreshold = overThresholdView,
      expectationOverThreshold = expectationOverThresholdView
    )
  }
}
