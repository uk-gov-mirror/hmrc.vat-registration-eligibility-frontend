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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.collection.immutable.ListMap

class PageIdBindingSpec extends PlaySpec {
  ListMap[String, JsValue](
    "" -> JsString(""),
    s"$ThresholdNextThirtyDaysId" -> JsBoolean(true),
    s"$ThresholdPreviousThirtyDaysId" -> Json.obj("value" -> JsBoolean(false)),
    s"$ThresholdInTwelveMonthsId" -> Json.obj("value" -> JsBoolean(false)),
    s"$VoluntaryRegistrationId" -> JsBoolean(true),
    s"$TurnoverEstimateId" -> Json.obj("selection" -> JsString("oneandtenthousand")),
    s"$CompletionCapacityId" -> JsString("noneOfThese"),
    s"$CompletionCapacityFillingInForId" -> JsString("wellMr"),
    s"$InternationalActivitiesId" -> JsBoolean(false),
    s"$InvolvedInOtherBusinessId" -> JsBoolean(false),
    s"$AnnualAccountingSchemeId" -> JsBoolean(false),
    s"$ZeroRatedSalesId" -> JsBoolean(true),
    s"$VATExemptionId" -> JsBoolean(false),
    s"$VATRegistrationExceptionId" -> JsBoolean(false),
    s"$AgriculturalFlatRateSchemeId" -> JsBoolean(false),
    s"$RacehorsesId" -> JsBoolean(false),
    s"$ApplicantUKNinoId" -> Json.obj(
      "value" -> JsBoolean(true),
      "optionalData" -> JsString("nino-fake-not-real")
    )
  ).foldLeft(Map[String, JsValue]()) {
    case (mockedReturn, currentItem) =>
      s"an exception should be experienced when only pages before ${currentItem._1} have been filled" in {
        intercept[Exception](PageIdBinding.sectionBindings(new CacheMap("testId", mockedReturn)))
      }
      mockedReturn + currentItem
  }
}
