package com.councilsearch

class Preview {
	Match match
	String phraseHit
	String text

	static belongsTo = Match

	static constraints = {
		match(nullable: false, blank: false)
		phraseHit(nullable: false, blank: false)
		text(nullable: false, blank: false)
	}

	static mapping = {
		text(sqlType: "text")
	}
}
