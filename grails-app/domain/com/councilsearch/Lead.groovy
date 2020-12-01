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

	String toString(){
		def message = "Name: ${this.name}<br>\n"
		message += "Sent: ${this.dateCreated}<br>\n"
		message += "Company: ${this.company}<br>\n"
		message += "Email: ${this.email}<br>\n"
		message += "Phone: ${this.phone}<br>\n"
		message += "Type: ${this.type}<br>\n"
		message += "Message: ${this.message}<br>\n"

		return message
	}


}
