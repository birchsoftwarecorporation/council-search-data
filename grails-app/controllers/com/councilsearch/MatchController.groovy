package com.councilsearch


import grails.rest.*
import grails.converters.*

import java.time.format.DateTimeFormatter

class MatchController {
	static responseFormats = ['json', 'xml']

	def matchService

	// get "/api/match/${state}/${template}/${count}" (controller:"match", action:"list")
    def list() {
		String stateAbbr = params.state
		String template = params.template
		int count = params.int('count')
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")

		User marketing = User.findByUsername("marketing@councilsearch.com")

		List matchesJSON = []

		matchService.listRecent(marketing, stateAbbr, template, count).each{ match ->
			Map matchMap = [:]

			matchMap.put("id", match.id)
			matchMap.put("documentTitle", match.document.title ?: "No Title")
			matchMap.put("meetingDate", dtf.format(match.document.meetingDate))
			matchMap.put("city", match.document.monitor.region.name)
			matchMap.put("stateAbbr", match.document.monitor.region.state.abbr)
			matchMap.put("preview", match.previews?.first()?.text ?: "Could not generate preview")

			matchesJSON.add(matchMap)
		}

		render matchesJSON as JSON
	}
}
