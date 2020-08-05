package com.councilsearch

class Lead {
	String name
	String email
	String company
	String phone
	String type
	String message
	Date dateCreated

	static constraints = {
		name(nullable: true, blank: true)
		email(nullable: true, blank: true)
		company(nullable: true, blank: true)
		phone(nullable: true, blank: true)
		type(nullable: true, blank: true)
		message(nullable: true, blank: true)
    }
}
