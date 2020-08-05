package com.councilsearch

class Monitor {
	String url
	String status
	String extractorId
	Date dateCreated
	Date lastUpdated
	Boolean hashDedup
	String sslVersion
	String userAgent
	String cookie
	String notes

	static belongsTo = [region: Region]
	static hasMany = [minutes: Minute, agendas: Agenda]

	static constraints = {
		url(nullable: false, blank: false)
		status(nullable: false, blank: false)
		extractorId(nullable: false, blank: false)
		hashDedup(nullable: true, blank: true)
		sslVersion(nullable: true, blank: true)
		userAgent(nullable: true, blank: true)
		cookie(nullable: true, blank: true)
		notes(nullable: true, blank: true)
	}
}
