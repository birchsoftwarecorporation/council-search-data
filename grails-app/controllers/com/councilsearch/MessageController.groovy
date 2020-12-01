package com.councilsearch


import grails.rest.*
import grails.converters.*

class MessageController {
	static responseFormats = ['json']

	def messageService

	def save(){
		def leadJSON = request.JSON
		Lead lead

		try{
			lead = new Lead(leadJSON)

			if(!lead.save()){
				response.status = 500
				log.error "Unable to save lead! Errors: " + lead.errors
				render ([error: "Unable to save lead: "+ lead.errors] as JSON)
			}

			// Notify us of the inquiry
			messageService.contact(lead)
		}catch(Exception e){
			response.status = 500
			log.error "Unable to save lead! Errors: " + lead.errors
			render ([error: "Unable to save lead!"] as JSON)
		}

		render lead as JSON
	}
}
