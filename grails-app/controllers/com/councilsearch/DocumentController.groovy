package com.councilsearch

import grails.converters.JSON

class DocumentController {
	def amazonWebService

	def view(){
		def uuid = params.uuid
		log.info("Viewing Document:${uuid}")

		if(!uuid){
			response.status = 400
			log.error("No UUID specified")
			render ([error: "No UUID specified"] as JSON)
			return
		}

		Document document = Document.findByUuid(uuid)

		if(!document){
			response.status = 400
			log.error("Could not find document with UUID:${uuid}")
			render ([error: "Could not find document with UUID:${uuid}"] as JSON)
			return
		}

		// JSON Converts excludes is garabage so we can build our own
		def docMap = [:]
		docMap.put("uuid", document.uuid)
		docMap.put("stateAbbr", document.monitor.region.state.abbr)
		docMap.put("regionName", document.monitor.region.name)
		docMap.put("monitorURL", document.monitor.url)
		docMap.put("dateCreated", document.dateCreated)
		docMap.put("meetingDate", document.meetingDate)
		docMap.put("lastModified", document.lastModified)
		docMap.put("documentType", document.getClass().name?.replace("com.councilsearch.","").capitalize())
		docMap.put("title", document.title)
		docMap.put("url", document.url)
		docMap.put("content", document.content.text?.replaceAll("\\n","<br>"))

		render docMap as JSON
	}

	def upload(){
//		def id = 1
//		File file = new File("C:\\birch-home\\council-search\\tmp\\w9-AgendaDiscovery.pdf")
//		try{
//			amazonWebService.s3Upload(id, file)
//		}catch(Exception e){
//			log.error("Could not upload file to S3")
//		}
//
//		render "upload"
	}

	def download(){
		def uuid = params.uuid
		def s3inputStream
		log.info("Downloading document with UUID:${uuid}")

		if(!uuid){
			response.status = 400
			log.error("No UUID specified")
			render ([error: "No UUID specified"] as JSON)
			return
		}

		Document document = Document.findByUuid(uuid)

		if(!document){
			response.status = 400
			log.error("Could not find document with UUID:${uuid}")
			render ([error: "Could not find document with UUID:${uuid}"] as JSON)
			return
		}

		try{
			s3inputStream = amazonWebService.s3Download(uuid)
		}catch(Exception e){
			log.error("Could not download file from S3: "+e)
		}

		if(!s3inputStream){
			log.error("Could not download file S3 input stream is empty")
			render "Could not download file"
			return
		}

		// Render PDF in
		response.setHeader("Content-disposition", "filename=${uuid}.pdf")
		response.contentType = 'application/pdf'
		response.outputStream << s3inputStream
		response.outputStream.flush()
	}

	def delete(){
//		def id = 1
//
//		try{
//			amazonWebService.s3Delete(id)
//		}catch(Exception e){
//			log.error("Could not delete file from S3")
//		}

		render "delete"
	}

}
