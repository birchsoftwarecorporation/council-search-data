package com.councilsearch

class Phrase {
	String name
	boolean exactMatch = true

    static constraints = {
		name(nullable: false, blank: false)
		exactMatch(nullable: false, blank: false)
    }

	static belongsTo = Alert
}
