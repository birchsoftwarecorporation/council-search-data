package com.councilsearch

class State {
	String name
	String abbr
	String fipsCode
	Long population
	Date dateCreated
	Date lastModified
	Integer utc // This will be for nation wide crawls east -> west
	static hasMany = [counties: County,
					  places: Place,
					  agencies: Agency]

	static constraints = {
		name(nullable: false, blank: false, unique: true)
		abbr(nullable: false, blank: false, unique: true)
		fipsCode(nullable: false, blank: false, unique: true)
		population(nullable: false, blank: false, unique: false)
		utc(nullable: false, blank: false, unique: false)
	}

	static {
		grails.converters.JSON.registerObjectMarshaller(State) { State state ->
			return [
					id: state.id,
					name: state.name,
					abbr: state.abbr
			]
		}
	}

}
