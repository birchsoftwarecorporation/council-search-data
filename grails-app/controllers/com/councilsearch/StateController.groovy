package com.councilsearch

import grails.converters.JSON

class StateController {

    def list() {
		render State.getAll() as JSON
	}

	def show() {
		def id = params.id

		if(!id){
			response.status = 400
			render ([error: "State Id not specified"] as JSON)
			return
		}

		State state = State.get(id)

		if(!state){
			response.status = 400
			render ([error: "State with id:${id} not found"] as JSON)
			return
		}

		render state as JSON
	}

	def error(){
		response.status = 403
		render ([error: "Action not allowed"] as JSON)
		return
	}

}
