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

package connectors

import javax.inject.Singleton

import config.WSHttp
import play.api.Logger
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, NotFoundException}

@Singleton
class IncorporationInformationConnector extends ServicesConfig {
  val incorpInfoUrl = baseUrl("incorporation-information")
  val incorpInfoUri = getConfString("incorporation-information.uri", "")
  val http: CoreGet = WSHttp

  val className = this.getClass.getSimpleName

  def getCompanyName(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    http.GET[JsValue](s"$incorpInfoUrl$incorpInfoUri/$transactionId/company-profile") recover {
      case notFound: NotFoundException =>
        Logger.error(s"[IncorporationInformationConnector] - [getCompanyName] - Could not find company name for regId $regId (txId: $transactionId)")
        throw notFound
      case e =>
        Logger.error(s"[IncorporationInformationConnector] - [getCompanyName] - There was a problem getting company for regId $regId (txId: $transactionId)", e)
        throw e
    }
  }
}
