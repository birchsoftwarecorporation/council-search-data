package com.councilsearch


import grails.rest.*
import grails.converters.*

class SitemapController {
	def sitemapService

	// Download the latest sitemap
	def view(){
		String fileName = params.fileName

		if(fileName == null || "".equalsIgnoreCase(fileName)){
			response.status = 400
			render ([error: "Could not find Sitemap with empty file name"] as JSON)
			return
		}

		File file = sitemapService.getSitemap(fileName)

		if(file == null || !file.exists()){
			response.status = 400
			render ([error: "Could not find ${fileName}"] as JSON)
			return
		}

		InputStream fileStream = new FileInputStream(file)

		response.setHeader("Content-disposition", "filename=${fileName}")
		response.contentType = 'application/octet-stream'
		response.outputStream << fileStream
		response.outputStream.flush()
	}

	// Generate and upload Sitemap XMLs
	def update(){
		// Out with the old
		sitemapService.clear()
		// Create
		boolean success = sitemapService.create()
		// Upload
		render ([message: "Upload ${success}"] as JSON)
	}

	def delete(){
		boolean isCleared = sitemapService.clear()

		if(!isCleared){
			response.status = 400
			render ([error: "Could not clear Sitemap directory"] as JSON)
			return
		}

		render ([message: "Successfully cleared Sitemap directory"] as JSON)
	}

}

