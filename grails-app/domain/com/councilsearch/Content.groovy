package com.councilsearch

class Content {
	String text
	Date dateCreated
	Date lastModified // TODO - Migrate to lastUpdated

	static belongsTo =  [document: Document]

	static constraints = {
		text(nullable: true, blank: true)
	}

	static mapping = {
		text(sqlType: "longtext", column: 'text')
	}
}
