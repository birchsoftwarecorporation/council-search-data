package com.councilsearch

class Region {
	String name
	String fipsCode
	String censusType
	Long population
	Date dateCreated
	Date lastModified
	String quality // low - medium - high
	static belongsTo = [state: State]
	static hasMany = [monitors: Monitor]

	static constraints = {
		name(nullable: false, blank: false, unique: false)
		fipsCode(nullable: true, blank: true, unique: false)
		censusType(nullable: true, blank: true, unique: false)
		population(nullable: true, blank: true, unique: false)
		quality(nullable: true, blank: true, unique: false)
	}
}
