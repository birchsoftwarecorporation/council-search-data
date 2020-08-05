package com.councilsearch

class Document {
	String title
	Date meetingDate
	String url
	String meetingDateStr
	String hash
	Date dateCreated
	Date lastModified
	Integer statusCode
	String message
	String contentType
	String location // finalLocation
	Boolean extracted
	Boolean indexed
	Boolean success
	String uuid

	static belongsTo =  [monitor: Monitor]
	static hasOne = [content: Content]

	static constraints = {
		title(nullable: false, blank: false)
		meetingDate(nullable: true, blank: true)
		url(nullable: true, blank: true)
		meetingDateStr(nullable: true, blank: true)
		hash(nullable: true, blank: true)
		message(nullable: true, blank: true)
		contentType(nullable: true, blank: true)
		location(nullable: true, blank: true)
		statusCode(nullable: true, blank: true)
		extracted(nullable: true, blank: true)
		indexed(nullable: true, blank: true)
		uuid(nullable: false, blank: false, unique: true)
	}

	static mapping = { }

}
