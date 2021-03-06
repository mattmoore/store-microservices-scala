package io.mattmoore.store.user

import cats.effect.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import doobie.*
import fs2.kafka.*
import io.mattmoore.store.algebras.*
import io.mattmoore.store.user.algebras.*
import io.mattmoore.store.user.domain.*
import io.mattmoore.store.user.repositories.*
import io.mattmoore.store.user.services.*
import natchez.*
import org.flywaydb.core.Flyway

object Main extends IOApp {
  type F[A] = IO[A]

  override def run(args: List[String]): F[ExitCode] = {
    entryPoint[F].use { ep =>
      ep.root("Starting the app").use { span =>
        Trace.ioTrace(span).flatMap { implicit trace =>
          implicit val natchez = span
          val xa: Transactor[F] = Transactor.fromDriverManager[F](
            "org.postgresql.Driver",
            "jdbc:postgresql:users",
            "postgres",
            "password"
          )
          Flyway
            .configure()
            .mixed(true)
            .baselineOnMigrate(true)
            .dataSource("jdbc:postgresql:users", "postgres", "password")
            .load()
            .migrate()
          val userRepository: RepositoryAlgebra[F, User] = new UserRepositoryInterpreter(xa)
          val userService: UserServiceAlgebra[F] = new UserServiceInterpreter[F](userRepository)

          val consumerSettings =
            ConsumerSettings[F, String, String]
              .withAutoOffsetReset(AutoOffsetReset.Earliest)
              .withBootstrapServers("localhost:9092")
              .withGroupId("group")

          KafkaConsumer
            .stream(consumerSettings)
            .subscribeTo("user.created")
            .records
//            .evalMap { committableConsumerRecord =>
//              userService
//                .add(
//                  User(
//                    firstName = committableConsumerRecord.record.value,
//                    lastName = "Moore",
//                    email = "matt@mattmoore.io",
//                    address = "123 Anywhere Street, Chicago, IL"
//                  )
//                )
//            }
            .compile
            .drain
            .as(ExitCode.Success)
        }
      }
    }
  }

  private def entryPoint[F[_]: Sync]: Resource[F, EntryPoint[F]] = {
    import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration}
    import natchez.jaeger.Jaeger
    Jaeger.entryPoint[F]("UserService") { c =>
      Sync[F].delay {
        c.withSampler(new SamplerConfiguration().withType("const").withParam(1))
          .withReporter(ReporterConfiguration.fromEnv)
          .getTracer
      }
    }
  }
}
