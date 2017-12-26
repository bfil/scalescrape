package io.bfil.scalescrape.data

import akka.http.scaladsl.model.HttpRequest
import io.bfil.scalescrape.context.ScrapingContext

case class ScrapingRequest(request: HttpRequest, context: ScrapingContext)
