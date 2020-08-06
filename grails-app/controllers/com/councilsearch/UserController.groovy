package com.councilsearch


import grails.rest.*
import grails.converters.*

class UserController {
	static responseFormats = ['json', 'xml']

	def springSecurityService

	def index() {
		def user = springSecurityService.getCurrentUser()
		boolean isAdmin = user.authorities.any { it.authority == "ROLE_ADMIN" }
		Organization org = user.organization
		List users = []

		User.findAllByOrganizationAndEnabled(org, true).each{ u ->
			Map uMap = [:]
			uMap.put("id", u.id)
			uMap.put("firstName", u.firstName)
			uMap.put("lastName", u.lastName)
			uMap.put("email", u.username)
			users.add(uMap)
		}

		render users as JSON
	}


}
