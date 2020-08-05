package com.councilsearch

class Organization {

	String name
	List employees = []

	Organization(String name) {
		this()
		this.name = name
	}

    static constraints = {
		name(nullable: false, blank: false, unique: true)
    }

	static hasMany = [employees: User]
}
