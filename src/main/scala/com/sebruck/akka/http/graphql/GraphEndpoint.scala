package com.sebruck.akka.http.graphql

import akka.http.scaladsl.model.MediaTypes.{`application/json`, `text/html`}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.sebruck.akka.http.graphql.GraphQLRequestUnmarshaller._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import io.circe.syntax._
import sangria.ast.{Document, OperationType}
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.circe._
import sangria.parser.DeliveryScheme.Try
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.Schema
import sangria.slowlog.SlowLog

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class GraphEndpoint[Ctx](
    schema: Schema[Ctx, Unit],
    context: Ctx,
    deferredResolver: DeferredResolver[Ctx],
    graphQLPath: String,
    graphQLPlaygroundResourcePath: Option[String])(implicit ec: ExecutionContext) {

  private def executeGraphQL(query: Document,
                             operationName: Option[String],
                             variables: Json,
                             tracing: Boolean) = {
    complete(
      Executor
        .execute(
          schema,
          query,
          context,
          variables = if (variables.isNull) Json.obj() else variables,
          operationName = operationName,
          deferredResolver = deferredResolver,
          middleware = if (tracing) SlowLog.apolloTracing :: Nil else Nil
        )
        .map(OK -> _)
        .recover {
          case error: QueryAnalysisError => BadRequest          -> error.resolveError
          case error: ErrorWithResolver  => InternalServerError -> error.resolveError
        })
  }

  private def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError =>
      Json.obj(
        "errors" -> Json.arr(Json.obj(
          "message" -> Json.fromString(syntaxError.getMessage),
          "locations" -> Json.arr(
            Json.obj("line"   -> Json.fromBigInt(syntaxError.originalError.position.line),
                     "column" -> Json.fromBigInt(syntaxError.originalError.position.column)))
        )))
    case NonFatal(e) =>
      formatError(e.getMessage)
    case e =>
      throw e
  }

  private def formatError(message: String): Json =
    Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(message))))

  private def validateMutation(httpMethod: HttpMethod, query: Document) = {
    if (httpMethod == HttpMethods.GET) {
      val operations  = query.operations
      val hasMutation = operations.exists(_._2.operationType == OperationType.Mutation)

      if (hasMutation) Left(new Exception("Mutations in GET Requests are not allowed."))
      else Right(())
    } else Right(())
  }

  private def parseAndExecuteGraphQL(httpMethod: HttpMethod,
                                     query: String,
                                     variables: Option[String],
                                     operationName: Option[String],
                                     tracing: Boolean) =
    (for {
      parsedQuery     <- QueryParser.parse(query).toEither
      parsedVariables <- variables.map(parse).getOrElse(Right(Json.obj()))
      _               <- validateMutation(httpMethod, parsedQuery)

    } yield executeGraphQL(parsedQuery, operationName, parsedVariables, tracing))
      .fold(error => complete((BadRequest, formatError(error))), identity)

  val route: Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing ⇒
      path(graphQLPath) {
        get {
          explicitlyAccepts(`text/html`) {
            graphQLPlaygroundResourcePath match {
              case Some(playground) => getFromResource(playground)
              case None =>
                reject(MalformedHeaderRejection("accept", "Serving text/html is not supported."))
            }
          } ~
            explicitlyAccepts(`application/json`) {
              parameters(('query, 'operationName.?, 'variables.?)) {
                (query, operationName, variables) =>
                  parseAndExecuteGraphQL(HttpMethods.GET,
                                         query,
                                         variables,
                                         operationName,
                                         tracing.isDefined)
              }
            }
        } ~
          post {
            parameters(('query.?, 'operationName.?, 'variables.?)) {
              (queryParam, operationNameParam, variablesParam) =>
                entity(as[Json]) { body =>
                  val query = queryParam orElse root.query.string.getOption(body)
                  val operationName = operationNameParam orElse root.operationName.string
                    .getOption(body)
                  val variables = variablesParam orElse root.variables.obj
                    .getOption(body)
                    .map(_.asJson.noSpaces)

                  query match {
                    case None => complete((BadRequest, formatError("No query to execute")))
                    case Some(q) =>
                      parseAndExecuteGraphQL(HttpMethods.POST,
                                             q,
                                             variables,
                                             operationName,
                                             tracing.isDefined)
                  }
                } ~
                  entity(as[Document]) { document =>
                    variablesParam.map(parse) match {
                      case Some(Left(error)) => complete((BadRequest, formatError(error)))
                      case Some(Right(json)) =>
                        executeGraphQL(document, operationNameParam, json, tracing.isDefined)
                      case None =>
                        executeGraphQL(document, operationNameParam, Json.obj(), tracing.isDefined)
                    }
                  }
            }
          }
      }
    }
}

object GraphEndpoint {
  def apply[Ctx](schema: Schema[Ctx, Unit],
                 context: Ctx,
                 deferredResolver: DeferredResolver[Ctx] = DeferredResolver.empty,
                 graphQLPath: String = "graphql",
                 graphQLPlaygroundResourcePath: Option[String] = None)(
      implicit ec: ExecutionContext): GraphEndpoint[Ctx] =
    new GraphEndpoint(schema, context, deferredResolver, graphQLPath, graphQLPlaygroundResourcePath)
}
