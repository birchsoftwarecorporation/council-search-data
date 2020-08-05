package com.councilsearch

class CustomConfig {
	String name
	String value

    static constraints = {
		name(nullable: false, blank: false, unique: true)
		value(nullable: false, blank: false, unique: false)
    }
}
