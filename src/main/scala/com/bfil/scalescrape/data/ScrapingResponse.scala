package com.bfil.scalescrape.data

import spray.http.HttpResponse
import com.bfil.scalescrape.context.ScrapingContext

case class ScrapingResponse(response: HttpResponse, context: ScrapingContext)