package com.councilsearch

class UrlMappings {

	static mappings = {
		// http://docs.grails.org/3.3.x/guide/theWebLayer.html#urlmappings
		// http://docs.grails.org/3.3.11/guide/theWebLayer.html#restfulMappings

//        delete "/$controller/$id(.$format)?"(action:"delete")
//        get "/$controller(.$format)?"(action:"index")
//        get "/$controller/$id(.$format)?"(action:"show")
//        post "/$controller(.$format)?"(action:"save")
//        put "/$controller/$id(.$format)?"(action:"update")
//        patch "/$controller/$id(.$format)?"(action:"patch")

		// Activity
		post "/api/guest/activity" (controller:"activity", action:"save")

		// Alert
		get "/api/alert" (controller:"alert", action:"list")
		get "/api/alert/$id" (controller:"alert", action:"show")
		get "/api/alert/process/$id" (controller:"alert", action:"process")
		post "/api/alert" (controller:"alert", action:"save")
		put "/api/alert" (controller:"alert", action:"update")
		delete "/api/alert/$id" (controller:"alert", action:"delete")

		// Comments
		post "/api/comment" (controller:"comment", action:"save")
		put "/api/comment" (controller:"comment", action:"update")

		// Documents
		get "/api/document/$uuid" (controller:"document", action:"show")
		get "/api/guest/document/reExtractContent/$uuid" (controller:"document", action:"reExtract")
		get "/api/guest/document/$uuid" (controller:"document", action:"show")
		get "/api/guest/s3/download/$uuid" (controller:"document", action:"download")
		get "/api/guest/document/reExtractDate" (controller: "document" , action: "reExtractDate")
		get "/api/guest/document/reDownloadToS3/$uuid" (controller: "document" , action: "reDownloadToS3")

		// ETL
		get "/api/guest/etl/start/$monitorId?" (controller:"extractTransferLoad", action:"start")
		get "/api/guest/etl/alert" (controller:"extractTransferLoad", action:"alerts")
		get "/api/guest/etl/event" (controller:"extractTransferLoad", action:"events")
		get "/api/guest/etl/notifications" (controller:"extractTransferLoad", action:"notifications")

		// Events
		get "/api/event" (controller:"event", action:"list")
		get "/api/event/$uuid" (controller:"event", action:"show")
		post "/api/event/$uuid/description" (controller:"event", action:"description")
		post "/api/event/$uuid/comment" (controller:"event", action:"comment")
		post "/api/event/$uuid/owner" (controller:"event", action:"owner")
		post "/api/event/$uuid/status" (controller:"event", action:"status")
		get "/api/event/$uuid/members" (controller:"event", action:"members")

		// Leads
		post "/api/guest/lead" (controller:"lead", action:"save")

		// Match
		get "/api/guest/match/${state}/${template}/${count}" (controller:"match", action:"list")

		// Monitor
		get "/api/guest/region/$regionId/monitor" (controller:"monitor", action:"list")
		get "/api/guest/monitor/$id" (controller:"monitor", action:"show")
		get "/api/guest/monitor/$id/process" (controller:"monitor", action:"process")
		post "/api/guest/region/$regionId/monitor" (controller:"monitor", action:"save")
		put "/api/guest/monitor" (controller:"monitor", action:"update")
		delete "/api/guest/monitor/$id" (controller:"monitor", action:"delete")

		// Region
		get "/api/guest/state/$abbr/regions" (controller:"region", action:"list")
		get "/api/regions/$id" (controller:"region", action:"show")
		get "/api/regions/create" (controller:"region", action:"error")
		post "/api/regions" (controller:"region", action:"error")
		get "/api/regions/$id/edit" (controller:"region", action:"error")
		put "/api/regions/$id" (controller:"region", action:"error")
		delete "/api/regions/$id" (controller:"region", action:"error")

		// Request
		post "/api/request/importio" (controller:"request", action:"importio")

		// Search
		post "/api/search" (controller:"search", action:"request")
		post "/api/guest/search" (controller:"search", action:"request")
		put "/api/guest/search/$id?" (controller:"search", action:"update")
		delete "/api/search/$id?" (controller:"search", action:"delete")

		// Sitemap
		get "/api/sitemap/$fileName" (controller: "sitemap", action:"view")
		put "/api/guest/sitemap" (controller: "sitemap", action:"update")
		delete "/api/sitemap" (controller: "sitemap", action:"delete")

		// State
		get "/api/guest/state" (controller:"state", action:"list")
		get "/api/guest/state/$id" (controller:"state", action:"show")

		// Suggestions
		get "/api/guest/suggest/$word/$count" (controller:"search", action:"suggest")
		get "/api/suggest/$word/$count" (controller:"search", action:"suggest")

		// User
		get "/api/user" (controller:"user", action:"list")
		get "/api/user/$id" (controller:"user", action:"show")
		delete "/api/user/$id" (controller:"user", action:"delete")

		"/"(controller: 'application', action:'index')
		"500"(view: '/error')
		"404"(view: '/notFound')
	}
}
