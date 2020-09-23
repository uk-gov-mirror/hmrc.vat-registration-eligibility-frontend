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

package forms

import forms.mappings.Mappings
import models._
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

class BusinessEntityFormProvider extends FormErrorHelper with Mappings {

  val businessEntity: String = "value"

  val ukCompany: String = "uk-company"

  val soleTrader: String = "sole-trader"

  val partnership: String = "partnership"

  val division: String = "division"

  val other: String = "other"

  val businessEntityError: String = "businessEntity.error.required"


  def apply(): Form[BusinessEntity] = Form(
    single(
      businessEntity -> of(formatter)
    )
  )

  def formatter: Formatter[BusinessEntity] = new Formatter[BusinessEntity] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BusinessEntity] = {
      data.get(key) match {
        case Some(`ukCompany`) => Right(UKCompany)
        case Some(`soleTrader`) => Right(SoleTrader)
        case Some(`partnership`) => Right(Partnership)
        case Some(`division`) => Right(Division)
        case Some(`other`) => Right(Other)
        case _ => Left(Seq(FormError(key, businessEntityError)))
      }
    }

    override def unbind(key: String, value: BusinessEntity): Map[String, String] = {
      val stringValue = value match {
        case UKCompany => ukCompany
        case SoleTrader => soleTrader
        case Partnership => partnership
        case Division => division
        case Other => other
      }
      Map(key -> stringValue)
    }
  }

  def businessEntityForm: Form[BusinessEntity] = Form(
    single(
      businessEntity -> of(formatter)
    )
  )

}