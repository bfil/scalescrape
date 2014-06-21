package com.bfil.scalescrape.data

import com.bfil.scalescrape.context.ScrapingContext

import spray.http.HttpRequest

case class ScrapingRequest(request: HttpRequest, context: ScrapingContext)