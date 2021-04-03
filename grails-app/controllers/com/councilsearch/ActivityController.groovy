package com.councilsearch


import grails.rest.*
import grails.converters.*

class ActivityController {
	static responseFormats = ['json', 'xml']

	def activityService

    def save() {
		def activityJSON = request.JSON
		Activity activity

		if(activityJSON == null){
			response.status = 400
			log.error "Unable to save activity with no data."
			render ([error: "Unable to save activity"] as JSON)
		}

		try{
			activity = activityService.create(activityJSON?.name, activityJSON?.details, request?.getHeader("X-Real-IP"))
		}catch(Exception e){
			response.status = 500
			render ([error: "Unable to save activity"] as JSON)
		}

		render activity as JSON
	}
}
