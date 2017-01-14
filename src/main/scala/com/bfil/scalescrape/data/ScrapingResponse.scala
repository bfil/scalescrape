package com.bfil.scalescrape.data

import akka.http.scaladsl.model.HttpResponse
import com.bfil.scalescrape.context.ScrapingContext

case class ScrapingResponse(response: HttpResponse, context: ScrapingContext)
