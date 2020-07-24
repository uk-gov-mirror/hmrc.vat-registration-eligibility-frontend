/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import play.api.libs.json._
import utils.{Enumerable, RadioOption, WithName}

sealed trait TurnoverEstimate

object TurnoverEstimate {

  case object Zeropounds extends WithName("zeropounds") with TurnoverEstimate
  case object Oneandtenthousand extends WithName("oneandtenthousand") with TurnoverEstimate
  case object TenThousand extends WithName("tenthousand") with TurnoverEstimate

  val values: Set[TurnoverEstimate] = Set(
    Zeropounds, Oneandtenthousand, TenThousand
  )

  val options: Set[RadioOption] = values.map {
    value =>
      RadioOption("turnoverEstimateSelection", value.toString)
  }

  implicit val enumerable: Enumerable[TurnoverEstimate] =
    Enumerable(values.toSeq.map(v => v.toString -> v): _*)
}

case class TurnoverEstimateFormElement(value : String, optionalData : Option[String])

object TurnoverEstimateFormElement {
  implicit val turnoverEstimateFormReads: Reads[TurnoverEstimateFormElement] = new Reads[TurnoverEstimateFormElement] {
    override def reads(json: JsValue): JsResult[TurnoverEstimateFormElement] = {
      val selection = (json \ "selection").as[String]
      val amount = (json \ "amount").asOpt[String]

      JsSuccess(TurnoverEstimateFormElement(selection, amount))
    }
  }

  implicit val turnoverEstimateFormWrites: Writes[TurnoverEstimateFormElement] = new Writes[TurnoverEstimateFormElement] {
    override def writes(formEle: TurnoverEstimateFormElement): JsValue = {
      val amount = if (formEle.optionalData.isDefined) Json.obj("amount" -> formEle.optionalData) else Json.obj()

      Json.obj("selection" -> formEle.value).deepMerge(amount)
    }
  }
}
