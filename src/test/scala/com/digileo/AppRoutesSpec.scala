package com.digileo

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class AppRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.toClassic

  "AppRoutes" should {
    "reject request if route is called with a non-numeric string" in {
      val mockCache = testKit.spawn(Behaviors.receiveMessage[Cache.Command] {
        case Cache.Question(_: Int, replyTo: ActorRef[Answer]) => {
          replyTo ! Some('B')
          Behaviors.same
        }
      })

      lazy val routes = new AppRoutes(mockCache).appRoutes

      val request = HttpRequest(uri = "/xyz")

      val result = request ~> routes
      result.handled should equal(false)
    }

    "respond with OK and value if Cache answers with a value " in {
      val mockCache = testKit.spawn(Behaviors.receiveMessage[Cache.Command] {
        case Cache.Question(_: Int, replyTo: ActorRef[Answer]) => {
          replyTo ! Some('B')
          Behaviors.same
        }
      })

      lazy val routes = new AppRoutes(mockCache).appRoutes

      val request = HttpRequest(uri = "/0")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("B")
      }
    }

    "respond with InternalServerError and empty string if Cache answers with None " in {
      val mockCache = testKit.spawn(Behaviors.receiveMessage[Cache.Command] {
        case Cache.Question(_: Int, replyTo: ActorRef[Answer]) => {
          replyTo ! None
          Behaviors.same
        }
      })

      lazy val routes = new AppRoutes(mockCache).appRoutes

      val request = HttpRequest(uri = "/0")

      request ~> routes ~> check {
        status should ===(StatusCodes.InternalServerError)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("")
      }
    }
  }
}
