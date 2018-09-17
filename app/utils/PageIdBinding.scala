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

package utils

import identifiers._
import models.ConditionalDateFormElement
import uk.gov.hmrc.http.cache.client.CacheMap

object PageIdBinding {
  def sectionBindings(map: CacheMap) : Map[String, Seq[(Identifier, Option[Any])]] = {
    val userAnswers = new UserAnswers(map)

    def validateVoluntaryReason = (userAnswers.thresholdNextThirtyDays, userAnswers.thresholdPreviousThirtyDays, userAnswers.thresholdInTwelveMonths) match {
      case (Some(false), Some(ConditionalDateFormElement(false, _)), Some(ConditionalDateFormElement(false, _))) =>
        userAnswers.voluntaryRegistration.orElse(throw new Exception("Voluntary Registration Not Answered"))
      case _ => None
    }

    def validateCompletionCapacityFillingInFor = userAnswers.completionCapacity
      .filter(_ == "noneOfThese")
      .flatMap(_ => userAnswers.completionCapacityFillingInFor.orElse(throw new Exception("Completion Capacity Filling In For Not Answered")))

    def validateVatExemption = userAnswers.zeroRatedSales
      .filter(identity)
      .flatMap(_ => userAnswers.vatExemption.orElse(throw new Exception("Vat Exemption Not Answered")))

    Map(
      "VAT-taxable sales" ->
        Seq(
          (ThresholdNextThirtyDaysId, userAnswers.thresholdNextThirtyDays.orElse(throw new Exception("Threshold Next Thirty Days Not Answered"))),
          (ThresholdPreviousThirtyDaysId, userAnswers.thresholdPreviousThirtyDays.orElse(throw new Exception("Threshold Previous Thirty Days Not Answered"))),
          (ThresholdInTwelveMonthsId, userAnswers.thresholdInTwelveMonths.orElse(throw new Exception("Threshold In Twelve Months Not Answered"))),
          (VoluntaryRegistrationId, validateVoluntaryReason),
          (TurnoverEstimateId, userAnswers.turnoverEstimate.orElse(throw new Exception("Turnover Estimates Not Answered")))
        ),
      "Who is doing the application?" ->
        Seq(
          (CompletionCapacityId, userAnswers.completionCapacity.orElse(throw new Exception("Completion Capacity Not Answered"))),
          (CompletionCapacityFillingInForId, validateCompletionCapacityFillingInFor)
        ),
      "Special situations" ->
        Seq(
          (InternationalActivitiesId, userAnswers.internationalActivities.orElse(throw new Exception("International Activities Not Answered"))),
          (InvolvedInOtherBusinessId, userAnswers.involvedInOtherBusiness.orElse(throw new Exception("Involved In Other Business Not Answered"))),
          (AnnualAccountingSchemeId, userAnswers.annualAccountingScheme.orElse(throw new Exception("Annual Accounting Scheme Not Answered"))),
          (ZeroRatedSalesId, userAnswers.zeroRatedSales.orElse(throw new Exception("Zero Rated Sales Not Answered"))),
          (VATExemptionId, validateVatExemption),
          (VATRegistrationExceptionId, userAnswers.vatRegistrationException.orElse(throw new Exception("Vat Registration Exception Not Answered"))),
          (AgriculturalFlatRateSchemeId, userAnswers.agriculturalFlatRateScheme.orElse(throw new Exception("Agricultural Flat Rate Scheme Not Answered"))),
          (RacehorsesId, userAnswers.racehorses.orElse(throw new Exception("Racehorses Not Answered"))),
          (ApplicantUKNinoId, userAnswers.applicantUKNino.orElse(throw new Exception("Applicant UK Nino Not Answered")))
        )
    )
  }
}
