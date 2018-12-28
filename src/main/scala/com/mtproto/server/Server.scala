package com.mtproto.server

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.mtproto.server.auth.AuthServer.handler
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

object Server extends IOApp with StrictLogging {

  override def run(args: List[String]): IO[ExitCode] = {
    actorSystem.use { implicit system =>
      implicit val materializer: ActorMaterializer = ActorMaterializer()

      def startServer(config: ApplicationConfig): IO[Done] = {
        IO.delay(logger.info(s"Starting application with config $config")) >>
          IO.fromFuture(IO(Tcp().bind(config.host, config.port).runForeach(_.handleWith(handler))))
      }

      (IO.delay(ConfigFactory.load()) >>= readConfig >>= startServer) >> IO.never >> IO.pure(ExitCode.Success)
    }
  }

  private def actorSystem: Resource[IO, ActorSystem] =
    Resource.make(IO.delay(ActorSystem()))(a => IO.fromFuture(IO(a.terminate())).void)

  private def readConfig(config: Config): IO[ApplicationConfig] = {
    IO.delay(
      ApplicationConfig(config.getString("application.host"), config.getInt("application.port"))
    )
  }

  private final case class ApplicationConfig(host: String, port: Int)

}
