package com.sebruck.akka.http.graphql

import java.net.URLEncoder

import akka.http.scaladsl.model.headers.{Accept, `Content-Type`}
import akka.http.scaladsl.model.{
  ContentType,
  ContentTypes,
  HttpCharsets,
  MediaRanges,
  MediaType,
  MediaTypes
}
import ContentTypes.`application/json`
import akka.http.scaladsl.server.{MalformedHeaderRejection, UnsupportedRequestContentTypeRejection}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.syntax._
import org.scalatest.{FlatSpec, Matchers}
import sangria.execution.deferred.DeferredResolver
import sangria.schema.{Field, _}

class GraphEndpointSpec
    extends FlatSpec
    with ScalatestRouteTest
    with Matchers
    with FailFastCirceSupport {

  val Path = "/graphql"
  val arg  = Argument("arg", BooleanType)
  val Query = ObjectType(
    "Query",
    fields[Unit, Unit](
      Field("test", StringType, resolve = c => c.arg(arg).toString, arguments = List(arg))
    )
  )

  val Mutation = ObjectType(
    "Mutation",
    fields[Unit, Unit](
      Field("mut", StringType, resolve = c => c.arg(arg).toString, arguments = List(arg))
    )
  )

  val TestSchema = Schema(Query, Some(Mutation))
  val route      = new GraphEndpoint(TestSchema, (), DeferredResolver.empty, "graphql", None).route
  val routeWithPlayground =
    new GraphEndpoint(TestSchema, (), DeferredResolver.empty, "graphql", Some("playground.html")).route

  behavior of "Get"
  it should "respond with the playground" in {
    Get(Path).withHeaders(List(Accept(MediaRanges.`text/*`))) ~> routeWithPlayground ~> check {
      contentType shouldBe ContentTypes.`text/html(UTF-8)`
    }
  }

  it should "reject when no playground resource is configured" in {
    Get(Path).withHeaders(List(Accept(MediaRanges.`text/*`))) ~> route ~> check {
      rejection shouldBe a[MalformedHeaderRejection]
    }
  }

  it should "not handle requests with wildcard accepts" in {
    Get(Path).withHeaders(Accept(MediaRanges.`*/*`)) ~> route ~> check {
      handled shouldBe false
    }
  }

  val acceptJson = List(Accept(MediaRanges.`application/*`))
  it should "accept queries" in {
    Get(s"$Path?query={test(arg:true)}").withHeaders(acceptJson) ~> route ~> check {
      responseAs[Json].noSpaces shouldBe """{"data":{"test":"true"}}""".stripMargin
    }
  }

  it should "accept queries variables" in {
    val query     = "query TestQuery($theArg: Boolean!) { test(arg: $theArg) }"
    val variables = """{"theArg":false}"""

    Get(s"$Path?query=${URLEncoder.encode(query, "utf-8")}&variables=${variables}")
      .withHeaders(acceptJson) ~> route ~> check {
      responseAs[Json].noSpaces shouldBe """{"data":{"test":"false"}}""".stripMargin
    }
  }

  it should "not accept mutations" in {
    val query     = "mutation TestMutation($theArg: Boolean!) { mut(arg: $theArg) }"
    val variables = """{"theArg":false}"""

    Get(s"$Path?query=${URLEncoder.encode(query, "utf-8")}&variables=${variables}")
      .withHeaders(acceptJson) ~> route ~> check {
      responseAs[Json].noSpaces shouldBe """{"errors":[{"message":"Mutations in GET Requests are not allowed."}]}""".stripMargin
    }
  }

  behavior of "POST"

  it should "accept queries with variables" in {
    val query     = "query TestQuery($theArg: Boolean!) { test(arg: $theArg) }"
    val variables = Map("theArg" -> true).asJson
    val body      = Map("query" -> query.asJson, "variables" -> variables)

    Post(Path)
      .withHeaders(acceptJson)
      .withEntity(`application/json`, body.asJson.noSpaces) ~> route ~> check {
      responseAs[Json].noSpaces shouldBe """{"data":{"test":"true"}}""".stripMargin
    }
  }

  it should "accept mutations" in {
    val mutation  = "mutation TestMutation($theArg: Boolean!) { mut(arg: $theArg) }"
    val variables = """{"theArg":false}"""
    val body      = Map("query" -> mutation, "variables" -> variables)

    Post(Path)
      .withHeaders(acceptJson)
      .withEntity(`application/json`, body.asJson.noSpaces) ~> route ~> check {
      responseAs[Json].noSpaces shouldBe """{"data":{"mut":"false"}}""".stripMargin
    }
  }

  it should "reject requests with invalid content types" in {
    val formEncoded = ContentType(
      MediaType.applicationWithFixedCharset("x-www-form-urlencoded", HttpCharsets.`UTF-8`))

    val invalidContentTypes =
      List(
        ContentTypes.`text/html(UTF-8)`,
        formEncoded,
        ContentType(MediaTypes.`multipart/form-data`)
      )

    invalidContentTypes.foreach { contentType =>
      Post(Path)
        .withHeaders(acceptJson)
        .withHeaders(`Content-Type`(contentType))
        .withEntity("") ~> route ~> check {

        rejections.foreach(rej => rej shouldBe a[UnsupportedRequestContentTypeRejection])
      }
    }
  }
}
