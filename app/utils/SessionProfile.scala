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

import java.time.LocalDate

import models.CurrentProfile
import play.api.mvc.{Request, Result}
import services.CurrentProfileService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait SessionProfile {
  val currentProfileService: CurrentProfileService

  def withCurrentProfile(f: CurrentProfile => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    currentProfileService.getCurrentProfile flatMap f
  }

  val missingDateText = "Date of Incorporation data expected to be found in Incorporation"
  val incorpDateNotWithin12Months = "Incorporation date not within last 12 months"

  def hasIncorpDate(f: LocalDate => Future[Result])(implicit cp: CurrentProfile): Future[Result] =
    cp.incorporationDate.fold(throw new IllegalStateException(missingDateText))(f)

  def hasIncorpDateWithinTwelveMonths(f: LocalDate => Future[Result])(implicit cp: CurrentProfile): Future[Result] =
    cp.incorporationDate match {
      case Some(id) if id.isAfter(LocalDate.now().minusYears(1)) => f(id)
      case Some(_)                                               => throw new IllegalStateException(incorpDateNotWithin12Months)
      case _                                                     => throw new IllegalStateException(missingDateText)
    }
}