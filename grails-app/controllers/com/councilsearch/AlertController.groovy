package com.councilsearch


import grails.rest.*
import grails.converters.*

class AlertController {
	static responseFormats = ['json', 'xml']

	def springSecurityService
	def alertService

    def list() {
		def user = springSecurityService.getCurrentUser()
		List alerts = alertService.getAllAlerts(user)

		render alerts as JSON
	}


	def show(){
		def user = springSecurityService.getCurrentUser()
		boolean isAdmin = user.authorities.any { it.authority == "ROLE_ADMIN" }
		def alertId = params.id

		Alert alert = Alert.get(alertId)

		if(!alert){
			response.status = 400
			render ([error: "Could not find Alert:${alertId}"] as JSON)
			return
		}

		// Only managers can view their own alerts or admins can view any
		if(alert.manager != user && !isAdmin){
			response.status = 400
			render ([error: "You cannot view alerts you dont own"] as JSON)
			return
		}

		render alert as JSON
	}

	def save(){
		def user = springSecurityService.getCurrentUser()
		def alertJson = request.JSON

		// No data
		if(alertJson == null){
			response.status = 400
			render ([error: "Alert create failed please check all required fields"] as JSON)
			return
		}

		Alert alert = alertService.create(alertJson, user)

		// Alert wasnt created
		if(!alert){
			response.status = 400
			render ([error: "Alert create failed please check all required fields"] as JSON)
			return
		}

		render alert as JSON
	}

	// TODO
	def update(){
		response.status = 400
		render ([error: "Alert update not implemented...yet"] as JSON)
		return
	}

	def delete(){
		def user = springSecurityService.getCurrentUser()
		boolean isAdmin = user.authorities.any { it.authority == "ROLE_ADMIN" }
		def alertId = params.id

		Alert alert = Alert.get(alertId)

		if(!alert){
			response.status = 400
			render ([error: "Could not delete Alert:${alertId} it is not found"] as JSON)
			return
		}

		// Only managers can delete their alert or admins
		if(alert.manager != user && !isAdmin){
			response.status = 400
			render ([error: "You cannot delete alerts you dont own"] as JSON)
			return
		}

		alertService.remove(alertId)


		render ([status: "Removed Alert:${alertId}"] as JSON)
	}

	def process(){
		def alertId = params.id

		if (!Alert.exists(alertId)) {
			response.status = 400
			render ([error: "Alert:${alertId} does not exist"] as JSON)
			return
		}

		alertService.process(alertId)

		render ([status: "Processed Alert:${alertId}"] as JSON)
	}

}
