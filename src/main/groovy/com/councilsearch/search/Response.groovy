package com.councilsearch.search

import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList

class Response {
	Integer total = 0
	// Results
	List<Result> results

	// Facets
	List<Filter> regionType
	List<Filter> region
	List<Filter> documentType

	Response(QueryResponse solrResponse){
		// Init vars
		this.total = 0
		results = []
		regionType = []
		region = []
		documentType = []

		buildResults(solrResponse)
		buildFilters(solrResponse)
	}

	private buildResults(QueryResponse solrResponse){
		if(solrResponse != null){
			SolrDocumentList solrDocList = solrResponse.getResults()
			// Set the total results
			this.total = solrDocList?.getNumFound()

			// Build our simplified docs
			for(SolrDocument sDoc : solrDocList){
				Result result = new Result()
				String id = sDoc.getFieldValue("id")
				result.regionId = sDoc.getFieldValue("region_id") as Long
				result.state = sDoc.getFieldValue("state")
				result.regionName = sDoc.getFieldValue("region")
				result.regionType = sDoc.getFieldValue("region_type")
				result.title = sDoc.getFieldValue("title")
				result.documentType = sDoc.getFieldValue("document_type")?.replace("com.councilsearch.","")
				result.meetingDate = sDoc.getFieldValue("meeting_date")
				result.uuid = sDoc.getFieldValue("uuid")
				result.text = solrResponse.getHighlighting()?.get(id)?.get("content")?.get(0) ?: sDoc.getFieldValue("content")?.get(0)?.take(2000)

				this.results.add(result)
			}
		}
	}

	private buildFilters(QueryResponse solrResponse){
		// TODO - getLimitingFacets for singles
		// Multi select filers
		solrResponse?.getFacetFields()?.each{ solrFacetField ->
			if("document_type".equalsIgnoreCase(solrFacetField.getName())){
				// Add the filters of the Facet
				solrFacetField?.getValues()?.each { val ->
					this.documentType.add(new Filter(val.getName(), val.getCount()))
				}
			}else if("region_type".equalsIgnoreCase(solrFacetField.getName())) {
				// Add the filters of the Facet
				solrFacetField?.getValues()?.each { val ->
					this.regionType.add(new Filter(val.getName(), val.getCount()))
				}
			}else if("region".equalsIgnoreCase(solrFacetField.getName())){
				// Add the filters of the Facet
				solrFacetField?.getValues()?.each { val ->
					this.region.add(new Filter(val.getName(), val.getCount()))
				}
			}
		}

		// Sort the results
		this.documentType.sort{ it.name }
		this.regionType.sort{ it.name }
		this.region.sort{ it.name }
	}

}
