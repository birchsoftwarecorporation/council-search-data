package com.councilsearch

import grails.gorm.transactions.NotTransactional
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.springframework.beans.factory.InitializingBean

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SearchService implements InitializingBean {

	def queryService

	String SOLR_BASE_URL
	String SOLR_PORT
	String SOLR_CLUSTER_NAME
	Integer SEARCH_INDEX_BATCH_SIZE
	HttpSolrClient SOLR_CLIENT_REQUEST
	ConcurrentUpdateSolrClient SOLR_CLIENT_UPDATE
	DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
	def update(def docId){
		log.info("Updating search index for Document:${docId}")
		UpdateResponse ur

		if(!docId){
			log.error("Can't update search index for Document with no Id")
			return ur
		}

		Map docMap = queryService.getSearchDocument(docId)

		try{
			solrAddDocument(docMap)
			SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
		}catch(Exception e){
			log.error("Could not perform update id:${docId} - "+e)
		}

		return ur
	}

	@NotTransactional
	def index(){
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

	@NotTransactional
	def etlIndex(List docMaps){
		Iterator<Map> dataItr = docMaps.iterator()

		while(dataItr.hasNext()) {
			Map docMap = dataItr.next()

			// Dont upload to Solr if the processing didnt end with a success
			if(!docMap.get("success")){
				continue
			}
			// Add the doc to index queue
			solrAddDocument(docMap)
		}

		// Indexed documents must be committed
		SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
	}

	@NotTransactional
	def solrAddDocument(Map docMap) {
		String docId = docMap.get("documentId")
		String content

		// Data for this can come from ETL or DB if ETL we dont need to do the DB called should exist
		if (docMap.containsKey("content")) {
			content = docMap.get("content") ?: ""
		}else{
			content = queryService.getContentByDocId(docId) ?: ""
		}

		log.debug("Adding Document:${docId} to Solr Index")

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
			sDoc.addField("meeting_date", DATE_FORMATTER.format(docMap.get("meetingDate")))
			sDoc.addField("date_created", DATE_FORMATTER.format(docMap.get("dateCreated")))
			sDoc.addField("uuid", docMap.get("uuid"))
			sDoc.addField("content", content)

			SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)
		}catch(Exception e){
			log.error("Could not add Document:${docId} to Solr "+e)
		}
	}

	@NotTransactional
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

	@NotTransactional
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
