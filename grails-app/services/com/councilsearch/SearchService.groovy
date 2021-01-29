package com.councilsearch

import com.councilsearch.search.Request
import com.councilsearch.search.Response
import grails.gorm.transactions.NotTransactional
import groovy.sql.Sql
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.hibernate.Hibernate
import org.springframework.beans.factory.InitializingBean

class SearchService implements InitializingBean {
	static transactional = false

	def dataSource

	def queryService

	String SOLR_BASE_URL
	String SOLR_PORT
	String SOLR_CLUSTER_NAME
	Integer SEARCH_INDEX_BATCH_SIZE
	HttpSolrClient SOLR_CLIENT_REQUEST
	ConcurrentUpdateSolrClient SOLR_CLIENT_UPDATE

	public void afterPropertiesSet() throws Exception {
		SOLR_BASE_URL = CustomConfig.findByName("SOLR_BASE_URL")?.getValue() ?: "http://localhost"
		SOLR_PORT = CustomConfig.findByName("SOLR_PORT")?.getValue() ?: "8983"
		SOLR_CLUSTER_NAME = CustomConfig.findByName("SOLR_CLUSTER_NAME")?.getValue() ?: "documents"
		SEARCH_INDEX_BATCH_SIZE = CustomConfig.findByName("SEARCH_INDEX_BATCH_SIZE")?.getValue() as Integer ?: 50

		try{
			log.info("Building SOLR request client")
			SOLR_CLIENT_REQUEST = new HttpSolrClient.Builder("${SOLR_BASE_URL}:${SOLR_PORT}/solr")
					.withConnectionTimeout(10000)
					.withSocketTimeout(60000)
					.build()
		}catch(Exception e){
			log.error("Could not connect to Solr Index with url: ${SOLR_BASE_URL}:${SOLR_PORT}/solr")
		}

		try{
			log.info("Building SOLR update client")
			SOLR_CLIENT_UPDATE = new ConcurrentUpdateSolrClient.Builder("${SOLR_BASE_URL}:${SOLR_PORT}/solr")
					.withConnectionTimeout(10000)
					.withSocketTimeout(60000)
					.build()
		}catch(Exception e){
			log.error("Could not connect to Solr Index with url: ${SOLR_BASE_URL}:${SOLR_PORT}/solr")
		}
	}

	@NotTransactional
	def update(def id){
		log.info("Updating search index for Document:${id}")
		UpdateResponse ur

		if(!id){
			log.error("Can't update search index for Document with no Id")
			return ur
		}

		Document doc = Document.get(id)

		if(!doc){
			log.error("Can't update search index with Document:${id}")
			return ur
		}

		try{
			SolrInputDocument sDoc = new SolrInputDocument()

			sDoc.addField("id", doc.id)
			sDoc.addField("state", doc.monitor.region.state.name)
			sDoc.addField("region_id", doc.monitor.region.id)
			sDoc.addField("region", doc.monitor.region.name)
			sDoc.addField("region_type", Hibernate.getClass(doc.monitor.region).getName()?.replace("com.councilsearch.", "").toLowerCase())
			sDoc.addField("monitor_id", doc.monitor.id)
			sDoc.addField("title", doc.title)
			sDoc.addField("document_type", Hibernate.getClass(doc).getName()?.replace("com.councilsearch.","").toLowerCase())
			sDoc.addField("meeting_date", doc.meetingDate)
			sDoc.addField("date_created", doc.dateCreated)
			sDoc.addField("uuid", doc.uuid)
			sDoc.addField("content", doc.content.text ?: "")

			ur = SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)

			// Indexed documents must be committed
			SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
		}catch(Exception e){
			log.error("Could not perform update id:${id} - "+e)
		}

		return ur
	}

	def update(){
		log.info("Fully updating Solr Index")
		int offset = 0
		def maxId = queryService.getMaxDocumentId()

		// Calculate batch number
		log.info("Found a max document Id: ${maxId}")

		// Use maxId (or row) to calculate offset
		while(offset < maxId){
			List docMaps = queryService.getAllSearchDocuments(offset, SEARCH_INDEX_BATCH_SIZE)
			Iterator dataItr = docMaps.iterator()

			// Grab the list iterator and go through the maps
			while(dataItr.hasNext()){
				Map docMap = dataItr.next()
				// Add the doc to index queue
				solrAddDocument(docMap)
				// Remove it when we are done
				dataItr.remove()
			}

			try{
				// Commit the batch
				SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
			}catch(Exception e){
				log.error("Could not commit batch ${offset}")
			}

			// Increase for the next round
			offset += SEARCH_INDEX_BATCH_SIZE
		}

		log.info("Finished fully updating Solr Index")
	}

	def solrAddDocument(Map docMap){
		def docId = docMap.get("documentId")

		// Running into MySQL OutOfMemory Exception dont know how
		String text = queryService.getContentByDocId(docId)

		try{
			SolrInputDocument sDoc = new SolrInputDocument()

			sDoc.addField("id", docId)
			sDoc.addField("state", docMap.get("stateName"))
			sDoc.addField("state_abbr", docMap.get("stateAbbr"))
			sDoc.addField("region_id", docMap.get("regionId"))
			sDoc.addField("region", docMap.get("regionName"))
			sDoc.addField("region_type", docMap.get("regionType"))
			sDoc.addField("monitor_id", docMap.get("monitorId"))
			sDoc.addField("title", docMap.get("title") ?: "No Title")
			sDoc.addField("document_type", docMap.get("documentType").replace("com.councilsearch.",""))
			sDoc.addField("meeting_date", docMap.get("meetingDate"))
			sDoc.addField("date_created", docMap.get("dateCreated"))
			sDoc.addField("uuid", docMap.get("uuid"))
			sDoc.addField("content", text)

			SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)
		}catch(Exception e){
			log.error("Could not perform Solr update for batch "+e)
		}
	}


	def delete(String id){
		UpdateResponse ur

		try{
			if(id != null){
				log.info("Delete Search document with Id ${id}")
				SOLR_CLIENT_REQUEST.deleteById(SOLR_CLUSTER_NAME, id)
			}else{
				log.info("No Id specified clearing Solr Cluster ${SOLR_CLUSTER_NAME}")
				SOLR_CLIENT_REQUEST.deleteByQuery(SOLR_CLUSTER_NAME, "*:*")
			}

			ur = SOLR_CLIENT_REQUEST.commit(SOLR_CLUSTER_NAME)
		}catch(Exception e){
			log.error("Could not perform delete: "+e)
		}

		return ur
	}

	def bulkIndex(List docMaps){
		Iterator<Map> dItr = docMaps.iterator()

		while(dItr.hasNext()) {
			Map docMap = dItr.next()

			// Dont upload to Solr if the processing didnt end with a success
			if(!docMap.get("success")){
				continue
			}

			try{
				def id = docMap.get("id") // mandatory solr field
				if(id != null){
					SolrInputDocument sDoc = new SolrInputDocument()
					sDoc.addField("id", id)
					sDoc.addField("state", docMap.get("stateName"))
					sDoc.addField("state_abbr", docMap.get("stateAbbr"))
					sDoc.addField("region_id", docMap.get("regionId"))
					sDoc.addField("region", docMap.get("regionName"))
					sDoc.addField("region_type", docMap.get("regionType"))
					sDoc.addField("monitor_id", docMap.get("monitorId"))
					sDoc.addField("title", docMap.get("title") ?: "No Title")
					sDoc.addField("document_type", docMap.get("docType").replace("com.councilsearch.",""))
					sDoc.addField("meeting_date", docMap.get("date"))
					sDoc.addField("uuid", docMap.get("uuid"))
					sDoc.addField("date_created", new Date())
					sDoc.addField("content", docMap.get("content") ?: "")

					SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)
				}else{
					log.error("Cannot update Solr index with null Id")
				}
			}catch(Exception e){
				log.error("Could not bulk ETL index: "+e)
			}
		}

		// Indexed documents must be committed
		SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
	}


	QueryResponse request(SolrQuery solrQuery){
		QueryResponse queryResponse

		try {
			queryResponse = SOLR_CLIENT_REQUEST.query(SOLR_CLUSTER_NAME, solrQuery)
		} catch (Exception e) {
			log.error("Could not query Solr"+ e)
		}

		return queryResponse
	}

}
