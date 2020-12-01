package com.councilsearch


import grails.rest.*
import grails.converters.*

class UserController {
	static responseFormats = ['json', 'xml']

	def springSecurityService

	def list() {
		def user = springSecurityService.getCurrentUser()
//		boolean isAdmin = user.authorities.any { it.authority == "ROLE_ADMIN" }
		Organization org = user.organization
		List usersJSON = []

		// Grab all of the orgs users
		List users = User.findAllByOrganizationAndEnabled(org, true)

		// Remove the curr user
		users.remove(user)

		// Sort the users by name L/F
		users = users.sort { a, b ->
			a.lastName <=> b.lastName ?: a.firstName <=> b.firstName
		}

		users.each{ u ->
			Map uMap = [:]
			uMap.put("id", u.id)
			uMap.put("firstName", u.firstName)
			uMap.put("lastName", u.lastName)
			uMap.put("email", u.username)
			usersJSON.add(uMap)
		}

		render usersJSON as JSON
	}


}
