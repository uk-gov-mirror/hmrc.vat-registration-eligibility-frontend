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

package forms

import java.time.LocalDate

import forms.FormValidation.Dates.{nonEmptyMonthYearModel, validPartialMonthYearModel}
import forms.FormValidation.{inRangeWithArgs, missingBooleanFieldMappingArgs}
import models.MonthYearModel
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.OverThresholdView
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

object OverThresholdFormFactory {

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x.compareTo(y)
  }

  val RADIO_YES_NO = "overThresholdRadio"

  def form(dateOfIncorporation: LocalDate): Form[OverThresholdView] = {

    val minDate: LocalDate = dateOfIncorporation
    val maxDate: LocalDate = LocalDate.now()
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
            nonEmptyMonthYearModel(validPartialMonthYearModel(inRangeWithArgs(minDate, maxDate)(Seq(dateOfIncorporation.format(FORMAT_DD_MMMM_Y))))))
        )
      )(OverThresholdView.bind)(OverThresholdView.unbind)
    )
  }
}
