package com.bfil.scalescrape.data

case class Request[T](url: String, content: T)