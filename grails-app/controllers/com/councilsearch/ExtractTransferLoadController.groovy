package com.councilsearch

import grails.converters.JSON

class ExtractTransferLoadController {
	def extractTransferLoadService

	// TODO - Rename this end point
	def start(){
		def monitorId = params.monitorId
		render extractTransferLoadService.start(monitorId) as JSON
	}

	// Process an alert
	def alerts(){
		extractTransferLoadService.processAlerts(null)

		render "Ran alert"
	}

	def events(){
		extractTransferLoadService.createEvents(null)

		render "Ran events"
	}

	def notifications(){
		log.info("Processing event notifications")

		try{
			extractTransferLoadService.notifications()
		}catch(Exception e){
			response.status = 400
			log.error("Error occured processing event notifications: "+e)
			render ([error: "Error occured processing event notifications: "+e] as JSON)
			return
		}

		log.info("Successfully processed event notifications")

		render "Successfully processed event notifications"
	}
}
