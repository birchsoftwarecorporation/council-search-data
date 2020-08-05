package com.councilsearch


import grails.rest.*
import grails.converters.*

class LeadController {
	static responseFormats = ['json']

	def index(){
		List leads = Lead.getAll()
		render leads as JSON
	}

	def save(){
		def leadJSON = request.JSON
		Lead lead

		try{
			lead = new Lead(leadJSON)

			if(!lead.save()){
				response.status = 500
				println "Unable to save lead! Errors: " + lead.errors
				render ([error: "Unable to save lead: "+ lead.errors] as JSON)
			}
		}catch(Exception e){
			response.status = 500
			println "Unable to save lead! Errors: " + lead.errors
			render ([error: "Unable to save lead!" + lead.errors] as JSON)
		}

		render lead as JSON
	}
}
