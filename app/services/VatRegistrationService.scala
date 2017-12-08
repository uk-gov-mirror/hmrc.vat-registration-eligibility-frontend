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

package services

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.{KeystoreConnector, VatRegistrationConnector}
import models._
import models.api.{VatServiceEligibility, _}
import models.external.IncorporationInfo
import common.enums.CacheKeys.CurrentProfile
import common.enums.VatRegStatus
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class VatRegistrationService @Inject()(val s4LService: S4LService,
                                       val keystoreConnector: KeystoreConnector,
                                       val vatRegConnector: VatRegistrationConnector) {

  def getVatScheme()(implicit profile: CurrentProfile, hc: HeaderCarrier): Future[VatScheme] =
    vatRegConnector.getRegistration(profile.registrationId)

  def deleteVatScheme()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[Unit] =
    vatRegConnector.deleteVatScheme(profile.registrationId)

  def submitEligibility(eligibility: VatServiceEligibility)(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[VatServiceEligibility] =
    vatRegConnector.upsertVatEligibility(profile.registrationId, eligibility)

  def getIncorporationInfo(txId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[IncorporationInfo]] =
    vatRegConnector.getIncorporationInfo(txId).value

  def getIncorporationDate(txId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[LocalDate]] =
    keystoreConnector.fetchAndGet[CurrentProfile](CurrentProfile.toString) flatMap { profile =>
      profile
        .getOrElse(throw new IllegalStateException("Current Profile expected to be found"))
        .incorporationDate match {
        case None => for {
          incorpDate <- OptionT(getIncorporationInfo(txId)).subflatMap(_.statusEvent.incorporationDate).value
          _ <- keystoreConnector.cache[CurrentProfile](CurrentProfile.toString, profile.get.copy(incorporationDate = incorpDate))
        } yield incorpDate
        case o@_ => Future.successful(o)
      }
    }

  def getStatus(regId: String)(implicit hc: HeaderCarrier): Future[VatRegStatus.Value] =
    vatRegConnector.getStatus(regId)
}
