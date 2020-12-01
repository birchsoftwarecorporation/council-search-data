package com.councilsearch

class Alert {
	String name
	Date dateCreated
	Date lastUpdated
	User manager
	String status
	String image

	static belongsTo = User
	static hasMany = [regions: Region, phrases: Phrase, members: User, matches: Match]

	static constraints = {
		name 	(nullable: false, blank: false)
		status	(nullable: false, blank: false, inList: ["pending", "live", "removed", "error"])
		manager	(nullable: false, blank: false)
		image	(nullable: true, blank: true)
	}
}
