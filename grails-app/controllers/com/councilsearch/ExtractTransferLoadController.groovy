package com.councilsearch

import grails.converters.JSON

class ExtractTransferLoadController {
	def extractTransferLoadService

	def start(){
		def monitorId = params.monitorId
		render extractTransferLoadService.start(monitorId) as JSON
	}

	def cleanText(){

	}

}
