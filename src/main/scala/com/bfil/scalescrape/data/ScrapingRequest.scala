package com.bfil.scalescrape.data

import akka.http.scaladsl.model.HttpRequest
import com.bfil.scalescrape.context.ScrapingContext

case class ScrapingRequest(request: HttpRequest, context: ScrapingContext)
