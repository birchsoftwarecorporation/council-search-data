package com.councilsearch

import grails.gorm.transactions.Transactional

import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Transactional
class DocumentService {
	def extractTransferLoadService
	def amazonWebService

    def reExtract(def uuid) {
		def content
		File file = File.createTempFile(uuid, ".pdf")
		def s3inputStream

		Document document = Document.findByUuid(uuid)

		if(!document){
			log.error("Could not find Document:${uuid} ")
			return null
		}

		// Grab the file
		try{
			s3inputStream = amazonWebService.s3Download(uuid)
			Files.copy(s3inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)

			// ReExtract
			content = extractTransferLoadService.extractPDF(file)
			content = extractTransferLoadService.cleanTextContent(content)

			// Save the new content
			if(!"".equals(content.trim())){
				document.content.text = content

				if(!document.save(flush: true)){
					log.error("Could not update Document:${uuid} new newly extracted text"+ document.errors)
				}
			}
		}catch(Exception e){
			log.error("Could not download file from S3: "+e)
			return null
		}finally{
			file.delete()
		}

		return content
    }
}
