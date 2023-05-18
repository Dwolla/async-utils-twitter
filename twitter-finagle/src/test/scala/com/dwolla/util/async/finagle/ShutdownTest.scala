package com.dwolla.util.async.finagle

import cats.effect._
import cats.syntax.all._
import cats.effect.syntax.all._
import example.thrift.SimpleService.SimpleService
import example.thrift.{SimpleRequest, SimpleResponse}

object ShutdownTest extends ResourceApp.Forever {
  override def run(args: List[String]): Resource[IO, Unit] =
    IO.executionContext.toResource.flatMap { implicit ec =>
      ThriftServer("0.0.0.0:12345", new SimpleService[IO] {
        override def makeRequest(request: SimpleRequest): IO[SimpleResponse] = {
          if (request.id == "hang") IO.println("hanging without response") >> IO.never
          else IO.println("handling request") >> IO.pure(SimpleResponse(s"hello ${request.id}"))
        }
      })
        .void
    }
}

object TestClient extends IOApp.Simple {
  override def run: IO[Unit] =
    IO.executionContext.flatMap { implicit ec =>
      ThriftClient[SimpleService][IO]("127.0.0.1:12345").use { c =>
        c.makeRequest(SimpleRequest("hang")).flatMap { res =>
          IO.println(res.id)
        }
      }
    }
}
