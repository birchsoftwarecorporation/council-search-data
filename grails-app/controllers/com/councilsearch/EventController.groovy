package com.councilsearch


import grails.rest.*
import grails.converters.*

class EventController {
	static responseFormats = ['json', 'xml']

	def springSecurityService
	def eventService

    def list() {
		def user = springSecurityService.getCurrentUser()
		List events = eventService.getEvents(user)

		render events as JSON
	}

	def show(){
		def user = springSecurityService.getCurrentUser()
		def uuid = params.uuid

		// TODO - You can only view Events you participate in

		Map eventMap = eventService.getEvent(uuid)

		if(!eventMap){
			response.status = 400
			render ([error: "Could not find Event:${uuid}"] as JSON)
			return
		}

		render eventMap as JSON
	}

	def description(){
		def user = springSecurityService.getCurrentUser()
		def eventUUID = params.uuid
		def descJson = request.JSON

		// TODO - You can only edit Event descriptions you participate in

		try{
			eventService.updateDescription(eventUUID, descJson?.text)
		}catch(Exception e){
			response.status = 400
			log.error("Could not update description: "+e)
			render ([error: "Could not update description: "+e] as JSON)
			return
		}

		render true
	}

	def comment(){
		def user = springSecurityService.getCurrentUser()
		def eventUUID = params.uuid
		def commentJson = request.JSON
		Comment comment
		// TODO - You can only edit Event descriptions you participate in

		try{
			comment = eventService.createComment(user, eventUUID, commentJson?.text)
		}catch(Exception e){
			response.status = 400
			log.error("Could not add comment: "+e)
			render ([error: "Could not add comment: "+e] as JSON)
			return
		}

		// Build the map rep
		Map cMap = [:]
		cMap.put("firstName", comment.owner.firstName)
		cMap.put("lastName", comment.owner.lastName)
		cMap.put("dateCreated", comment.dateCreated)
		cMap.put("text", comment.text)

		render cMap as JSON
	}

	// Get this events team members
	def members(){
		def user = springSecurityService.getCurrentUser()
		def eventUUID = params.uuid
		List members = []

		// TODO - You can only edit Event descriptions you participate in

		try{
			members = eventService.getMembers(eventUUID)
		}catch(Exception e){
			response.status = 400
			log.error("Could not retrieve event members: "+e)
			render ([error: "Could not retrieve event members: "+e] as JSON)
			return
		}

		render members as JSON
	}

	// Get this events team members
	def owner(){
		def user = springSecurityService.getCurrentUser()
		def eventUUID = params.uuid
		def ownerJson = request.JSON

		// TODO - You can only edit Event descriptions you participate in

		try{
			eventService.updateOwner(eventUUID, ownerJson.ownerId)
		}catch(Exception e){
			response.status = 400
			log.error("Could not update owner: "+e)
			render ([error: "Could not update owner: "+e] as JSON)
			return
		}

		render true
	}

	def status(){
		def user = springSecurityService.getCurrentUser()
		def eventUUID = params.uuid
		def statusJson = request.JSON
		def req = request.JSON

		// TODO - You can only edit Event descriptions you participate in

		try{
			eventService.updateStatus(eventUUID, statusJson.status)
		}catch(Exception e){
			response.status = 400
			log.error("Could not update status: "+e)
			render ([error: "Could not update event status: "+e] as JSON)
			return
		}

		render true
	}
}
