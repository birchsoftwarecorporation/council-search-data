package com.councilsearch

class Activity {
	Date dateCreated
	String name
	String details
	String ipAddress

    static constraints = {
		name(nullable: false, blank: false)
		details(nullable: true, blank: true)
		ipAddress(nullable: true, blank: true)
    }
}
