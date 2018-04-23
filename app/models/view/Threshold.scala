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

package models.view

import java.time.LocalDate

import play.api.libs.json._

case class Threshold(
                      overThresholdThirtyDaysPreIncorp: Option[Boolean] = None,
                      voluntaryRegistration: Option[Boolean] = None,
                      voluntaryRegistrationReason: Option[String] = None,
                      overThresholdOccuredTwelveMonth: Option[ThresholdView] = None,
                      pastOverThresholdThirtyDays : Option[ThresholdView] = None,
                      overThresholdThirtyDays: Option[ThresholdView] = None
                    )


object Threshold{

  implicit val format: OFormat[Threshold] = Json.format[Threshold]

  def apiReads(incorpDate : Option[LocalDate]): Reads[Threshold] = new Reads[Threshold] {
    override def reads(json: JsValue): JsResult[Threshold] = {
      val mandatoryRegistration = (json \ "mandatoryRegistration").as[Boolean]
      val voluntaryReason = (json \ "voluntaryReason").asOpt[String]
      val apiTwelveMonth = (json \ "overThresholdOccuredTwelveMonth").asOpt[LocalDate]
      val apiPastThirtyDays = (json \ "pastOverThresholdDateThirtyDays").asOpt[LocalDate]
      val apiNextThirtyDays = (json \ "overThresholdDateThirtyDays").asOpt[LocalDate]

      val voluntary = if (mandatoryRegistration) None else Some(voluntaryReason.nonEmpty)

      implicit def dateToView(date : Option[LocalDate]): Option[ThresholdView] = {
        Some(ThresholdView(date.isDefined, date))
      }

      incorpDate match {
        case Some(_) =>
          JsSuccess(Threshold(None, voluntary, voluntaryReason, apiTwelveMonth, apiPastThirtyDays, apiNextThirtyDays))
        case None   =>
          JsSuccess(Threshold(Some(mandatoryRegistration), voluntary, voluntaryReason, None, None))
      }
    }
  }

  val apiWrites: Writes[Threshold] = new Writes[Threshold] {
    override def writes(threshold: Threshold): JsValue = {
      def purgeNull(jsObj : JsObject) : JsObject =
        JsObject(jsObj.value.filterNot {
          case (_, value) => value == JsNull
        })

      implicit def viewToBool(thresholdView: Option[ThresholdView]) : Boolean = thresholdView match {
        case Some(sel) => sel.selection
        case _         => false
      }

      val isMandatory: Boolean = List[Boolean](
        threshold.overThresholdThirtyDaysPreIncorp.contains(true),
        threshold.overThresholdOccuredTwelveMonth,
        threshold.pastOverThresholdThirtyDays,
        threshold.overThresholdThirtyDays
      ).contains(true)

      purgeNull(Json.obj(
        "mandatoryRegistration" -> isMandatory,
        "voluntaryReason" -> (if (isMandatory) None else threshold.voluntaryRegistrationReason),
        "overThresholdOccuredTwelveMonth" -> threshold.overThresholdOccuredTwelveMonth.filter(_.selection == true).flatMap(_.date),
        "pastOverThresholdDateThirtyDays" -> threshold.pastOverThresholdThirtyDays.filter(_.selection == true).flatMap(_.date),
        "overThresholdDateThirtyDays" -> threshold.overThresholdThirtyDays.filter(_.selection == true).flatMap(_.date)
      ))
    }
  }
}
