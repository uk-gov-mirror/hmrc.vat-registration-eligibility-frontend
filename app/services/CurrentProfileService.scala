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

package services

import connectors.{BusinessRegistrationConnector, CompanyRegistrationConnector, DataCacheConnector}
import javax.inject.Inject
import models.CurrentProfile
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class CurrentProfileServiceImpl @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val incorporationInformationService: IncorporationInformationService,
                                           val companyRegistrationConnector: CompanyRegistrationConnector,
                                           val businessRegistrationConnector: BusinessRegistrationConnector) extends CurrentProfileService {

}

trait CurrentProfileService {
  val dataCacheConnector: DataCacheConnector
  val incorporationInformationService: IncorporationInformationService
  val companyRegistrationConnector: CompanyRegistrationConnector
  val businessRegistrationConnector: BusinessRegistrationConnector

  private def constructCurrentProfile(internalID : String)(implicit headerCarrier: HeaderCarrier): Future[CurrentProfile] = for {
    regId           <- businessRegistrationConnector.getBusinessRegistrationId
    transId         <- companyRegistrationConnector.getTransactionId(regId)
    incorpDate      <- incorporationInformationService.getIncorpDate(transId)
    companyName     <- incorporationInformationService.getCompanyName(transId)
    currentProfile  = CurrentProfile(regId, transId, incorpDate, companyName)
    _               <- dataCacheConnector.save(internalID, "CurrentProfile", currentProfile)
  } yield currentProfile

  def fetchOrBuildCurrentProfile(internalID : String)(implicit headerCarrier: HeaderCarrier): Future[CurrentProfile] = {
    dataCacheConnector.getEntry[CurrentProfile](internalID, "CurrentProfile") flatMap {
      case Some(currentProfile) => Future.successful(currentProfile)
      case _                    => constructCurrentProfile(internalID)
    }
  }
}
