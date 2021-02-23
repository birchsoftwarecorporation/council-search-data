package com.councilsearch

import grails.gorm.PagedResultList
import grails.gorm.transactions.Transactional

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.regex.Matcher

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

	@Transactional
	def reExtractDate() {
		// First iteration prep
		def offset = 0
		def max = 100
		PagedResultList docList = Document.list(max: max, offset: offset, sort: "id", order: "desc")
		def total = docList.getTotalCount()

		// Batch through the documents
		while(offset < total){
			log.info("Iterating through offset:${offset}")
			Iterator dItr = docList.iterator()

			while(dItr.hasNext()){
				Document document = dItr.next()

				if(document.meetingDate != null){
					continue
				}

				log.info("Reextracting date for Document:${document.id}")
				Date oldMeetingDate = document.meetingDate
				String meetingStr = document.meetingDateStr
				LocalDate meetingDate

				// Grab the content if no str
				if(meetingStr == null){
					meetingStr = document.content?.text?.take(500) ?: ""
				}

				// Clean some characters
				meetingStr = meetingStr.replaceAll("\r", " ")
						.replaceAll("\n", " ")
						.replaceAll(",", " ")
						.replaceAll("/", " ")
						.replaceAll("-", " ")
						.replaceAll("_", " ")
						.replaceAll("\\.", " ")
						.replaceAll("\\|", " ")
						.replaceAll("\\s+", " ")
						.trim()
						.toLowerCase()

				switch(meetingStr) {
					// M d yyyy - Ex: 03 4 2020
					case ~/.*(\b\d{1,2}\s+\d{1,2}\s+\d{4}).*/:
						Matcher m = Matcher.lastMatcher
						meetingDate = stringToDate(m.group(1), "M d yyyy")
						break;
					// MMMM d yyyy - Ex: January 6 2020
					case ~/.*(\b\w{4,}\s+\d{1,2}\s+\d{4}).*/:
						Matcher m = Matcher.lastMatcher
						meetingDate = stringToDate(m.group(1), "MMMM d yyyy")
						break;
					// MMM d yyyy - Ex: Jan 20 2020
					case ~/.*(\b\w{3}\s+\d{1,2}\s+\d{4}).*/:
						Matcher m = Matcher.lastMatcher
						meetingDate = stringToDate(m.group(1), "MMM d yyyy")
						break;
					//	M d yy - Ex: 02/23/21
					case ~/.*(\b\d{1,2}\s+\d{1,2}\s+\d{2}).*/:
						Matcher m = Matcher.lastMatcher
						meetingDate = stringToDate(m.group(1), "M d yy")
						break;
					// yyyy M d
					case ~/.*(\b\d{4}\s+\d{1,2}\s+\d{1,2}\b).*/:
						Matcher m = Matcher.lastMatcher
						meetingDate = stringToDate(m.group(1), "yyyy M d")
						break;
					// MMddyyyy
					case ~/.*(\b\d{8}).*/:
						Matcher m = Matcher.lastMatcher
						meetingDate = stringToDate(m.group(1), "MMddyyyy")
						break;
				}

				log.info("Meeting String:\t${meetingStr?.take(20)}\told:${oldMeetingDate}\tnew:${meetingDate}")

				if(meetingDate != null){
					// TODO - Migrate GORM Date to LocalDateTime
					document.meetingDate = java.sql.Date.valueOf(meetingDate)

					if(!document.save(flush: true)){
						log.error("Could not update meetingDate for Document:${document.id} "+ document.errors)
					}
				}

			}

			log.info("Committing batch: ${offset}")

			// Get ready for the next round
			offset += max
			docList = Document.list(max: max, offset: offset, sort: "id", order: "desc")
		}
	}

	LocalDate stringToDate(String dateStr, String format){
		LocalDate date
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(format).toFormatter(Locale.ENGLISH)

		try {
			date = LocalDate.parse(dateStr, formatter)
		}catch (Exception e) {
			log.error("Could not parse date: ${dateStr} format:${format} "+e.getMessage())
		}

		return date
	}
}
