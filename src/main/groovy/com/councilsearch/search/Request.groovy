package com.councilsearch.search

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.ORDER

class Request {
	// Query selections
	String q
	Integer offset
	Integer rows
	Boolean exactMatch

	// Filter selections
	List<String> documentType
	List<String> regionType
	List<String> region

	// Date filter
	String startDateStr
	String endDateStr
	Date startDate
	Date endDate

	// Sort
	String sort

	// Defaults
	List responseFields
	Boolean toHighlight = true
	List searchFields = ["content"]
	List highlightFields = ["content"]
	Integer snippetNumber = 1
	Integer highlightSize = 2000
	Integer maxAnalyzedChars = -1
	String highlightWrapStart = "<span class=\"highlight\">"
	String highlightWrapStop = "</span>"
	Boolean toFacet = true
	Integer minCount = 1
	List facetFields = []
	List multiFacetFields = ["document_type", "region_type", "region"]

	Request(){ }

	void setStartDateStr(String dateStr){
		if(dateStr != null && !"".equalsIgnoreCase(dateStr)){
			this.startDate = parseDate(dateStr, "yyyy-MM-dd'T'HH:mm:ss")
		}
	}

	void setEndDateStr(String dateStr){
		if(dateStr != null && !"".equalsIgnoreCase(dateStr)){
			this.endDate = parseDate(dateStr, "yyyy-MM-dd'T'HH:mm:ss")
		}
	}

	Date parseDate(String dateStr, String format){
		Date date

		if(dateStr != null && format != null && !"".equals(dateStr) && !"".equals(format)) {
			try {
				date = Date.parse(format, dateStr)
			} catch (Exception e) {
				// TODO - Add logging to pojo
				println("Could not parse date:${dateStr} with format: ${format} " + e)
			}
		}

		return date
	}

	SolrQuery createSolrQuery(){
		SolrQuery query = new SolrQuery()

		// Do the must match
		if(this.q != null && !"".equalsIgnoreCase(this.q) && this.exactMatch){
			this.q = "\"${this.q.replaceAll("\"","")}\"" // remove any quotes the user typed in to add our own
		}

		// Set the query
		query.setQuery(this.q ?: "*:*")
			// Set the paging
			.setStart(this.offset ?: 0)
			// Set the row max
			.setRows(this.rows ?: 10)
			// Set the return fields
			.setFields(this.responseFields?.join(","))
			// Set the default search text field
			.setParam("df", this.searchFields?.join(","))
			// Set Highlighting
			.setHighlight(this.toHighlight ?: false)
			.setHighlightSnippets(this.snippetNumber ?: 1)
			.setParam("hl.fl", this.highlightFields?.join(",") ?: "content")
			.setParam("hl.maxAnalyzedChars", maxAnalyzedChars as String ?: "51200")
			.setParam("hl.mergeContiguous", "true")
			.setParam("hl.fragsize", this.highlightSize as String ?: "500")
			.setParam("hl.simple.pre", this.highlightWrapStart ?: "<b>")
			.setParam("hl.simple.post", this.highlightWrapStop ?: "</b>")
			// Add Facets
			.setFacet(true)
			.setFacetMinCount(1)

		// Lets tell the Index which facets too expect
//		this.facetFields?.each { singleFacetField ->
//			query.addFacetField(singleFacetField)
//		}

		// Add - multi
		this.multiFacetFields?.each { multiFacetField ->
			query.addFacetField("{!ex=${multiFacetField}}${multiFacetField}")
		}

		// Add filters selections - Multi
		if(documentType?.size() > 0){
			query.addFilterQuery("{!tag=document_type}document_type:(${documentType?.join(" ")})")
		}
		if(regionType?.size() > 0){
			query.addFilterQuery("{!tag=region_type}region_type:(${regionType?.join(" ")})")
		}
		if(region?.size() > 0){
			query.addFilterQuery("{!tag=region}region:(${region?.join(" ")})")
		}

		// Add date string
		if(this.startDate != null && this.endDate != null && this.startDate.compareTo(this.endDate) < 0){
			query.addFilterQuery(createSolrDateFilter())
		}

		// Add sort - default is upcoming at top
		if("Most Relevant".equalsIgnoreCase(sort)){
			// No sorting
		}else{
			// Default is to show latest
			query.addSort("meeting_date", ORDER.desc)
		}

		return query
	}

	String createSolrDateFilter(){
		return "meeting_date:[${this.startDate.format("yyyy-MM-dd'T'HH:mm:ss'Z'")} TO ${this.endDate.format("yyyy-MM-dd'T'HH:mm:ss'Z'")}]"
	}
}
