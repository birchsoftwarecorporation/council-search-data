package com.councilsearch

import com.councilsearch.search.Request
import com.councilsearch.search.Response
import grails.converters.JSON
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse

class SearchController {
	def searchService
	def queryService

    def request() {
		def sReqJson = request.JSON
		Request searchRequest
		Response searchResult

		log.info("Requesting Search results for JSON: ${sReqJson}")

		// Record the activity
//		Activity activity = new Activity(name: "search", details: sReqJson.toString(), ipAddress: request.getRemoteAddr())
//		activity.save()

		try{
			searchRequest = new Request(sReqJson)
			QueryResponse queryResponse = searchService.request(searchRequest.createSolrQuery())
			searchResult = new Response(queryResponse)
		}catch(Exception e){
			log.error("Could not process search request: "+ e)
			response.status = 400
			render ([error: "Could not process search request"] as JSON)
			return
		}

		render searchResult as JSON
	}

	def update(){
		def id = params.id

		if(id != null){
			searchService.update(id)
			render ([status: "Updated Document:${id}"] as JSON)
			return
		}else{
			searchService.update()
			render ([status: "Updated all documents"] as JSON)
			return
		}
	}

	def delete(){
		def id = params.id
		UpdateResponse ur = searchService.delete(id)

		if(ur == null){
			response.status = 400
			render ([error: "Could not perform delete"] as JSON)
			return
		}

		render ([status: "Search index status: ${ur.getStatus()}"] as JSON)
	}

	def suggest(){
		def word = params.word
		Integer count = params.count?.toInteger() ?: 10
		List suggestions = queryService.suggestions(word)

		render suggestions.take(count) as JSON
	}

//	def tika(){
//		File file = new File("C:\\birch-home\\gig\\PiaseckiT-Email.pst")
//		log.info("Starting...")
//		searchService.processPSTFile(file)
//		log.info("End")
//
//		render ([status: "Finished"] as JSON)
//	}

}
