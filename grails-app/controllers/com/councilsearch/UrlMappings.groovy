package com.councilsearch

class UrlMappings {

    static mappings = {
//        delete "/$controller/$id(.$format)?"(action:"delete")
//        get "/$controller(.$format)?"(action:"index")
//        get "/$controller/$id(.$format)?"(action:"show")
//        post "/$controller(.$format)?"(action:"save")
//        put "/$controller/$id(.$format)?"(action:"update")
//        patch "/$controller/$id(.$format)?"(action:"patch")

		// Alert
		get "/api/alert" (controller:"alert", action:"index")
		get "/api/alert/$id" (controller:"alert", action:"show")
		post "/api/alert" (controller:"alert", action:"save")
		put "/api/alert" (controller:"alert", action:"update")
		delete "/api/alert/$id" (controller:"alert", action:"delete")

		get "/api/guest/alert" (controller:"alert", action:"index")
		get "/api/guest/alert/$id" (controller:"alert", action:"show")
		post "/api/guest/alert" (controller:"alert", action:"save")
		put "/api/guest/alert" (controller:"alert", action:"update")
		delete "/api/guest/alert/$id" (controller:"alert", action:"delete")



		// http://docs.grails.org/3.3.x/guide/theWebLayer.html#urlmappings
		// http://docs.grails.org/3.3.11/guide/theWebLayer.html#restfulMappings
		// State
		get "/api/guest/state" (controller:"state", action:"index")
		get "/api/guest/state/$id" (controller:"state", action:"show")

		// Region
		get "/api/states/$abbr/regions/$type" (controller:"region", action:"index")
		get "/api/regions/$id" (controller:"region", action:"show")
		get "/api/regions/create" (controller:"region", action:"error")
		post "/api/regions" (controller:"region", action:"error")
		get "/api/regions/$id/edit" (controller:"region", action:"error")
		put "/api/regions/$id" (controller:"region", action:"error")
		delete "/api/regions/$id" (controller:"region", action:"error")

		// Monitor
		get "/api/regions/$regionId/monitors" (controller:"monitor", action:"index")
		get "/api/monitors/$id" (controller:"monitor", action:"show")
		post "/api/monitors" (controller:"monitor", action:"save")
//		get "/api/regions/$id/edit" (controller:"region", action:"error")
		put "/api/monitors" (controller:"monitor", action:"update")
//		delete "/api/regions/$id" (controller:"region", action:"error")

		// Request
		post "/api/request/importio" (controller:"request", action:"importio")

		// ETL
		get "/api/guest/etl/start/$monitorId?" (controller:"extractTransferLoad", action:"start")

		// Search
		post "/api/search" (controller:"search", action:"request")
		post "/api/guest/search" (controller:"search", action:"request")
		put "/api/search/$id?" (controller:"search", action:"update")
		delete "/api/search/$id?" (controller:"search", action:"delete")

		// Suggestions
		get "/api/guest/suggest/$word/$count" (controller:"search", action:"suggest")
		get "/api/suggest/$word/$count" (controller:"search", action:"suggest")

		// Documents
		get "/api/document/$uuid" (controller:"document", action:"view")
		get "/api/guest/document/$uuid" (controller:"document", action:"view")
		get "/api/guest/s3/download/$uuid" (controller:"document", action:"download")

		// Sales Leads
		get "/api/guest/lead" (controller:"lead", action:"index")
		post "/api/guest/contact" (controller:"lead", action:"save")

		// Sitemap
		get "/api/sitemap/$fileName" (controller: "sitemap", action:"view")
		put "/api/guest/sitemap" (controller: "sitemap", action:"update")
		delete "/api/sitemap" (controller: "sitemap", action:"delete")

		// User
		get "/api/user" (controller:"user", action:"index")
		get "/api/user/$id" (controller:"user", action:"show")
//		post "/api/user" (controller:"user", action:"save")
//		put "/api/user" (controller:"user", action:"update")
		delete "/api/user/$id" (controller:"user", action:"delete")


		"/"(controller: 'application', action:'index')
        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
