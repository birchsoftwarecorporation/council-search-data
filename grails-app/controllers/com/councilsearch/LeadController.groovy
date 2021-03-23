package com.councilsearch


import grails.rest.*
import grails.converters.*

class LeadController {
	static responseFormats = ['json', 'xml']

	def leadService

	def save(){
		def leadJSON = request.JSON
		Lead lead

		if(leadJSON == null){
			response.status = 400
			log.error "Unable to save lead with no data."
			render ([error: "Unable to save lead: "+ lead.errors] as JSON)
		}

		try{
			lead = leadService.save(leadJSON)
		}catch(Exception e){
			response.status = 500
			render ([error: "Unable to save lead!"] as JSON)
		}

		render lead as JSON
	}
}
