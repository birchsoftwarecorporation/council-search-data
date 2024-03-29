package com.councilsearch

import com.councilsearch.search.Request
import com.councilsearch.search.Response
import grails.converters.JSON
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse

class SearchController {
	def searchService
	def queryService
	def activityService

    def request() {
		def sReqJson = request.JSON
		Request searchRequest
		Response searchResult

		log.info("Requesting Search results for JSON: ${sReqJson}")

		// Record the activity
		activityService.create("search", sReqJson.toString(), request.getHeader("X-Real-IP"))

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
			searchService.index()
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

		// Record the activity
		activityService.create("suggest", word, request.getHeader("X-Real-IP"))

		Integer count = params.count?.toInteger() ?: 10
		List suggestions = queryService.suggestions(word)

		render suggestions.take(count) as JSON
	}
}
