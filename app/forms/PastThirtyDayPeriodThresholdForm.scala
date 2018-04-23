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

import models.view.ThresholdView
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}
import forms.FormValidation.Dates._
import forms.FormValidation._
import models.DayMonthYearModel
import models.MonthYearModel.FORMAT_DD_MMMM_Y

import scala.util.Try

object PastThirtyDayPeriodThresholdForm {
  val RADIO_YES_NO = "pastThirtyDayPeriodRadio"

  def bind(selection: Boolean, dateModel: Option[DayMonthYearModel]): ThresholdView =
    ThresholdView(selection, dateModel.fold[Option[LocalDate]](None)(_.toLocalDate))

  def unbind(expect: ThresholdView): Option[(Boolean, Option[DayMonthYearModel])] =
    Try {
      expect.date.fold((expect.selection, Option.empty[DayMonthYearModel])) {
        d => (expect.selection, Some(DayMonthYearModel.fromLocalDate(d)))
      }
    }.toOption

  def form(dateOfIncorporation: LocalDate): Form[ThresholdView] = {
    implicit val specificErrorCode: String = "pastThirtyDayPeriod.date"

    Form(
      mapping(
        RADIO_YES_NO -> missingBooleanFieldMappingArgs()(Seq(dateOfIncorporation.format(FORMAT_DD_MMMM_Y)))("pastThirtyDayPeriod.selection"),
        "pastThirtyDayPeriod" -> mandatoryIf(
          isEqual(RADIO_YES_NO, "true"),
          mapping(
            "day" -> text,
            "month" -> text,
            "year" -> text
          )(DayMonthYearModel.apply)(DayMonthYearModel.unapply).verifying(
            nonEmptyDayMonthYearModel(validPartialDayMonthYearModel(incorporationDateValidation(dateOfIncorporation)())))
        )
      )(bind)(unbind)
    )
  }
}

