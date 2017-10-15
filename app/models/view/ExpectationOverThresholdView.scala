/*
 * Copyright 2017 HM Revenue & Customs
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

package models.view

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

import models.api.{VatExpectedThresholdPostIncorp, VatScheme, VatThresholdPostIncorp}
import models._
import play.api.libs.json._

import scala.util.Try

case class ExpectationOverThresholdView(selection: Boolean, date: Option[LocalDate] = None)

object ExpectationOverThresholdView {
  def bind(selection: Boolean, dateModel: Option[DayMonthYearModel]): ExpectationOverThresholdView =
    ExpectationOverThresholdView(selection, dateModel.flatMap(_.toLocalDate))

  def unbind(expect: ExpectationOverThresholdView): Option[(Boolean, Option[DayMonthYearModel])] =
    Try {
      expect.date.fold((expect.selection, Option.empty[DayMonthYearModel])) {
        d => (expect.selection, Some(DayMonthYearModel.fromLocalDate(d)))
      }
    }.toOption

  implicit val format = Json.format[ExpectationOverThresholdView]

  implicit val viewModelFormat = ViewModelFormat(
    readF = (group: S4LVatEligibilityChoice) => group.expectationOverThreshold,
    updateF = (c: ExpectationOverThresholdView, g: Option[S4LVatEligibilityChoice]) =>
      g.getOrElse(S4LVatEligibilityChoice()).copy(expectationOverThreshold = Some(c))
  )

  // Returns a view model for a specific part of a given VatScheme API model
  implicit val modelTransformer = ApiModelTransformer[ExpectationOverThresholdView] { vs: VatScheme =>
    vs.vatServiceEligibility.flatMap(_.vatEligibilityChoice.map{_.vatExpectedThresholdPostIncorp}).collect {
      case Some(VatExpectedThresholdPostIncorp(selection, d@_)) => ExpectationOverThresholdView(selection, d)
    }
  }
}