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

case class Threshold(taxableTurnover: Option[Boolean] = None,
                     voluntaryRegistration: Option[Boolean] = None,
                     voluntaryRegistrationReason: Option[String] = None,
                     overThreshold: Option[OverThresholdView] = None,
                     expectationOverThreshold: Option[ExpectationOverThresholdView] = None)

object Threshold{

  implicit val format: OFormat[Threshold] = Json.format[Threshold]

  def apiReads(incorpDate : Option[LocalDate]): Reads[Threshold] = new Reads[Threshold] {
    override def reads(json: JsValue): JsResult[Threshold] = {
      val mandatoryRegistration = (json \ "mandatoryRegistration").as[Boolean]
      val voluntaryReason = (json \ "voluntaryReason").asOpt[String]
      val overThresholdDate = (json \ "overThresholdDate").asOpt[LocalDate]
      val expectedOverThresholdDate = (json \ "expectedOverThresholdDate").asOpt[LocalDate]
      //another one here for story

      val voluntary = if (mandatoryRegistration) None else Some(voluntaryReason.nonEmpty)

      incorpDate match {
        case Some(_) =>
          val overThresholdView = Some(OverThresholdView(overThresholdDate.isDefined, overThresholdDate))
          val expectedOverThresholdView = Some(ExpectationOverThresholdView(expectedOverThresholdDate.isDefined, expectedOverThresholdDate))
          JsSuccess(Threshold(None, voluntary, voluntaryReason, overThresholdView, expectedOverThresholdView))
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

      val isMandatory: Boolean = List(
        threshold.taxableTurnover.fold(false)(identity),
        threshold.overThreshold.fold(false)(od => od.selection),
        threshold.expectationOverThreshold.fold(false)(od => od.selection)
      ).contains(true)

      purgeNull(Json.obj(
        "mandatoryRegistration" -> isMandatory,
        "voluntaryReason" -> (if (isMandatory) None else threshold.voluntaryRegistrationReason),
        "overThresholdDate" -> threshold.overThreshold.filter(_.selection == true).flatMap(_.date),
        "expectedOverThresholdDate" -> threshold.expectationOverThreshold.filter(_.selection == true).flatMap(_.date)
      ))
    }
  }
}
