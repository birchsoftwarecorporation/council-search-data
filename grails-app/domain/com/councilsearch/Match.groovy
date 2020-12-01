package com.councilsearch

class Match {
	Alert alert
	Document document
	Date dateCreated
	Boolean eventCreated

	static belongsTo = Alert
	static hasMany = [previews: Preview]

    static constraints = {
		document(nullable: false, blank: false)
		eventCreated(nullable: false, blank: false)
    }

	// Reserved mysql words
	static mapping = {
		table '`match`'
	}
}
