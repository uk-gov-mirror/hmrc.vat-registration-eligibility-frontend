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

package connectors

import javax.inject.Inject

import common.enums.VatRegStatus
import config.WSHttp
import models.CurrentProfile
import models.external.IncorporationInfo
import models.view.{Eligibility, Threshold}
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.InternalExceptions.VatFootprintNotFound
import utils.RegistrationWhitelist

import scala.concurrent.Future

class VatRegistrationConnectorImpl @Inject()(val http: WSHttp, config: ServicesConfig) extends VatRegistrationConnector {
  val vatRegUrl = config.baseUrl("vat-registration")
}

trait VatRegistrationConnector extends RegistrationWhitelist {

  val vatRegUrl: String
  val http: WSHttp

  def getEligibility(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[Option[(String, Int)]] = {
    http.GET[HttpResponse](s"$vatRegUrl/vatreg/${currentProfile.registrationId}/eligibility") map { res =>
      if (res.status == NO_CONTENT) None else Some(Json.fromJson(res.json)(Eligibility.apiReads).get)
    } recover {
      case e: Exception => throw logResponse(e, "getEligibility")
    }
  }

  def getThreshold(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[Option[JsValue]] = {
    http.GET[HttpResponse](s"$vatRegUrl/vatreg/${currentProfile.registrationId}/threshold").map { res =>
      if (res.status == NO_CONTENT) None else Some(res.json)
    } recover {
      case e: Exception => throw logResponse(e,"getThreshold")
    }
  }

  def patchEligibility(result: String, version: Int)(implicit currentProfile: CurrentProfile, headerCarrier: HeaderCarrier): Future[JsValue] = {
    val json = Json.toJson((result, version))(Eligibility.apiWrites)
    http.PATCH[JsValue, JsValue](s"$vatRegUrl/vatreg/${currentProfile.registrationId}/eligibility", json) map { _ =>
      json
    } recover {
      case e: Exception => throw logResponse(e, "patchEligibility")
    }
  }

  def patchThreshold(threshold: Threshold)(implicit currentProfile: CurrentProfile, headerCarrier: HeaderCarrier): Future[JsValue] = {
    val json = Json.toJson(threshold)(Threshold.apiWrites)
    http.PATCH[JsValue, JsValue](s"$vatRegUrl/vatreg/${currentProfile.registrationId}/threshold", json) map { _ =>
      json
    } recover {
        case e: Exception => throw logResponse(e, "patchThreshold")
    }
  }

  def getIncorporationInfo(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[Option[IncorporationInfo]] = ifRegIdNotWhitelisted(regId){
    http.GET[IncorporationInfo](s"$vatRegUrl/vatreg/incorporation-information/$transactionId").map(Some(_)).recover {
      case _ => None
    }
  }(returnDefaultIncorpInfo)

  def getStatus(regId: String)(implicit hc: HeaderCarrier): Future[VatRegStatus.Value] = {
    http.GET[JsObject](s"$vatRegUrl/vatreg/$regId/status") map { json =>
      (json \ "status").as[VatRegStatus.Value]
    } recover {
      case e: NotFoundException =>
        logResponse(e, "getStatus")
        throw new VatFootprintNotFound(s"[CurrentProfileService] [getRegIdAndStatus] Could not find a vat footprint for regId: $regId")
      case e: Exception => throw logResponse(e, "getStatus")
    }
  }
}

