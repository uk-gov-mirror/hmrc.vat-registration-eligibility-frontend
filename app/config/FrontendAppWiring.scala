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

package config

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.crypto.{ApplicationCrypto, CryptoWithKeysFromConfig}
import uk.gov.hmrc.http.cache.client.{ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import javax.inject.{Inject, Singleton}


trait Logging {
  val logger: Logger = LoggerFactory.getLogger(getClass)
}

class VatShortLivedHttpCaching @Inject()(val http: HttpClient, config: ServicesConfig) extends ShortLivedHttpCaching {

  override lazy val defaultSource = config.getString("appName")
  override lazy val baseUri       = config.baseUrl("cachable.short-lived-cache")
  override lazy val domain        = config.getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))

}

@Singleton
class VatShortLivedCache @Inject()(val shortLiveCache: ShortLivedHttpCaching,
                                   applicationCrypto: ApplicationCrypto) extends ShortLivedCache {

  override implicit lazy val crypto: CryptoWithKeysFromConfig = applicationCrypto.JsonCrypto

}