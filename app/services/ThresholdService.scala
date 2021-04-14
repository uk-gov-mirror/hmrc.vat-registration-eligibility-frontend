/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.FrontendAppConfig
import connectors.DataCacheConnector
import deprecated.DeprecatedConstants
import identifiers.{ThresholdNextThirtyDaysId, VATRegistrationExceptionId, VoluntaryRegistrationId}
import javax.inject.{Inject, Singleton}
import models.requests.DataRequest
import play.api.i18n.MessagesApi
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{MessagesUtils, VATDateHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ThresholdService @Inject()(val messagesApi: MessagesApi,
                                 val dataCacheConnector: DataCacheConnector,
                                 p: views.html.components.p,
                                 bullets: views.html.components.bullets,
                                 link: views.html.components.link
                                )(implicit val appConfig: FrontendAppConfig) extends MessagesUtils {

  def removeVoluntaryAndNextThirtyDays(implicit request: DataRequest[_]): Future[CacheMap] = {
    dataCacheConnector.removeEntry(request.internalId, VoluntaryRegistrationId.toString).flatMap {
      _ => dataCacheConnector.removeEntry(request.internalId, ThresholdNextThirtyDaysId.toString)
    }
  }

  def removeException(implicit request: DataRequest[_]): Future[CacheMap] = {
    dataCacheConnector.removeEntry(request.internalId, VATRegistrationExceptionId.toString)
  }

  def removeVoluntaryRegistration(implicit request: DataRequest[_]): Future[CacheMap] = {
    dataCacheConnector.removeEntry(request.internalId, VoluntaryRegistrationId.toString)
  }

  sealed trait ThresholdDateResult

  case class notIncorped() extends ThresholdDateResult

  case class limitedIncorpedLessThan12MonthsAgo() extends ThresholdDateResult

  case class limitedIncorpedEqualOrAfter20170401() extends ThresholdDateResult

  case class limitedIncorpedTaxYear2016to2017() extends ThresholdDateResult

  case class limitedIncorpedTaxYear2015to2016() extends ThresholdDateResult

  case class default() extends ThresholdDateResult


  def returnHeadingTwelveMonths(enum: ThresholdDateResult)(implicit r: DataRequest[_]): String = {
    enum match {
      case notIncorped() => messages("threshold.headingNotIncorp")
      case limitedIncorpedLessThan12MonthsAgo() => messages("thresholdInTwelveMonths.headingIncorpLess12m")
      case _ => messages("thresholdInTwelveMonths.headingIncorpMore12m")
    }
  }

  def returnHeadingForTwelveMonthsDateEntry(enum: ThresholdDateResult)(implicit r: DataRequest[_]): String = {
    enum match {
      case _ => messages("thresholdInTwelveMonths.heading2")
    }
  }

  def returnHelpText1TwelveMonths(enum: ThresholdDateResult)(implicit r: DataRequest[_]): String = {
    enum match {
      case notIncorped() => messages("thresholdInTwelveMonths.firstHelpTextNotIncorp")
      case limitedIncorpedLessThan12MonthsAgo() => messages("thresholdInTwelveMonths.firstHelpTextIncorpLess12m")
      case _ => messages("thresholdInTwelveMonths.firstHelpTextIncorpMore12m")
    }
  }


  def returnHeadingPrevious(enum: ThresholdDateResult)(implicit r: DataRequest[_]): String = {
    enum match {
      case (limitedIncorpedEqualOrAfter20170401() | limitedIncorpedLessThan12MonthsAgo()) => messages("thresholdPreviousThirtyDays.headingLtdAndIncorpAfterApr17")
      case _ => messages("thresholdPreviousThirtyDays.heading")
    }
  }

  def returnHelpText1Previous(enum: ThresholdDateResult)(implicit r: DataRequest[_]): Html = {
    enum match {
      case (limitedIncorpedEqualOrAfter20170401() | limitedIncorpedLessThan12MonthsAgo()) => Html("")
      case limitedIncorpedTaxYear2016to2017() => HtmlFormat.fill(collection.immutable.Seq(
        p {
          Html(messages("thresholdPreviousThirtyDays.text"))
        },
        bullets(
          messages("thresholdPreviousThirtyDays.bullet1"),
          messages("thresholdPreviousThirtyDays.bullet2")
        )
      ))
      case limitedIncorpedTaxYear2015to2016() => HtmlFormat.fill(collection.immutable.Seq(
        p {
          Html(messages("thresholdPreviousThirtyDays.text"))
        },
        bullets(
          messages("thresholdPreviousThirtyDays.bullet1"),
          messages("thresholdPreviousThirtyDays.bullet2"),
          messages("thresholdPreviousThirtyDays.bullet3")
        )
      ))
      case _ => HtmlFormat.fill(collection.immutable.Seq(
        p {
          Html(messages("thresholdPreviousThirtyDays.text"))
        },
        bullets(
          messages("thresholdPreviousThirtyDays.bullet1"),
          messages("thresholdPreviousThirtyDays.bullet2"),
          messages("thresholdPreviousThirtyDays.bullet3")
        ),
        p {
          Html(messages("thresholdPreviousThirtyDays.beforeLinkText"))
          link(appConfig.VATNotice700_1supplementURL, messages("thresholdPreviousThirtyDays.linkText"))
          Html(".")
        }
      )
      )
    }
  }

  def returnThresholdDateResult[T](f: ThresholdDateResult => T)(implicit request: DataRequest[_]): T = {
    val res = Option(DeprecatedConstants.fakeIncorpDate) match { //TODO - Option to allow the match to compile, but will never hit the none route
      case None => notIncorped() //TODO - Is this needed still?
      case Some(d) if VATDateHelper.lessThan12Months(d) => limitedIncorpedLessThan12MonthsAgo()
      case Some(d) if VATDateHelper.dateEqualOrAfter201741(d) => limitedIncorpedEqualOrAfter20170401()
      case Some(d) if VATDateHelper.dateBefore201741After2016331(d) => limitedIncorpedTaxYear2016to2017()
      case Some(d) if VATDateHelper.dateBefore201641After2015331(d) => limitedIncorpedTaxYear2015to2016()
      case _ => default()
    }
    f(res)
  }
}