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

package forms

import java.time.LocalDate

import forms.FormValidation.Dates.{incorporationDateValidation, nonEmptyMonthYearModel, validPartialMonthYearModel}
import forms.FormValidation.missingBooleanFieldMappingArgs
import models.MonthYearModel
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.OverThresholdView
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

import scala.util.Try

object OverThresholdFormFactory {
  val RADIO_YES_NO = "overThresholdRadio"

  def bind(selection: Boolean, dateModel: Option[MonthYearModel]): OverThresholdView =
    OverThresholdView(selection, dateModel.flatMap(_.toLocalDate))

  def unbind(overThreshold: OverThresholdView): Option[(Boolean, Option[MonthYearModel])] =
    Try {
      overThreshold.date.fold((overThreshold.selection, Option.empty[MonthYearModel])) {
        d => (overThreshold.selection, Some(MonthYearModel.fromLocalDate(d)))
      }
    }.toOption

  def form(dateOfIncorporation: LocalDate): Form[OverThresholdView] = {
    implicit val specificErrorCode: String = "overThreshold.date"

    Form(
      mapping(
        RADIO_YES_NO -> missingBooleanFieldMappingArgs()(Seq(dateOfIncorporation.format(FORMAT_DD_MMMM_Y)))("overThreshold.selection"),
        "overThreshold" -> mandatoryIf(
          isEqual(RADIO_YES_NO, "true"),
          mapping(
            "month" -> text,
            "year" -> text
          )(MonthYearModel.apply)(MonthYearModel.unapply).verifying(
            nonEmptyMonthYearModel(validPartialMonthYearModel(incorporationDateValidation(dateOfIncorporation)())))
        )
      )(bind)(unbind)
    )
  }
}
