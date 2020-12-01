package com.councilsearch

class Comment {
	User owner
	Date dateCreated
	String text

	static belongsTo = Event

	static constraints = {
		owner 	(nullable: false, blank: false)
		text	(nullable: false, blank: false)
	}

	static mapping = {
		text(sqlType: "text")
	}
}
