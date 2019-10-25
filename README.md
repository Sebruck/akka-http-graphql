# Akka HTTP ➕ Sangria GraphQL ➕ Circe ＝ 💖

This library allows to easily create a [Sangria GraphQL](https://sangria-graphql.org) server based on 
[Akka HTTP](https://github.com/akka/akka-http) and [circe](https://circe.github.io/circe/).

## Supported Scala Versions
Currently only `2.12` is supported, as soon as Sangria is available for `2.13` both will be supported.

## Features

* executing GraphQL queries via GET & POST
* Anti CORS measures:
  * Decline mutations via GET
  * POST requests accept only content types `application/json` and `application/graphql`
* Serve `playground.html` (if configured) on `GET` with `Accepts: text/html`

## Usage

Add the following dependency to your sbt project

```
"com.github.sebruck" %% "akka-http-graphql" % "0.1"
```

````scala
  import akka.actor.ActorSystem
  import akka.http.scaladsl.Http
  import akka.stream.ActorMaterializer
  import com.sebruck.akka.http.graphql.GraphEndpoint
  import sangria.execution.deferred.DeferredResolver
  import sangria.schema._

  import scala.concurrent.ExecutionContextExecutor
  import scala.util.{Failure, Success}

  implicit val actorSystem: ActorSystem     = ActorSystem()
  implicit val mat: ActorMaterializer       = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher

  // Define Schema
  val Query = ObjectType(
    "Query",
    fields[Unit, Unit](
      Field("test", StringType, resolve = _ => "Hello!")
    )
  )

  val MySchema = Schema(Query)

  // Initialise Akka Http Endpoint
  val endpoint = GraphEndpoint(schema = MySchema,
                               context = (),
                               deferredResolver = DeferredResolver.empty,
                               graphQLPath = "graphql",
                               graphQLPlaygroundResourcePath = Some("playground.html"))

  // Start server
  Http().bindAndHandle(endpoint.route, "127.0.0.1", 8080).onComplete {
    case Success(binding) =>
      println(s"Bound to $binding")
    case Failure(exception) => throw exception
  }
````

If you have the [GraphQL Playground](https://github.com/prisma-labs/graphql-playground/blob/master/packages/graphql-playground-html/withAnimation.html)
in your `resources` directory, you can now open [http://localhost:8080/graphql](http://localhost:8080/graphql) 
in your browser and play with your freshly created GraphQL API!

## Contributing
Contributions and are very welcome!
