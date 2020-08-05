package com.councilsearch

import com.councilsearch.importio.ImportioResponse
import grails.converters.JSON

class RequestController {
	RequestService requestService

	def importio(){
		def json = request.JSON
		String importioUrl = json.url

		if(importioUrl == null || "".equals(importioUrl)){
			response.status = 400
			render ([error: "Cannot request empty URL "] as JSON)
			return
		}

		ImportioResponse ir = requestService.importIORequest(importioUrl)

		if(!ir){
			response.status = 500
			render ([error: "Could not get response from Importio server"] as JSON)
			return
		}

		render ir as JSON
	}
}
