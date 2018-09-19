package repositories

import helpers.IntegrationSpecBase
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global


class SessionRepositoryISpec extends IntegrationSpecBase {

  class Setup {
    val newMongoInstance = new ReactiveMongoRepository(app.injector.instanceOf[Configuration], mongo)
  }

  "dropCollectionIndexTemp" should {
    "drop collection , create index, drop index using dropCollectionIndexTemp then attempt to create index again Successfully" in new Setup {
      await(newMongoInstance.drop)
      await(newMongoInstance.collection.indexesManager.list()).isEmpty mustBe true
      await(newMongoInstance.createIndex(newMongoInstance.fieldName, newMongoInstance.createdIndexName,10))
      await(newMongoInstance.collection.indexesManager.list()).nonEmpty mustBe true
      await(newMongoInstance.dropCollectionIndexTemp) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).nonEmpty mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe false
      await(newMongoInstance.createIndex(newMongoInstance.fieldName, newMongoInstance.createdIndexName,129)) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe true
    }
    "drop and create works sucessfully when documents are in the collection" in new Setup {
      await(newMongoInstance.collection.indexesManager.list()).nonEmpty mustBe true
      val record1 = CacheMap("1",Map("foo" -> Json.obj("" -> "")))
      val record2 = CacheMap("2",Map("foo" -> Json.obj("" -> "")))
      await(newMongoInstance.upsert(record1)) mustBe true
      await(newMongoInstance.upsert(record2)) mustBe true
      await(newMongoInstance.count) mustBe 2
      await(newMongoInstance.dropCollectionIndexTemp) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe false
      await(newMongoInstance.createIndex(newMongoInstance.fieldName, newMongoInstance.createdIndexName,129)) mustBe true
      await(newMongoInstance.collection.indexesManager.list()).map(_.name.getOrElse("")).contains("userAnswersExpiry") mustBe true
      await(newMongoInstance.count) mustBe 2
    }
  }
}