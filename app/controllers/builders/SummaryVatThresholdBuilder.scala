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

package controllers.builders

import java.time.format.DateTimeFormatter

import models.view.{SummaryRow, SummarySection, Threshold}

case class SummaryVatThresholdBuilder(t: Threshold) extends SummarySectionBuilder {
  override val sectionId: String = "threshold"
  val monthYearPresentationFormatter =  DateTimeFormatter.ofPattern("MMMM y")

  val overThresholdSelectionRow: SummaryRow = SummaryRow(
    s"$sectionId.overThresholdSelection",
    t.overThresholdOccuredTwelveMonth.fold("")(o => if (o.selection) "app.common.yes" else "app.common.no"),
    Some(controllers.routes.ThresholdController.goneOverTwelveShow())
  )

  val overThresholdDateRow: SummaryRow = SummaryRow(
    s"$sectionId.overThresholdDate",
    t.overThresholdOccuredTwelveMonth.flatMap(_.date).fold("")(d => d.format(monthYearPresentationFormatter)),
    Some(controllers.routes.ThresholdController.goneOverTwelveShow())
  )

  val dayMonthYearPresentationFormatter = DateTimeFormatter.ofPattern("dd MMMM y")

  val pastOverThresholdSelectionRow: SummaryRow = SummaryRow(
    s"$sectionId.expectationOverThresholdSelection",
    t.pastOverThresholdThirtyDays.fold("")(o => if (o.selection) "app.common.yes" else "app.common.no"),
    Some(controllers.routes.ThresholdController.pastThirtyDaysShow())
  )

  val pastOverThresholdDateRow: SummaryRow = SummaryRow(
    s"$sectionId.expectationOverThresholdDate",
    t.pastOverThresholdThirtyDays.flatMap(_.date).fold("")(d => d.format(dayMonthYearPresentationFormatter)),
    Some(controllers.routes.ThresholdController.pastThirtyDaysShow())
  )

  val overThresholdThirtySelectionRow: SummaryRow = SummaryRow(
    s"$sectionId.overThresholdThirtySelection",
    t.overThresholdThirtyDays.fold("")(o => if (o.selection) "app.common.yes" else "app.common.no"),
    Some(controllers.routes.ThresholdController.overThresholdThirtyShow())
  )

  val section: SummarySection = SummarySection(
    sectionId,
    rows = Seq(
      (overThresholdThirtySelectionRow, true),
      (pastOverThresholdSelectionRow, true),
      (pastOverThresholdDateRow, t.pastOverThresholdThirtyDays.flatMap(_.date).isDefined),
      (overThresholdSelectionRow, true),
      (overThresholdDateRow, t.overThresholdOccuredTwelveMonth.flatMap(_.date).isDefined)
    )
  )
}
