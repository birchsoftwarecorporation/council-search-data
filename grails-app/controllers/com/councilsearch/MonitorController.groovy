package com.councilsearch

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import grails.converters.JSON

import java.sql.SQLException

class MonitorController {

	def monitorService

	def list() {
		def regionId = params.regionId
		log.info("Showing Monitors for Region:${regionId}")

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
		def regionId = params.regionId
		def dataJSON = request.JSON

		Monitor monitor

		try{
			monitor = monitorService.create(regionId, dataJSON)
		}catch(Exception e){
			response.status = 500
			log.error "Unable to save monitor! Errors: " + e.message
			render ([error: "Unable to save monitor!"] as JSON)
			return
		}

		render monitor as JSON
	}

	def update(){
		def dataJSON = request.JSON
		Monitor monitor

		try{
			monitor = monitorService.update(dataJSON)
		}catch(Exception e){
			response.status = 500
			log.error "Unable to save monitor! Errors: " + e.message
			render ([error: "Unable to save monitor!"] as JSON)
			return
		}

		render monitor as JSON
	}

	def delete(){
		def id = params.id

		try{
			monitorService.delete(id)
		}catch(Exception e){
			response.status = 500
			log.error "Unable to delete monitor! Errors: " + e.message
			render ([error: "Unable to delete monitor!"] as JSON)
			return
		}

		render "Successfully deleted Monitor:${id}"
	}

	def process(){
		def id = params.id
		def dataMaps = []

		try{
			dataMaps = monitorService.process(id)
		}catch(SQLException | IOException | FailingHttpStatusCodeException | MalformedURLException e){
			response.status = 500
			log.error "Unable to process Monitor:${id}. Errors: " + e.message
			render ([error: "Unable to process Monitor:${id}"] as JSON)
			return
		}

		render dataMaps as JSON
	}


}
