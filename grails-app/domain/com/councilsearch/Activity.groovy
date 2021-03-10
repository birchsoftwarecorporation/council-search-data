package com.councilsearch

class Activity {
	String name
	String details
	String ipAddress
	Date dateCreated

    static constraints = {
		name(nullable: false, blank: false)
		details(nullable: false, blank: false)
		ipAddress(nullable: true, blank: true)
    }

	static mapping = {
		details(sqlType: "longtext", column: 'text')
	}
}
