package com.councilsearch

import grails.converters.JSON

class MonitorController {
	def index() {
		def regionId = params.regionId
		log.info("Showing Monitors for Region:${regionId}")

		if(!regionId){
			response.status = 400
			log.error("Region Id not specified")
			render ([error: "Region Id not specified"] as JSON)
			return
		}

		Region region = Region.get(regionId)

		if(!region){
			response.status = 400
			log.error("Region with regionId:${regionId} not found")
			render ([error: "Region with regionId:${regionId} not found"] as JSON)
			return
		}

		List monitors = Monitor.findAllByRegion(region)

		render monitors as JSON
	}

	def show() {
		def id = params.id

		log.info("Showing Monitor:${id}")

		if(!id){
			log.error("Monitor Id not specified")
			response.status = 400
			render ([error: "Monitor Id not specified"] as JSON)
			return
		}

		Monitor monitor = Monitor.get(id)

		if(!monitor){
			response.status = 400
			log.error("Monitor with id:${id} not found")
			render ([error: "Monitor with id:${id} not found"] as JSON)
			return
		}

		render monitor as JSON
	}

	def save(){
		def monitorJSON = request.JSON
		Monitor monitor

		log.info("Saving Monitor...")

		try{
			// Example: { "url":"google.com", "status":"open", "extractorId":"dfsdflkjlk", "region":{"id":"1"}}
			monitor = new Monitor(monitorJSON)

			if(!monitor.save()){
				response.status = 500
				log.error "Unable to save monitor! Errors: " + monitor.errors
				render ([error: "Unable to save monitor: "+ monitor.errors] as JSON)
			}
		}catch(Exception e){
			response.status = 500
			log.error "Unable to save monitor! Errors: " + monitor?.errors
			render ([error: "Unable to save monitor!" + monitor?.errors] as JSON)
		}

		render monitor as JSON
	}

//	def update(Monitor monitor){
////		def monitorJSON = request.JSON
////		Monitor monitor
//
//		try{
//			// Example: { "url":"google.com", "status":"open", "extractorId":"dfsdflkjlk", "region":{"id":"1"}}
////			monitor = Monitor.get(monitorJSON)
//
//			if(!monitor.save()){
//				response.status = 500
//				println "Unable to update monitor! Errors: " + monitor.errors
//				render ([error: "Unable to update monitor: "+ monitor.errors] as JSON)
//			}
//		}catch(Exception e){
//			response.status = 500
//			println "Unable to update monitor! Errors: " + monitor.errors
//			render ([error: "Unable to update monitor!" + monitor.errors] as JSON)
//		}
//
//		render monitor as JSON
//	}

}
