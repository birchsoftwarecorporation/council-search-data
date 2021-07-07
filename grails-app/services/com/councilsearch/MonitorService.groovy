package com.councilsearch

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import java.sql.SQLException
import java.time.Duration
import java.time.Instant

@Transactional
class MonitorService {

	def queryService

	def create(def regionId, def monitorJSON) throws Exception {
		log.info("Creating Monitor for Region:${regionId}...")
		Monitor monitor

		if(monitorJSON == null){
			throw new Exception("Cannot create Monitor JSON data not found")
		}

		// Find the region
		Region region = Region.get(regionId)

		if(!region){
			log.error("Could not create Monitor. Region:${regionId} was not found")
			throw new Exception("Could not create Monitor. Parent Region not found")
		}

		monitor = new Monitor(monitorJSON)

		// Assoc the new monitor with the region
		region.addToMonitors(monitor)

		if(!region.save()){
			log.error("Failed to add Monitor to Region:${region.id} "+region.errors)
			throw new Exception("Could not save Monitor to Region")
		}

		// Save the monitor
		if(!monitor.save()){
			log.error("Could not create Monitor for Region:${region.id} "+monitor.errors)
			throw new Exception("Could not create Monitor")
		}

		return monitor
	}

	def update(def monitorJSON) throws Exception {
		if(!monitorJSON){
			throw new Exception("Cannot update Monitor JSON data not found")
		}

		log.info("Updating Monitor:${monitorJSON?.id}")

		// Find the Monitor
		Monitor monitor = Monitor.get(monitorJSON?.id)

		if(!monitor){
			log.error("Could not update Monitor. Monitor:${monitorJSON?.id} was not found")
			throw new Exception("Could not update Monitor. Monitor not found")
		}

		monitor.url = monitorJSON.url
		monitor.status = monitorJSON.status
		monitor.hashDedup = monitorJSON.hashDedup
		monitor.proxy = monitorJSON.proxy
		monitor.sslVersion = monitorJSON.sslVersion
		monitor.userAgent = monitorJSON.userAgent
		monitor.cookie = monitorJSON.cookie
		monitor.notes = monitorJSON.notes
		monitor.urlsXPath = monitorJSON.urlsXPath
		monitor.rowXPath = monitorJSON.rowXPath
		monitor.titleXPath = monitorJSON.titleXPath
		monitor.titleRegex = monitorJSON.titleRegex
		monitor.dateXPath = monitorJSON.dateXPath
		monitor.dateRegex = monitorJSON.dateRegex
		monitor.agendaXPath = monitorJSON.agendaXPath
		monitor.agendaContentXPath = monitorJSON.agendaContentXPath
		monitor.agendaSupplementXPath = monitorJSON.agendaSupplementXPath
		monitor.minuteXPath = monitorJSON.minuteXPath
		monitor.minuteContentXPath = monitorJSON.minuteContentXPath
		monitor.minuteSupplementXPath = monitorJSON.minuteSupplementXPath

		// Save the monitor
		if(!monitor.save()){
			log.error("Could not update Monitor for Region:${monitorJSON?.id} "+monitor.errors)
			throw new Exception("Could not update Monitor")
		}

		log.info("Completed Monitor:${monitorJSON?.id} update successfully")

		return monitor
	}

	def delete(def id){
		log.info("Deleteing Monitor:${id}")

		// Find the Monitor
		Monitor monitor = Monitor.get(id)

		if(!monitor){
			log.error("Could not delete Monitor. Monitor:${id} was not found")
			throw new Exception("Could not delete Monitor. Monitor not found")
		}

		Region region = monitor.region
		region.removeFromMonitors(monitor) // auto deletes orphans
	}

	@NotTransactional
	def process(def id) throws SQLException, IOException, FailingHttpStatusCodeException, MalformedURLException{
		List<Map> docMaps = []

		// Make sure it exists
		if(!Monitor.exists(id)){
			log.error("Could not process Monitor. Monitor:${id} was not found")
			throw new Exception("Could not process Monitor. Monitor not found")
		}

		List results = queryService.monitorById(id)

		if(results != null && results.size() > 0){
			docMaps = process(results?.get(0))
		}else{
			log.info("No query results for Monitor:${id}")
		}

		return docMaps
	}

	@NotTransactional
	def process(Map monitorMap) throws IOException, FailingHttpStatusCodeException, MalformedURLException{
		log.info("Processing Monitor:${monitorMap.monitorId}")
		Instant start = Instant.now()
		log.info("Extracting website: ${monitorMap?.url}")
		List<Map> docMaps = []
		WebClient client = WebClientHelper.buildClient()

		try{
			//  1 - Request Page
			HtmlPage page = (HtmlPage) client.getPage(monitorMap?.url)
			client.waitForBackgroundJavaScriptStartingBefore(60000)
			log.info("Loaded website: ${monitorMap?.url}")

			List rows = page.getByXPath(monitorMap?.rowXPath)?.take(25)
			Iterator rowItr = rows.iterator()
			int i = 1

			log.info("Found ${rows.size()} rows")

			while(rowItr.hasNext()){
				def row = rowItr.next()
				Map agendaDocMap = [:]
				Map minutesDocMap = [:]
				def title
				def dateStr
				URL agendaURL
				URL minutesURL

				// Extract title
				if(monitorMap?.titleXPath != null && !"".equals(monitorMap?.titleXPath)){
					title = row.getFirstByXPath(monitorMap?.titleXPath)?.getTextContent()
				}

				// Extract dateStr
				if(monitorMap?.dateXPath != null && !"".equals(monitorMap?.dateXPath)){
					dateStr = row.getFirstByXPath(monitorMap?.dateXPath)?.getTextContent()
				}

				// Extract Agenda URL
				if(monitorMap?.agendaXPath != null && !"".equals(monitorMap?.agendaXPath)){
					// 1 - Link
					HtmlAnchor anchor = row.getFirstByXPath(monitorMap?.agendaXPath)

					// FYI - works well but experiment HTMLUnit API
					// if there is a url, calc the full
					if(anchor?.getHrefAttribute() != null && !"".equals(anchor.getHrefAttribute()?.trim())){
						agendaURL = anchor?.getTargetUrl(anchor?.getHrefAttribute(), page)
						// TODO 2 - Weird script regex
					}
				}

				// Process Minute
				if(monitorMap?.minuteXPath != null && !"".equals(monitorMap?.minuteXPath)){
					// 1 - Link
					HtmlAnchor anchor = row.getFirstByXPath(monitorMap?.minuteXPath)

					// FYI - works well but experiment HTMLUnit API
					// if there is a url, calc the full
					if(anchor?.getHrefAttribute() != null && !"".equals(anchor.getHrefAttribute()?.trim())){
						minutesURL = anchor?.getTargetUrl(anchor?.getHrefAttribute(), page)
						// TODO 2 - Weird script regex
					}
				}

				// Add the agenda doc map data
				if(agendaURL != null) {
					// Basic info
					agendaDocMap.put("stateName", monitorMap.stateName)
					agendaDocMap.put("regionId", monitorMap.regionId)
					agendaDocMap.put("regionName", monitorMap.regionName)
					agendaDocMap.put("monitorId", monitorMap.monitorId)
					agendaDocMap.put("sslVersion", monitorMap.sslVersion)
					agendaDocMap.put("userAgent", monitorMap.userAgent)
					agendaDocMap.put("cookie", monitorMap.cookie)
					// Details
					agendaDocMap.put("title", title?.replaceAll("\\n", " ")?.replaceAll("\\s{2,}"," ")?.trim())
					agendaDocMap.put("dateStr", dateStr?.replaceAll("\\n", " ")?.replaceAll("\\s{2,}"," ")?.trim())
					agendaDocMap.put("documentType", "com.councilsearch.Agenda")
					agendaDocMap.put("url", agendaURL?.toString())
					docMaps.add(agendaDocMap)
				}

				// Add the agenda doc map data
				if(minutesURL != null) {
					// Basic info
					minutesDocMap.put("stateName", monitorMap.stateName)
					minutesDocMap.put("regionId", monitorMap.regionId)
					minutesDocMap.put("regionName", monitorMap.regionName)
					minutesDocMap.put("monitorId", monitorMap.monitorId)
					minutesDocMap.put("sslVersion", monitorMap.sslVersion)
					minutesDocMap.put("userAgent", monitorMap.userAgent)
					minutesDocMap.put("cookie", monitorMap.cookie)
					// Details
					minutesDocMap.put("title", title?.replaceAll("\\n", " ")?.replaceAll("\\s{2,}"," ")?.trim())
					minutesDocMap.put("dateStr", dateStr?.replaceAll("\\n", " ")?.replaceAll("\\s{2,}"," ")?.trim())
					minutesDocMap.put("documentType", "com.councilsearch.Minute")
					minutesDocMap.put("url", minutesURL?.toString())
					docMaps.add(minutesDocMap)
				}

				// TODO - Agenda Supplemental links
				// TODO - Agenda/Minute Content
				i++
			}
		}finally{
			client?.close()
			Instant end = Instant.now()
			log.info("Completed extraction in ${Duration.between(start, end).toMillis()} ms for Monitor:${monitorMap.monitorId} website:${monitorMap?.url} finding: ${docMaps.size()}")
		}

		return docMaps
	}

}