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

import config.WSHttp
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.RegistrationWhitelist

import scala.concurrent.Future

class IncorporationInformationConnectorImpl @Inject()(val http: WSHttp, config: ServicesConfig) extends IncorporationInformationConnector {
  val incorpInfoUrl = config.baseUrl("incorporation-information")
  val incorpInfoUri = config.getConfString("incorporation-information.uri", "")
}

trait IncorporationInformationConnector extends RegistrationWhitelist {
  val incorpInfoUrl: String
  val incorpInfoUri: String

  val http: WSHttp

  def getCompanyName(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[JsValue] = ifRegIdNotWhitelisted(regId){
    http.GET[JsValue](s"$incorpInfoUrl$incorpInfoUri/$transactionId/company-profile") recover {
      case notFound: NotFoundException =>
        logger.error(s"[getCompanyName] - Could not find company name for regId $regId (txId: $transactionId)")
        throw notFound
      case e =>
        logger.error(s"[getCompanyName] - There was a problem getting company for regId $regId (txId: $transactionId)", e)
        throw e
    }
  }(returnDefaultCompanyName)

  def getIncorpUpdate(regId : String, transID : String)(implicit hc : HeaderCarrier) : Future[Option[JsValue]] = {
    ifRegIdNotWhitelisted[Option[JsValue]](regId) {
      http.GET[HttpResponse](s"$incorpInfoUrl$incorpInfoUri/$transID/incorporation-update").map(res => res.status match {
        case 200  => Some(res.json)
        case 204  => None
      })
    }(_ => Some(Json.obj()))
  }
}
