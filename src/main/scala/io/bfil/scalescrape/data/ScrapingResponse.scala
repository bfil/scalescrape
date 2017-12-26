package io.bfil.scalescrape.data

import akka.http.scaladsl.model.HttpResponse
import io.bfil.scalescrape.context.ScrapingContext

case class ScrapingResponse(response: HttpResponse, context: ScrapingContext)
