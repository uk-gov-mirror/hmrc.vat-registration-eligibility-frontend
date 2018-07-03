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

package services

import java.time.format.DateTimeFormatter

import connectors.{DataCacheConnector, VatRegistrationConnector}
import identifiers.{CompletionCapacityFillingInForId, CompletionCapacityId, Identifier, VoluntaryRegistrationId}
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{JsonSummaryRow, PageIdBinding, UserAnswers}

import scala.concurrent.{ExecutionContext, Future}

class VatRegistrationServiceImpl @Inject()(val vrConnector : VatRegistrationConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           val messagesApi : MessagesApi,
                                           val iiService : IncorporationInformationService) extends VatRegistrationService {
}

trait VatRegistrationService extends I18nSupport {
  val vrConnector : VatRegistrationConnector
  val dataCacheConnector: DataCacheConnector
  val iiService : IncorporationInformationService
  implicit lazy val messages = messagesApi

  def submitEligibility(internalId : String, regId: String, txId: String)(implicit hc : HeaderCarrier, ec : ExecutionContext): Future[JsObject] = {
    for {
      officers          <- iiService.getOfficerList(txId)
      block             <- createEligibilityBlock(internalId, officers)
      _                 <- vrConnector.saveEligibility(regId, block)
    } yield {
      block
    }
  }

  private[services] def prepareQuestionData(key : String, data: Boolean) = {
    JsonSummaryRow(key, messagesApi(s"$key.heading"), messagesApi(s"site.${if (data) "yes" else "no"}"), Json.toJson(data))
  }

  private[services] def getVoluntaryRegistrationJson(data: Boolean) = {
    val key = VoluntaryRegistrationId.toString
    JsonSummaryRow(key, messagesApi(s"$key.summary.heading"), messagesApi(s"site.${if (data) "yes" else "no"}"), Json.toJson(data))
  }

  private[services] def prepareQuestionData(key : String, data: String) = {
    JsonSummaryRow(key, messagesApi(s"$key.heading"), data, Json.toJson(data))
  }

  private[services] def prepareQuestionData(key: String, data: ConditionalDateFormElement) = {
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val value = JsonSummaryRow(s"$key-value", messagesApi(s"$key.heading"), messagesApi(if (data.value) s"site.yes" else "site.no"), Json.toJson(data.value))
    val dataObj = data.optionalData.map(date => JsonSummaryRow(s"$key-optionalData", messagesApi(s"$key.heading2"), date.format(formatter), Json.toJson(date)))

    dataObj.foldLeft(value)((old, list) => old ++ list)
  }

  private[services] def prepareQuestionData(key: String, data: TurnoverEstimateFormElement) = {
    val value = JsonSummaryRow(s"$key-value", messagesApi(s"$key.heading"), messagesApi(s"turnoverEstimate.${data.value}"), Json.toJson(data.value))
    val dataObj = data.optionalData
      .map(turnover => JsonSummaryRow(s"$key-optionalData", messagesApi(s"$key.giveAnEstimate"), s"£${"%,d".format(turnover.toLong)}", JsNumber(BigDecimal(turnover.toLong))))

    dataObj.foldLeft(value)((oList, dList) => oList ++ dList)
  }

  private[services] def prepareQuestionData(key: String, data: ConditionalNinoFormElement, officers: Seq[Officer], onBehalfOf: Option[String]) = {
    val heading = onBehalfOf.fold(messagesApi(s"$key.heading")){
      id =>
        val officer = officers.find(_.generateId == id).getOrElse(throw new Exception("director not present"))
        messagesApi(s"$key.heading.onBehalfOf", officer.shortName)
    }
    val value = JsonSummaryRow(s"$key-value", heading, messagesApi(if (data.value) s"site.yes" else "site.no"), Json.toJson(data.value))
    val dataObj = data.optionalData.map(nino => JsonSummaryRow(s"$key-optionalData", messagesApi(s"$key.heading2"), nino, Json.toJson(nino)))

    dataObj.foldLeft(value)((oList, dList) => oList ++ dList)
  }

  private[services] def getCompletionCapacityFillingInForJson(data: String, officers: Seq[Officer]) = {
    val key = CompletionCapacityFillingInForId.toString
    val officer = officers.find(_.generateId == data).getOrElse(throw new Exception("selected officer not present"))
    JsonSummaryRow(key, messagesApi(s"$key.heading"), officer.shortName, Json.toJson(officer))
  }

  private[services] def getCompletionCapacityJson(data: String, officers: Seq[Officer]) = {
    val key = CompletionCapacityId.toString
    officers match {
      case officer :: Nil =>
        val (answer, answerValue) = if (data == "noneOfThese") {
          (messagesApi("site.no"), Json.toJson(data))
        } else {
          (messagesApi("site.yes"), Json.toJson(officer))
        }
        JsonSummaryRow(key, messagesApi(s"$key.heading.single", officer.shortName), answer, answerValue)
      case multipleOfficers =>
        val (answer, answerValue) = if (data == "noneOfThese") {
          (messagesApi("completionCapacity.noneOfThese"), Json.toJson(data))
        } else {
          val officer = multipleOfficers.find(_.generateId == data).getOrElse(throw new Exception("director not present"))
          (officer.shortName, Json.toJson(officer))
        }
        JsonSummaryRow(key, messagesApi(s"$key.heading"), answer, answerValue)
    }
  }

  private def buildIndividualQuestion(officers: Seq[Officer], onBehalfOf: Option[String]): PartialFunction[(Identifier, Any), List[JsValue]] = {
    case (id, e: ConditionalDateFormElement) => prepareQuestionData(id.toString, e)
    case (id, e: ConditionalNinoFormElement) => prepareQuestionData(id.toString, e, officers, onBehalfOf)
    case (id, e: TurnoverEstimateFormElement) => prepareQuestionData(id.toString, e)
    case (CompletionCapacityId, e: String) => getCompletionCapacityJson(e, officers)
    case (CompletionCapacityFillingInForId, e: String) => getCompletionCapacityFillingInForJson(e, officers)
    case (VoluntaryRegistrationId, e: Boolean) => getVoluntaryRegistrationJson(e)
    case (id, e: Boolean) => prepareQuestionData(id.toString, e)
    case (id, e: String) => prepareQuestionData(id.toString, e)
  }

  private def getEligibilitySections(cacheMap: CacheMap, officers: Seq[Officer]) = PageIdBinding.sectionBindings(cacheMap) map {
    case (sectionTitle, questionIds) => Json.obj(
      "title" -> sectionTitle,
      "data"  -> (questionIds flatMap {
        case (questionId, userAnswer) =>
          userAnswer.fold(List[JsValue]())(
            answer => buildIndividualQuestion(officers, new UserAnswers(cacheMap).completionCapacityFillingInFor)((questionId, answer))
          )
      })
    )
  }

  private def createEligibilityBlock(internalId : String, officer: Seq[Officer])(implicit hc : HeaderCarrier, executionContext: ExecutionContext): Future[JsObject] = {
    dataCacheConnector.fetch(internalId) map {
      case Some(map)  => Json.obj("sections" -> getEligibilitySections(map, officer))
      case _          => throw new RuntimeException
    }
  }
}
