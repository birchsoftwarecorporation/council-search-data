package com.councilsearch

class Event {
	Match match // Optional
	User owner
	Date dateCreated
	Date lastUpdated
	Date dueDate
	Date emailed
	String status // Open | In Progress | Complete
	String description
	String uuid
	Boolean isRemoved // Kind of want to keep these around just in case

	static hasMany = [comments: Comment, members: User]

	static constraints = {
		match (nullable: true, blank: true)
		owner (nullable: false, blank: false)
		dueDate (nullable: false, blank: false)
		status (nullable: false, blank: false, inList: ["open", "in progress", "complete", "error"])
		description	(nullable: true, blank: true)
		isRemoved (nullable: true, blank: false)
		emailed(nullable: true, blank: true)
		uuid(nullable: false, blank: false, unique: true)
	}

	static mapping = {
		description(sqlType: "text")
		isRemoved defaultValue: false
	}
}
