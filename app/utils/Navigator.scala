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

import controllers.routes
import identifiers._
import javax.inject.{Inject, Singleton}
import models.{ConditionalDateFormElement, ConditionalNinoFormElement, Mode}
import play.api.libs.json.Reads
import play.api.mvc.Call
import utils.DefaultImplicitJsonReads.{BooleanReads, StringReads}

@Singleton
class Navigator @Inject()() {

  private[utils] def pageIdToPageLoad(pageId: Identifier): Call = pageId match {
    case ThresholdNextThirtyDaysId => routes.ThresholdNextThirtyDaysController.onPageLoad()
    case ThresholdPreviousThirtyDaysId => routes.ThresholdPreviousThirtyDaysController.onPageLoad()
    case VoluntaryRegistrationId => routes.VoluntaryRegistrationController.onPageLoad()
    case ChoseNotToRegisterId => routes.ChoseNotToRegisterController.onPageLoad()
    case ThresholdInTwelveMonthsId => routes.ThresholdInTwelveMonthsController.onPageLoad()
    case TurnoverEstimateId => routes.TurnoverEstimateController.onPageLoad()
    case CompletionCapacityId => routes.CompletionCapacityController.onPageLoad()
    case CompletionCapacityFillingInForId => routes.CompletionCapacityFillingInForController.onPageLoad()
    case InvolvedInOtherBusinessId => routes.InvolvedInOtherBusinessController.onPageLoad()
    case InternationalActivitiesId => routes.InternationalActivitiesController.onPageLoad()
    case AnnualAccountingSchemeId => routes.AnnualAccountingSchemeController.onPageLoad()
    case ZeroRatedSalesId => routes.ZeroRatedSalesController.onPageLoad()
    case VATExemptionId => routes.VATExemptionController.onPageLoad()
    case VATRegistrationExceptionId => routes.VATRegistrationExceptionController.onPageLoad()
    case ApplyInWritingId => routes.ApplyInWritingController.onPageLoad()
    case EligibilityDropoutId(mode) => routes.EligibilityDropoutController.onPageLoad(mode)
    case AgriculturalFlatRateSchemeId => routes.AgriculturalFlatRateSchemeController.onPageLoad()
    case RacehorsesId => routes.RacehorsesController.onPageLoad()
    case ApplicantUKNinoId => routes.ApplicantUKNinoController.onPageLoad()
    case _ => throw new RuntimeException(s"[Navigator] [pageIdToPageLoad] Could not load page for pageId: $pageId")
  }

  private[utils] def nextOn[T](condition: T, fromPage: Identifier, onSuccessPage: Identifier, onFailPage: Identifier)
                              (implicit reads: Reads[T]): (Identifier, UserAnswers => Call) ={
    fromPage -> {
      _.getAnswer[T](fromPage) match {
        case Some(`condition`) => pageIdToPageLoad(onSuccessPage)
        case _ => pageIdToPageLoad(onFailPage)
      }
    }
  }

  private[utils] def nextOnConditionalFormElement(condition: Boolean, fromPage: Identifier, onSuccessPage: Identifier, onFailPage: Identifier):
  (Identifier, UserAnswers => Call) ={
    fromPage -> {
      _.thresholdInTwelveMonths match {
        case Some(ConditionalDateFormElement(`condition`, _)) => pageIdToPageLoad(onSuccessPage)
        case _ => pageIdToPageLoad(onFailPage)
      }
    }
  }

  private[utils] def nextOnNino(fromPage: Identifier, onSuccessPage: Identifier, onFailPage: Identifier):
  (Identifier, UserAnswers => Call) ={
    fromPage -> {
      _.applicantUKNino match {
        case Some(ConditionalNinoFormElement(true, _)) => pageIdToPageLoad(onSuccessPage)
        case _ => pageIdToPageLoad(onFailPage)
      }
    }
  }

  private[utils] def navigateFromThresholdInTwelveMonths: (Identifier, UserAnswers => Call) = {
    ThresholdInTwelveMonthsId -> { answers =>
      (answers.thresholdNextThirtyDays, answers.thresholdPreviousThirtyDays, answers.thresholdInTwelveMonths) match {
        case (Some(true), _, _) | (_ , Some(ConditionalDateFormElement(true, _)), _) | (_, _, Some(ConditionalDateFormElement(true, _))) =>
          pageIdToPageLoad(TurnoverEstimateId)
        case _ => pageIdToPageLoad(VoluntaryRegistrationId)
      }
    }
  }

  private[utils] def toNextPage(fromPage: Identifier, toPage: Identifier): (Identifier, UserAnswers => Call) = fromPage -> {
    _ => pageIdToPageLoad(toPage)
  }

  private val routeMap: Map[Identifier, UserAnswers => Call] = Map(
    nextOn(false, ThresholdNextThirtyDaysId, ThresholdPreviousThirtyDaysId, ThresholdPreviousThirtyDaysId),
    toNextPage(ThresholdPreviousThirtyDaysId, ThresholdInTwelveMonthsId),
    navigateFromThresholdInTwelveMonths,
    nextOn(true, VoluntaryRegistrationId, TurnoverEstimateId, ChoseNotToRegisterId),
    toNextPage(TurnoverEstimateId, CompletionCapacityId),
    nextOn("noneOfThese", CompletionCapacityId, CompletionCapacityFillingInForId, InvolvedInOtherBusinessId),
    toNextPage(CompletionCapacityFillingInForId, InvolvedInOtherBusinessId),
    nextOn(false, InvolvedInOtherBusinessId, InternationalActivitiesId, EligibilityDropoutId(InvolvedInOtherBusinessId.toString)),
    nextOn(false, InternationalActivitiesId, AnnualAccountingSchemeId, EligibilityDropoutId(InternationalActivitiesId.toString)),
    nextOn(false, AnnualAccountingSchemeId, ZeroRatedSalesId, EligibilityDropoutId(AnnualAccountingSchemeId.toString)),
    nextOn(true, ZeroRatedSalesId, VATExemptionId, VATRegistrationExceptionId),
    nextOn(false, VATExemptionId, VATRegistrationExceptionId, ApplyInWritingId),
    nextOn(false, VATRegistrationExceptionId, AgriculturalFlatRateSchemeId, EligibilityDropoutId(VATRegistrationExceptionId.toString)),
    nextOn(false, AgriculturalFlatRateSchemeId, RacehorsesId, EligibilityDropoutId(AgriculturalFlatRateSchemeId.toString)),
    nextOn(false, RacehorsesId, ApplicantUKNinoId, EligibilityDropoutId(RacehorsesId.toString)),
    toNextPage(ApplicantUKNinoId, EligibilityDropoutId(ApplicantUKNinoId.toString))
  )

  def nextPage(id: Identifier, mode: Mode): UserAnswers => Call =
    routeMap.getOrElse(id, _ => routes.ThresholdNextThirtyDaysController.onPageLoad())
}
