package com.councilsearch

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Monitor {
	String url
	String status
	Date dateCreated
	Date lastUpdated
	String sslVersion
	String userAgent
	String cookie
	String notes

	// Config
	String importioId
	String importioName
	Boolean hashDedup
	// 	Pre step - find list of urls with real data from main url above
	String urlsXPath
	// 	Real config
	String rowXPath
	String titleXPath
	String titleRegex
	String dateXPath
	String dateRegex
	String agendaXPath
	String agendaContentXPath
	String agendaSupplementXPath
	String minuteXPath
	String minuteContentXPath
	String minuteSupplementXPath
	Boolean proxy

	static belongsTo = [region: Region]
	static hasMany = [minutes: Minute, agendas: Agenda]

	static constraints = {
		url(nullable: false, blank: false)
		status(nullable: false, blank: false)
		importioId(nullable: true, blank: true)
		importioName(nullable: true, blank: true)
		hashDedup(nullable: true)
		sslVersion(nullable: true, blank: true)
		userAgent(nullable: true, blank: true)
		cookie(nullable: true, blank: true)
		notes(nullable: true, blank: true)
		urlsXPath(nullable: true, blank: true)
		rowXPath(nullable: true, blank: true)
		titleXPath(nullable: true, blank: true)
		titleRegex(nullable: true, blank: true)
		dateXPath(nullable: true, blank: true)
		dateRegex(nullable: true, blank: true)
		agendaXPath(nullable: true, blank: true)
		agendaContentXPath(nullable: true, blank: true)
		agendaSupplementXPath(nullable: true, blank: true)
		minuteXPath(nullable: true, blank: true)
		minuteContentXPath(nullable: true, blank: true)
		minuteSupplementXPath(nullable: true, blank: true)
		proxy(nullable: true)
	}

	static {
		grails.converters.JSON.registerObjectMarshaller(Monitor) { Monitor monitor ->
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			Map monitorMap = [:]

			monitorMap.put("id", monitor.id)
			monitorMap.put("regionId", monitor.region.id)
			monitorMap.put("url", monitor.url)
			monitorMap.put("status", monitor.status)
			monitorMap.put("importioId", monitor.importioId)
			monitorMap.put("sslVersion", monitor.sslVersion)
			monitorMap.put("userAgent", monitor.userAgent)
			monitorMap.put("cookie", monitor.cookie)
			monitorMap.put("notes", monitor.notes)
			monitorMap.put("urlsXPath", monitor.urlsXPath)
			monitorMap.put("rowXPath", monitor.rowXPath)
			monitorMap.put("titleXPath", monitor.titleXPath)
			monitorMap.put("titleRegex", monitor.titleRegex)
			monitorMap.put("dateXPath", monitor.dateXPath)
			monitorMap.put("dateRegex", monitor.dateRegex)
			monitorMap.put("agendaXPath", monitor.agendaXPath)
			monitorMap.put("agendaContentXPath", monitor.agendaContentXPath)
			monitorMap.put("agendaSupplementXPath", monitor.agendaSupplementXPath)
			monitorMap.put("minuteXPath", monitor.minuteXPath)
			monitorMap.put("minuteContentXPath", monitor.minuteContentXPath)
			monitorMap.put("minuteSupplementXPath", monitor.minuteSupplementXPath)
			monitorMap.put("proxy", monitor.proxy)
			monitorMap.put("hashDedup", monitor.hashDedup)
			// Need to upgrade Date -> LocalDate
//			monitorMap.put("dateCreated", monitor.dateCreated.format(formatter))
//			monitorMap.put("lastUpdated", monitor.lastUpdated.format(formatter))

			return monitorMap
		}
	}
}
