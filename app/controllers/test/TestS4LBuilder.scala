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
    val taxableTurnover: Option[Boolean] = data.taxableTurnoverChoice

    val overThresholdView: Option[ThresholdView] = data.overThresholdTwelveSelection match {
      case Some(true) => Some(ThresholdView(selection = true, Some(LocalDate.of(
        data.overThresholdTwelveYear.map(_.toInt).get,
        data.overThresholdTwelveMonth.map(_.toInt).get,
        1
      ).`with`(TemporalAdjusters.lastDayOfMonth()))))
      case Some(false) => Some(ThresholdView(selection = false, None))
      case _ => None
    }

    val pastOverThresholdView: Option[ThresholdView] = data.pastOverThresholdThirtySelection match {
      case Some(true) => Some(ThresholdView(selection = true, Some(LocalDate.of(
        data.pastOverThresholdThirtyYear.map(_.toInt).get,
        data.pastOverThresholdThirtyMonth.map(_.toInt).get,
        data.pastOverThresholdThirtyDay.map(_.toInt).get
      ))))
      case Some(false) => Some(ThresholdView(selection = false, None))
      case _ => None
    }

    val overThresholdThirtyView: Option[ThresholdView] = data.overThresholdThirtySelection match {
      case Some(true) => Some(ThresholdView(selection = true, Some(LocalDate.of(
        data.overThresholdThirtyYear.map(_.toInt).get,
        data.overThresholdThirtyMonth.map(_.toInt).get,
        data.overThresholdThirtyDay.map(_.toInt).get
      ))))
      case Some(false) => Some(ThresholdView(selection = false, None))
      case _ => None
    }

    val voluntaryRegistration: Option[Boolean] = data.voluntaryChoice
    val voluntaryRegistrationReason: Option[String] = data.voluntaryRegistrationReason

    Threshold(
      overThresholdThirtyDaysPreIncorp = taxableTurnover,
      voluntaryRegistration = voluntaryRegistration,
      voluntaryRegistrationReason = voluntaryRegistrationReason,
      overThresholdOccuredTwelveMonth = overThresholdView,
      pastOverThresholdThirtyDays = pastOverThresholdView,
      overThresholdThirtyDays = overThresholdThirtyView
    )
  }
}
