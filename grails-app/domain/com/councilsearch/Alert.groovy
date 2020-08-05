package com.councilsearch

class Alert {
	String name
	Date dateCreated
	Date lastUpdated
	User manager
	String status

	static constraints = {
		name 	(nullable: false, blank: false)
		status	(nullable: true, blank: true)
		manager	(nullable: false, blank: false)
	}

	static belongsTo = User
	static hasMany = [regions: Region, phrases: Phrase, members: User]
}
