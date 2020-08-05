package com.councilsearch

import grails.gorm.transactions.NotTransactional
import groovy.sql.Sql

class QueryService {
	def dataSource

	static transactional = false

	@NotTransactional
	def monitorById(def monitorId) {
		Sql sql = new Sql(dataSource)
		List monitor = []
		String query = """
			SELECT 
				s.name stateName,
				s.abbr stateAbbr,
				r.id regionId,
				r.name regionName,
				replace(r.class, "com.councilsearch.", "") regionType,
				m.id monitorId,
				m.extractor_id extractorId,
				m.hash_dedup hashDedup,
				m.ssl_version sslVersion,
				m.user_agent userAgent,
				m.cookie
			FROM
				monitor m 
				left join region r on r.id = m.region_id
				left join state s on s.id = r.state_id
			WHERE
				m.id = :monitorId
		"""

		try{
			monitor = sql.rows(query, [monitorId: monitorId])
		}catch(Exception e){
			log.error("Could not query for Monitor with Id: ${monitorId} - "+ e)
		}finally{
			sql.close()
		}

		return monitor
	}

	@NotTransactional
    def monitorsByStatus(String status) {
		Sql sql = new Sql(dataSource)
		List monitors = []
		String query = """
			SELECT 
				s.name stateName,
				s.abbr stateAbbr,
				r.id regionId,
				r.name regionName,
				replace(r.class, "com.councilsearch.", "") regionType,
				m.id monitorId,
				m.extractor_id extractorId,
				m.hash_dedup hashDedup,
				m.ssl_version sslVersion,
				m.user_agent userAgent,
				m.cookie
			FROM
				monitor m 
				left join region r on r.id = m.region_id
				left join state s on s.id = r.state_id
			WHERE
				status = :status
		"""

		try{
			monitors = sql.rows(query, [status: status])
		}catch(Exception e){
			log.error("Could not query for Live Monitors: "+ e)
		}finally{
			sql.close()
		}

		return monitors
    }


	@NotTransactional
	void createDocuments(List docMaps) {
		Sql sql = new Sql(dataSource)
		Iterator<Map> dItr = docMaps.iterator()

		while(dItr.hasNext()) {
			Map docMap = dItr.next()

			String query = """
				INSERT INTO document (monitor_id, class, version, title, date_created, last_modified, 
									meeting_date_str, meeting_date, url, extracted, indexed, status_code, 
									content_type, location, hash, uuid, success, message)
				VALUES(:monitorId, :docType, 1, :title, now(), now(), 
					:dateStr, :meetingDate, :url, true, false, :statusCode, 
					:contentType, :location, :hash, :uuid, :success, :message);
			"""

			try{
				def results = sql.executeInsert(query, [monitorId: docMap.get("monitorId"),
														docType: docMap.get("docType"),
														title: docMap.get("title") ?: "No title",
														dateStr: docMap.get("dateStr"),
														meetingDate: docMap.get("date"),
														url: docMap.get("url"),
														statusCode: docMap.get("statusCode"),
														contentType: docMap.get("contentType"),
														location: docMap.get("location"),
														hash: docMap.get("hash"),
														uuid: docMap.get("uuid"),
														success: docMap.get("success"),
														message: docMap.get("message")])

				// Create content while the connection is open
				results.each{ cols ->
					if(cols.size() > 0){
						def docId = cols.get(0)

						if(docId != null && docId > 0){
							docMap.put("id", docId)
							String contentQuery = """
								INSERT into content(version, date_created, last_modified, document_id, text)
								VALUES(1, now(), now(), :docId, :text);
							"""

							try{
								sql.executeInsert(contentQuery, [docId: docId, text: docMap.get("content") ?: ""])
							}catch(Exception e){
								log.error("Could not create content for Document:${docId} - " + e)
							}
						}
					}
				}
			}catch(Exception e) {
				log.error("Could not create document: ${docMap.get("url")}" + e)
			}
		}

		sql.close()
	}

	@NotTransactional
	def suggestions(String word) {
		Sql sql = new Sql(dataSource)
		List suggestions = []

		String query = "SELECT name FROM suggestion WHERE name like \"${word}%\""

		try{
			sql.rows(query).each{ result ->
				Map sugMap = [:]
				sugMap.put("name", result.get("name"))
				suggestions.add(sugMap)
			}
		}catch(Exception e){
			log.error("Could not query for suggestions: "+ e)
		}finally{
			sql.close()
		}

		return suggestions
	}

	def getMaxDocumentId() {
		def id
		Sql sql = new Sql(dataSource)
		String query = """
			SELECT 
				id
			FROM
				document
			ORDER BY id DESC
		"""

		try {
			id = sql.rows(query, 0, 1)?.get(0)?.get("id")
		} catch (Exception e) {
			log.error("Could not query for Search Documents: " + e)
		} finally {
			sql.close()
		}

		return id

	}

	def getAllSearchDocuments(def offset, def maxRows) {
		log.info("Getting searchable data from database offset:${offset} maxRows:${maxRows}")
		Sql sql = new Sql(dataSource)
		List documents = []
		String query = """
			SELECT
				# Document
				d.id documentId,
				d.title title,
				replace(d.class, "com.councilsearch.", "") documentType,
				d.meeting_date meetingDate,
				d.date_created dateCreated,
				d.uuid uuid,
				c.text text,
				# Monitor
				m.id monitorId,
				# Region
				r.id regionId,
				r.name regionName,
				replace(r.class, "com.councilsearch.", "") regionType,
				# State
				s.name stateName,
				s.abbr stateAbbr
			FROM
				document d
				left join content c on d.id = c.document_id
				left join monitor m on m.id = d.monitor_id
				left join region r on r.id = m.region_id
				left join state s on s.id = r.state_id
		"""

		try {
			documents = sql.rows(query, offset, maxRows)
		} catch (Exception e) {
			log.error("Could not query for Search Documents: " + e)
		} finally {
			sql.close()
		}

		return documents
	}

	def getDocumentURLsByMonitorId(def monitorId){
		log.info("Getting urls for Monitor:${monitorId}")
		Sql sql = new Sql(dataSource)
		List urls = []
		String query = """
			SELECT 
				url
			FROM
				document
			WHERE
				monitor_id = :monitorId
		"""

		try {
			sql.rows(query, [monitorId: monitorId]).each{
				urls.push(it.get("url"))
			}
		} catch (Exception e) {
			log.error("Could not query for Document URLs by Monitor Id: " + e)
		} finally {
			sql.close()
		}

		return urls
	}

	// Trying to keep the connection open for this one
	def distinctMonitorIds(Sql sql) {
		List results = []
		String query = """
			select 
				distinct(d.monitor_id) monitorId
			from document d
			order by monitorId
		"""

		try{
			results = sql.rows(query)
		}catch(Exception e){
			log.error("Could not query for distinct Monitor ids"+ e)
		}

		return results
	}

	// Trying to keep the connection open for this one
	def getSitemapInfo(Sql sql, def monitorId) {
		List results = []
		String query = """
			select 
				s.name state,
				r.name region,
				d.date_created dateCreated,
				d.meeting_date meetingDate,
				replace(d.class, "com.councilsearch.", "") docType,
				d.uuid
			from document d
				left join monitor m on m.id = d.monitor_id
				left join region r on r.id = m.region_id
				left join state s on s.id = r.state_id
			where d.monitor_id = :monitorId
			and d.meeting_date is not null
		"""

		try{
			results = sql.rows(query, [monitorId: monitorId])
		}catch(Exception e){
			log.error("Could not query for latest Sitemap Information by MonitorId"+ e)
		}

		return results
	}

	def getAlertIdsByUser(def userId){
		log.info("Getting Alert ids by User")
		Sql sql = new Sql(dataSource)
		List alertIds = []
		String query = """
			SELECT 
				a.id alertId
			FROM
				alert a
			WHERE
				a.manager_id = :userId
		"""

		try {
			alertIds = sql.rows(query, [userId: userId])
		} catch (Exception e) {
			log.error("Could not query for Alert Ids by UserId: " + e)
		} finally {
			sql.close()
		}

		return alertIds
	}

	def getAlertIdsByMember(def userId){
		log.info("Getting Alert ids by Member")
		Sql sql = new Sql(dataSource)
		List alertIds = []
		String query = """
			SELECT 
				au.alert_members_id alertId
			FROM
				alert_user au
			WHERE
				au.user_id = :userId
		"""

		try {
			alertIds = sql.rows(query, [userId: userId])
		} catch (Exception e) {
			log.error("Could not query for Alert Ids by UserId: " + e)
		} finally {
			sql.close()
		}

		return alertIds
	}
}
