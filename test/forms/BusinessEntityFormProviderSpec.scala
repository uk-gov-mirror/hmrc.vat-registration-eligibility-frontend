/*
 * Copyright 2021 HM Revenue & Customs
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

import base.SpecBase
import models._
import play.api.data.FormError


class BusinessEntityFormProviderSpec extends SpecBase {
  val form = new BusinessEntityFormProvider()()

  "businessEntityForm" must {

    val businessEntity = "value"

    val businessEntityErrorKey = "businessEntity.error.required"

    "successfully parse a UK Company entity" in {
      val res = form.bind(Map(businessEntity -> UKCompany.toString))
      res.value must contain(UKCompany)
    }

    "successfully parse a Sole trader entity" in {
      val res = form.bind(Map(businessEntity -> SoleTrader.toString))
      res.value must contain(SoleTrader)
    }

    "successfully parse a Partnership entity" in {
      val res = form.bind(Map(businessEntity -> Partnership.toString))
      res.value must contain(Partnership)
    }

    "successfully parse a Division entity" in {
      val res = form.bind(Map(businessEntity -> Division.toString))
      res.value must contain(Division)
    }

    "successfully parse a Other entity" in {
      val res = form.bind(Map(businessEntity -> Other.toString))
      res.value must contain(Other)
    }

    "fail when nothing has been entered in the view" in {
      val res = form.bind(Map.empty[String, String])
      res.errors must contain(FormError(businessEntity, businessEntityErrorKey))
    }

    "fail when it is not an expected value in the view" in {
      val res = form.bind(Map(businessEntity -> "invalid"))
      res.errors must contain(FormError(businessEntity, businessEntityErrorKey))
    }
  }
}

