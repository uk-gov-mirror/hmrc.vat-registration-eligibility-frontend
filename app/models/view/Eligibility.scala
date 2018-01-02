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

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Eligibility(haveNino: Option[Boolean],
                       doingBusinessAbroad: Option[Boolean],
                       doAnyApplyToYou: Option[Boolean],
                       applyingForAnyOf: Option[Boolean],
                       applyingForVatExemption: Option[Boolean],
                       companyWillDoAnyOf: Option[Boolean])

object Eligibility {
  implicit val format: Format[Eligibility] = (
    (__ \ "haveNino").formatNullable[Boolean] and
    (__ \ "doingBusinessAbroad").formatNullable[Boolean] and
    (__ \ "doAnyApplyToYou").formatNullable[Boolean] and
    (__ \ "applyingForAnyOf").formatNullable[Boolean] and
    (__ \ "applyingForVatExemption").formatNullable[Boolean] and
    (__ \ "companyWillDoAnyOf").formatNullable[Boolean]
  )(Eligibility.apply, unlift(Eligibility.unapply))

  val apiWrites: Writes[(String, Int)] = new Writes[(String, Int)] {
    override def writes(o: (String, Int)): JsValue = Json.parse(
      s"""
        |{
        | "version" : ${o._2},
        | "result" : "${o._1}"
        |}
      """.stripMargin
    )
  }

  val apiReads: Reads[(String, Int)] = new Reads[(String, Int)] {
    override def reads(json: JsValue): JsResult[(String, Int)] = {
      JsSuccess((json.\("result").as[String], json.\("version").as[Int]))
    }
  }
}
