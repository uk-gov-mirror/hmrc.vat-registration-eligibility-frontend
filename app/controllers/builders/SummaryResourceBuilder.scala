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

package controllers.builders


import models.api.VatServiceEligibility
import models.view.{SummaryRow, SummarySection}

case class SummaryResourceBuilder(vatServiceEligibility: VatServiceEligibility)
  extends SummarySectionBuilder {

  override val sectionId: String = "resources"

  val companyOwnRow: SummaryRow = SummaryRow(
    s"$sectionId.companyOwn",
    vatServiceEligibility.companyWillDoAnyOf.map {
      case true => "app.common.yes"
      case false => "app.common.no"
    }.getOrElse(""),
    Some(controllers.routes.EligibilityController.showCompanyWillDoAnyOf())
  )

  val companySellRow: SummaryRow = SummaryRow(
    s"$sectionId.companySell",
    vatServiceEligibility.companyWillDoAnyOf.map {
      case true => "app.common.yes"
      case false => "app.common.no"
    }.getOrElse(""),
    Some(controllers.routes.EligibilityController.showCompanyWillDoAnyOf())
  )

  val section: SummarySection =
    SummarySection(
      sectionId,
      rows = Seq(
        (companyOwnRow, true),
        (companySellRow, true)
      )
    )

}
