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

import play.api.i18n.Messages
import play.api.libs.json._

sealed trait BusinessEntity {

  import BusinessEntity._

  override def toString: String = this match {
    case UKCompany => UKCompanyKey
    case SoleTrader => SoleTraderKey
    case Partnership => PartnershipKey
    case Division => DivisionKey
    case Other => OtherKey
  }
}

object UKCompany extends BusinessEntity

object SoleTrader extends BusinessEntity

object Partnership extends BusinessEntity

object Division extends BusinessEntity

object Other extends BusinessEntity

object BusinessEntity {
  val UKCompanyKey = "uk-company"
  val SoleTraderKey = "sole-trader"
  val PartnershipKey = "partnership"
  val DivisionKey = "division"
  val OtherKey = "other"


  private def stringToBusinessEntity(businessEntity: String): BusinessEntity = businessEntity match {
    case UKCompanyKey => UKCompany
    case SoleTraderKey => SoleTrader
    case PartnershipKey => Partnership
    case DivisionKey => Division
    case OtherKey => Other
    case unknownKey => throw new IllegalArgumentException(s"Unknown Business Entity: $unknownKey")
  }

  def businessEntityToString(businessEntity: BusinessEntity)(implicit messages: Messages): String = businessEntity match {
    case UKCompany => messages("businessEntity.ukcompany")
    case SoleTrader => messages("businessEntity.soletrader")
    case Partnership => messages("businessEntity.partnership")
    case Division => messages("businessEntity.division")
    case Other => messages("businessEntity.other")
  }

  implicit val jsonReads: Reads[BusinessEntity] = new Reads[BusinessEntity] {
    override def reads(json: JsValue): JsResult[BusinessEntity] =
      json.validate[String] map stringToBusinessEntity
  }

  implicit val jsonWrites: Writes[BusinessEntity] = Writes[BusinessEntity] {
    case UKCompany => JsString(UKCompanyKey)
    case SoleTrader => JsString(SoleTraderKey)
    case Partnership => JsString(PartnershipKey)
    case Division => JsString(DivisionKey)
    case Other => JsString(OtherKey)
    case unknownKey => throw new IllegalArgumentException(s"Unknown Business Entity: $unknownKey")
  }

  implicit val jsonFormat: Format[BusinessEntity] = Format[BusinessEntity](jsonReads, jsonWrites)
}
