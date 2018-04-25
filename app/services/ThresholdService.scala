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

import java.time.LocalDate
import javax.inject.Inject

import common.enums.CacheKeys
import connectors.{S4LConnector, VatRegistrationConnector}
import models.CurrentProfile
import models.view._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class ThresholdServiceImpl @Inject()(val s4LConnector: S4LConnector,
                                     val vatRegistrationConnector: VatRegistrationConnector) extends ThresholdService {
  override def now: LocalDate = LocalDate.now()
}

trait ThresholdService {
  val s4LConnector: S4LConnector
  val vatRegistrationConnector: VatRegistrationConnector

  def now: LocalDate

  private def updateThreshold(data: Threshold)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    vatRegistrationConnector.patchThreshold(data) flatMap { _ =>
      s4LConnector.clear(cp.registrationId) map {
        _ => data
      }
    }
  }

  private def updateVoluntaryInfo(threshold: Threshold): Threshold = threshold match {
    case Threshold(Some(true), _, _, _, _, _) =>
      threshold.copy(voluntaryRegistration = None, voluntaryRegistrationReason = None)
    case Threshold(None,_, _,
      Some(ThresholdView(s1, _)),
      Some(ThresholdView(s2, _)),
      Some(ThresholdView(s3, _))
    ) if s1 || s2 || s3 =>
      threshold.copy(voluntaryRegistration = None, voluntaryRegistrationReason = None)
    case Threshold(_, Some(false), Some(_), _, _, _) =>
      threshold.copy(voluntaryRegistrationReason = None)
    case _ => threshold
  }

  private def thresholdViewsMandatory(threshold : Threshold): Boolean = {
    List[Option[ThresholdView]](
      threshold.overThresholdOccuredTwelveMonth,
      threshold.pastOverThresholdThirtyDays,
      threshold.overThresholdThirtyDays
    ).foldLeft(false)((current, tv) =>
      current || (tv.isDefined && tv.get.selection)
    )
  }

  private[services] def isModelComplete(threshold: Threshold)(implicit cp: CurrentProfile): Completion[Threshold] = {
    (threshold, thresholdViewsMandatory(threshold)) match {
      case (Threshold(
        Some(true),
        None,
        None,
        None,
        None,
        None
      ), _)       => Completed(threshold)
      case (Threshold(
        Some(false),
        Some(false),
        None,
        None,
        None,
        None
      ), _)       => Completed(threshold)
      case (Threshold(
        Some(false),
        Some(true),
        Some(_),
        None,
        None,
        None
      ), _)       => Completed(threshold)
      case (Threshold(
        None,
        Some(true),
        Some(_),
        Some(_),
        _,
        Some(_)
      ), false)   => Completed(threshold)
      case (Threshold(
        None,
        None,
        None,
        Some(_),
        _,
        Some(_)
      ), true)    => Completed(threshold)
      case _      => Incomplete(threshold)
    }
  }

  private[services] def saveThreshold(updatedThreshold: Threshold)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    isModelComplete(updateVoluntaryInfo(updatedThreshold)) match {
      case Incomplete(threshold) => s4LConnector.save(cp.registrationId, CacheKeys.Threshold, threshold).map(_ => threshold)
      case Completed(threshold) => updateThreshold(threshold)
    }
  }

  def saveOverThresholdThirtyDaysPreIncorp(taxableTurnover: Boolean)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(overThresholdThirtyDaysPreIncorp = Some(taxableTurnover)))
    }
  }

  def saveVoluntaryRegistration(voluntaryRegistration: Boolean)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(voluntaryRegistration = Some(voluntaryRegistration)))
    }
  }

  def saveVoluntaryRegistrationReason(reason: String)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(voluntaryRegistrationReason = Some(reason)))
    }
  }

  def saveOverThresholdThirtyDays(overThreshold: Boolean)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(overThresholdThirtyDays = Some(
        ThresholdView(overThreshold, if (overThreshold) Some(LocalDate.now()) else None)))
      )
    }
  }

  def saveOverThresholdPastThirtyDays(pastThreshold: ThresholdView)(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(pastOverThresholdThirtyDays = Some(pastThreshold)))
    }
  }

  def saveOverThresholdSinceIncorp(occuredSince : Boolean)
                                  (implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(overThresholdOccuredTwelveMonth = Some(
        ThresholdView(occuredSince, if (occuredSince) cp.incorporationDate else None)))
      )
    }
  }

  def saveOverThresholdTwelveMonths(overOccuredTv: ThresholdView)
                                   (implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    getThreshold flatMap { storedThreshold =>
      saveThreshold(storedThreshold.copy(overThresholdOccuredTwelveMonth = Some(overOccuredTv)))
    }
  }

  def getThreshold(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[Threshold] = {
    s4LConnector.fetchAndGet[Threshold](cp.registrationId, CacheKeys.Threshold) flatMap {
      case Some(s4l) => Future.successful(s4l)
      case None => vatRegistrationConnector.getThreshold map {
        case Some(threshold) => threshold
        case None => Threshold(None, None, None, None, None)
      }
    }
  }

  def fetchCurrentVatThreshold(implicit hc: HeaderCarrier): Future[String] = {
    vatRegistrationConnector.getVATThreshold(now).map { threshold =>
      delimiterNumericString(threshold)
    }
  }

  private def delimiterNumericString(number: String): String = {
    "%,d".format(number.toInt)
  }
}