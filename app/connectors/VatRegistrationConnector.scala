/*
 * Copyright 2019 HM Revenue & Customs
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

import config.{FrontendAppConfig, WSHttp}
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class VatRegistrationConnectorImpl @Inject()(val http : WSHttp,
                                             val servicesConfig: FrontendAppConfig) extends VatRegistrationConnector {
  override val vatRegistrationUrl: String = servicesConfig.baseUrl("vat-registration")
  override val vatRegistrationUri: String =
    servicesConfig.getConfString("vat-registration.uri", throw new RuntimeException("expected incorporation-information.uri in config but none found"))
}

trait VatRegistrationConnector {
  val http : CoreGet with CorePost with WSHttp
  val vatRegistrationUrl : String
  val vatRegistrationUri : String

  def saveEligibility(regId: String, eligibility : JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {
    http.PATCH[JsValue, HttpResponse](s"$vatRegistrationUrl$vatRegistrationUri/$regId/eligibility-data", eligibility).map(_.json) recover {
      case e: NotFoundException => Logger.error(s"[VatRegistrationConnector][saveEligibility] No vat registration found for regId: $regId")
        throw e
      case e => Logger.error(s"[VatRegistrationConnector][saveEligibility] an error occurred for regId: $regId with exception: ${e.getMessage}")
        throw e
    }
  }
}