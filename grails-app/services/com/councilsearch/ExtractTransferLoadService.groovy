package com.councilsearch

import com.councilsearch.search.Request
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import grails.gsp.PageRenderer
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import com.google.common.hash.Hashing
import org.apache.pdfbox.multipdf.Overlay
import org.apache.pdfbox.tools.TextToPDF
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import java.nio.charset.Charset
import org.springframework.beans.factory.InitializingBean
import org.jsoup.Jsoup

import java.sql.SQLException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.regex.Matcher

class ExtractTransferLoadService implements InitializingBean {

	def queryService
	def requestService
	def searchService
	def eventService
	def messageService
	def amazonWebService
	def monitorService

	PageRenderer groovyPageRenderer

	String PDF_OVERLAY_LOCATION
	String TEMP_DIR
	Integer ETL_REGION_THREAD
	Integer ETL_DOCUMENT_BATCH_SIZE

	// TODO - Separate this to its own microservice

	public void afterPropertiesSet() throws Exception {
		TEMP_DIR = CustomConfig.findByName("TEMP_DIR")?.getValue() ?: ""
		ETL_REGION_THREAD = CustomConfig.findByName("ETL_REGION_THREAD")?.getValue() as Integer ?: 2
		ETL_DOCUMENT_BATCH_SIZE = CustomConfig.findByName("ETL_DOCUMENT_BATCH_SIZE")?.getValue() as Integer ?: 5
		PDF_OVERLAY_LOCATION = this.class.classLoader.getResource('overlay.pdf')?.file
	}

	def start(def monitorId){
		List monitors = []

		// Get active monitors
		if(monitorId != null){ // Process single montior
			log.info("Starting ETL for Monitor:${monitorId}")
			monitors = queryService.monitorById(monitorId)
		}else{ // Process all Live
			log.info("Starting ETL for All Monitors")
			monitors = queryService.monitorsByStatus("Live")
		}

		Iterator<Map> mItr = monitors.iterator()

		// Organize them by region to not crash their website...
		Map docsByRegion = [:]

		// Grab the documents from the monitor and add them by region so we dont
		// crush a single muni server
		while(mItr.hasNext()){
			Map monitor = mItr.next()
			def regionId = monitor.regionId
			List docPayloads = processMonitor(monitor) ?: []

			// Add to docs list
			if(docsByRegion.containsKey(regionId)){
				// If region exists add too
				docsByRegion.put(regionId, docsByRegion.get(regionId) + docPayloads)
			}else{
				docsByRegion.put(regionId, docPayloads)
			}

			// Remove it when we are done
			mItr.remove()
		}

		List regionKeys = docsByRegion.keySet() as List

		// Process the documents for each region in separate threads
		regionKeys.collate(ETL_REGION_THREAD).each { regionKeysBatch ->
			Iterator<Long> regionItr = regionKeysBatch.iterator()

			// Grab the iterator over this batch
			while(regionItr.hasNext()){
				int count = 0
				Long regionId = regionItr.next()
				List docPayloads = docsByRegion.get(regionId)

				// Take the list of documents and batch them to commit them
				docPayloads.collate(ETL_DOCUMENT_BATCH_SIZE).each { docPayloadsBatch ->
					count = count + ETL_DOCUMENT_BATCH_SIZE // Just for logging info
					log.info("Processing Region:${regionId} document batch:${count} of ${docPayloads?.size()}")
					requestDocuments(docPayloadsBatch)
					extractDocuments(docPayloadsBatch)
					generateHash(docPayloadsBatch)
					generateUUID(docPayloadsBatch)
					extractMeetingDate(docPayloadsBatch)
					// removePhrases() // Things like "tune into Cox channel 9"
					queryService.createDocuments(docPayloadsBatch)
					brandPDF(docPayloadsBatch)
					amazonWebService.uploadDocuments(docPayloadsBatch)
					searchService.etlIndex(docPayloadsBatch)
					cleanPayloads(docPayloadsBatch)
				}
			}
		}

		log.info "Finished ETL: ${monitorId ?: "All"}"
		return docsByRegion
	}

	def processMonitor(Map monitor){
		log.info "Extracting data for Monitor:${monitor.monitorId}"
		boolean textDedup = monitor.hashDedup ?: false
		List docMaps = []

		try{
			docMaps = monitorService.process(monitor) ?: []

			log.info "Monitor:${monitor.monitorId} found ${docMaps?.size()} documents"

			// Dont dedup urls will dedup based on the text downstream
			if(!textDedup && docMaps.size() > 0){
				docMaps = deduplicateDocumentByURL(monitor, docMaps)
			}

			log.info "End Gathering Monitor:${monitor.monitorId} data"
		}catch(SQLException se){
			log.error("Could not query for duplicate urls: " + se)
		}catch(IOException ioe){
			log.error("Could not process monitor: "+ioe)
		}catch(FailingHttpStatusCodeException fhsce){
			log.error("Could not process monitor: "+fhsce)
		}catch(MalformedURLException mue){
			log.error("Could not process Monitor: ${monitor.monitorId} with URL: ${monitor.url} "+mue)
		}

		return docMaps
	}

	List<Map> deduplicateDocumentByURL(Map monitor, List<Map> docMaps) throws SQLException{
		def monitorId = monitor.get("monitorId")
		log.info("URL Deduplicating Monitor:${monitorId} with initial document count:  ${docMaps.size()}")

		List existingUrls = queryService.getDocumentURLsByMonitorId(monitorId)

		// Step 1 - Dedup urls
		docMaps.unique{ docMap ->
			docMap.url
		}

		// Step 2 - Dedup urls that already exist
		Iterator<Map> dItr = docMaps.iterator()

		while(dItr.hasNext()){
			Map docMap = dItr.next()

			// Remove it the docMap if the url already exists
			if(existingUrls.contains(docMap.url)){
				dItr.remove()
			}
		}

		log.info("Finished URL Deduplicating Monitor:${monitorId} with final document count:  ${docMaps.size()}")

		return docMaps
	}

	// Download documents
	def requestDocuments(List docPayloads){
		Iterator<Map> dItr = docPayloads.iterator()

		while(dItr.hasNext()){
			Map docPayload = dItr.next()

			if(docPayload?.url != null && !"".equals(docPayload?.url) && docPayload?.content == null){
				HttpResponse response

				sleep(1000) // Lets delay this until we can parse the Robots or Proxy

				if(docPayload.url?.toLowerCase().startsWith("https")){
					response = requestService.executeSSLGet(docPayload.url, null, null, 0)
				}else if(docPayload.url?.toLowerCase().startsWith("http")){
					response = requestService.executeGet(docPayload.url, null, 0)
				}else{
					docPayload.put("message", "URL missing protocol")
					log.error("Missing protocol for url:${docPayload.url}")
					// Get rid of it since we cants do anything with it
					dItr.remove()
					continue
				}

				// Something didnt go right
				if(response == null || response?.getStatusLine()?.getStatusCode() != HttpStatus.SC_OK){
					docPayload.put("message", "Response is empty or status is not 200")
					docPayload.put("statusCode", response?.getStatusLine()?.getStatusCode())
					docPayload.put("success", false)
					continue
				}

				HttpEntity entity = response.getEntity()

				docPayload.put("statusCode", response.getStatusLine()?.getStatusCode())
				docPayload.put("contentType", entity?.contentType?.value)
				docPayload.put("location", response.getFirstHeader("Location")?.getValue())
				docPayload.put("success", true) // Lets assume success so far...

				File file = createFile(entity)

				if(file == null || !file.exists()){
					docPayload.put("message", "Could not download file")
					continue
				}

				docPayload.put("filePath", file.getAbsolutePath())
			}
		}

		return docPayloads
	}

	def createFile(HttpEntity entity){
		File file = File.createTempFile("file-", ".cs")
		InputStream is
		FileOutputStream fos

		try{
			is = entity?.getContent()
			fos = new FileOutputStream(file)
			int inByte

			while((inByte = is?.read()) != -1){
				fos?.write(inByte)
			}
		}catch(Exception e){
			log.error "Cant create file Exception: "+e
			// Filed so delete th file
			file?.delete()
			return null
		}finally{
			is?.close()
			fos?.close()
		}

		return file
	}


	def extractDocuments(List<Map> payloads){
		Iterator<Map> dItr = payloads.iterator()

		while(dItr.hasNext()) {
			Map payload = dItr.next()
			boolean success = payload.get("success")

			// TODO - What about content only docs?
			// We didnt download a file
			if(!success){
				log.warn("Skipping unloaded document: "+payload.url)
				continue
			}

			String content = ""
			String url = payload.get("url")
			String contentType = payload.get("contentType")
			String filePath = payload.get("filePath")

			// Getting random File(null) error
			if(filePath == null || "".equals(filePath?.trim())){
				payload.put("message", "Cannot extractDocument with empty filePath")
				payload.put("success", false)
				log.warn("Cannot extractDocument with empty filePath")
				continue
			}

			File file = new File(filePath)

			if(file != null && file.exists()){
				// PDF
				if(contentType?.toLowerCase()?.contains("pdf") || url?.toLowerCase()?.contains(".pdf")) {
					content = cleanTextContent(extractPDF(file))
				// HTML
				}else if(contentType?.toLowerCase()?.contains("html") || url?.toLowerCase()?.contains(".html")){
					content = cleanTextContent(extractHTML(file))

					// Create PDF from extracted HTML
					File pdf = createPDF(content)

					// swap the HTML file for new PDF file
					if(pdf != null && pdf.exists()){
						payload.put("filePath", pdf.getAbsolutePath())
						// delete the old html file
						file.delete()
					}
				// Word
				}else if(contentType?.toLowerCase()?.contains("word") || url?.toLowerCase()?.contains(".docx")){
					// Apache tika
					content = cleanTextContent(extractWord(file))

					// Create PDF from extracted Word content
					File pdf = createPDF(content)

					// swap the HTML file for new PDF file
					if(pdf != null && pdf.exists()){
						payload.put("filePath", pdf.getAbsolutePath())
						// delete the old html file
						file.delete()
					}
				// Wild card
				}else{
					String sampleText = extractLines(file, 3)

					if(sampleText?.toLowerCase()?.contains("pdf")){
						content = cleanTextContent(extractPDF(file))
					}else{
						payload.put("message", "Cannot extract file")
						payload.put("success", false)
						log.warn("Cannot extract file assoc with URL: "+payload.url)
					}
				}
			}

			payload.put("content", content)
		}
	}

	String extractPDF(File file){
		String content = ""

		if(file != null && file.exists()){
			try {
				content = pdfBox(file)
				// Clean the content of some weird tags
				content = HTMLTagRemover(content)
			} catch (Exception e) {
				log.error "Could not extract text from PDF: " + e
			}
		}

		return content
	}

	String extractHTML(File file){
		String content = ""

		if(file != null && file.exists()){
			// JSOUP
			try {
				// TODO - Parse but leave in paragraph markers
				content = Jsoup.parse(file, "UTF-8")?.body()?.text()
			} catch (Exception e) {
				log.error "Could not extract text from HTML: " + e
			}
		}

		return content
	}

	String extractWord(File file){
		String content = ""

		try {
			AutoDetectParser parser = new AutoDetectParser()
			BodyContentHandler handler = new BodyContentHandler()
			Metadata metadata = new Metadata()
			InputStream stream = file.newInputStream()
			parser.parse(stream, handler, metadata)
			content = cleanTextContent(handler.toString())
		} catch(Exception e) {
			log.error "Could not extract text from MS Word Docx: " + e
		}

		return content
	}

	File createPDF(String content){
		PDDocument doc
		TextToPDF ttp = new TextToPDF()
		File pdf = File.createTempFile("createPDF-", ".cs")

		try{
			doc = ttp.createPDFFromText(new StringReader(content))
			doc.save(pdf)
		}catch(Exception e){
			log.error("Could not create PDF from content: "+e)
		}finally{
			doc?.close()
		}

		return pdf
	}

	// Grab a couple lines of a file
	String extractLines(File file, int maxLines){
		String lines = ""
		int currLineCount = 0

		if(file != null && file.exists()){
			file.withReader('UTF-8') { reader ->
				def line
				while ((line = reader.readLine()) != null) {
					lines += line
					currLineCount++;
					if(currLineCount >= maxLines){
						break;
					}
				}
			}
		}

		return lines
	}

	String HTMLTagRemover(String text){
		return text?.replaceAll("\\<.*?\\>", " ");
	}

	def extractMeetingDate(List<Map> docMaps){
		LocalDate meetingDate
		Iterator<Map> docMapsItr = docMaps.iterator()

		while(docMapsItr.hasNext()) {
			Map docMap = docMapsItr.next()
			String dateStr = docMap.get("dateStr")

			// If the dateStr is empty grab some content
			if(dateStr == null || "".equals(dateStr.trim())){
				dateStr = docMap.get("content")?.take(500) ?: ""
			}

			// Clean some characters
			dateStr = dateStr.replaceAll("\r", " ")
								.replaceAll("\n", " ")
								.replaceAll(",", " ")
								.replaceAll("/", " ")
								.replaceAll("-", " ")
								.replaceAll("_", " ")
								.replaceAll("\\.", " ")
								.replaceAll("\\|", " ")
								.replaceAll("\\s{2,}", " ")
								.trim()

			switch(dateStr) {
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
				case ~/.*(\b\w{3,}\s+\d{1,2}\s+\d{4}).*/:
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

			if(meetingDate != null){
				docMap.put("meetingDate", meetingDate)
			}else{ // Documents no good if we dont have a meeting
				log.error("Unsupported date type. Could not process date from string: ${dateStr}")
				docMap.put("message", "Unsupported date type. Could not process date from string")
				docMap.put("success", false)
			}
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

	def pdfBox(File file) throws Exception{
		String content = ""
		PDDocument pdf

		if(file != null && file.exists()){
			try {
				pdf = PDDocument.load(file)
				AccessPermission ap = pdf.getCurrentAccessPermission()

				if (!ap.canExtractContent()){
					throw new IOException("You do not have permission to extract text")
				}

				PDFTextStripper stripper = new PDFTextStripper()
				content = stripper.getText(pdf)
			}catch (Exception e) {
				throw e
			}finally {
				pdf?.close()
			}
		}

		return content
	}

	def cleanPayloads(List<Map> payloads){
		Iterator<Map> dItr = payloads.iterator()

		while(dItr.hasNext()) {
			Map payload = dItr.next()
			String filePath = payload.get("filePath")

			// Getting random File(null) error
			if(filePath == null || "".equals(filePath?.trim())){
				log.warn("Cannot remove File with empty filePath")
				continue
			}

			File file = new File(filePath)

			if(file != null && file.exists()){
				try{
					def isDeleted = file.delete()

					if(!isDeleted){
						log.error "Could not delete file: "+file.absolutePath
					}
				}catch(Exception e){
					log.error "Exception deleting file: "+e
				}
			}
			dItr.remove() // Remove it since we are done
		}
	}

	def generateHash(List<Map> payloads){
		Iterator<Map> dItr = payloads.iterator()

		while(dItr.hasNext()) {
			String hash = ""
			Map payload = dItr.next()
			String content = payload.get("content")

			if(content != null && !"".equals(content?.trim())){
				hash = Hashing.sha256().hashString(content, Charset.defaultCharset()).toString()
			}

			if("".equalsIgnoreCase(hash)){
				payload.put("message", "Could not create hash code")
				payload.put("success", false)
			}

			payload.put("hash", hash)
		}

	}

	def generateUUID(List<Map> payloads){
		Iterator<Map> dItr = payloads.iterator()

		while(dItr.hasNext()) {
			Map payload = dItr.next()
			UUID uuid = UUID.randomUUID()
			payload.put("uuid", uuid.toString())
		}
	}

	def brandPDF(List payloads){
		Iterator<Map> dItr = payloads.iterator()

		while(dItr.hasNext()) {
			Map payload = dItr.next()

			// Dont brand if the processing didnt end with a success
			if(!payload.get("success")){
				continue
			}

			String filePath = payload.get("filePath")

			// Getting random File(null) error
			if(filePath == null || "".equals(filePath?.trim())){
				log.warn("Cannot brandPDF with empty filePath")
				continue
			}

			File pdf = new File(filePath)

			if(pdf != null && pdf.exists()){
				PDDocument pdfBoxDoc
				Overlay overlay = new Overlay()
				File brandedPDF = File.createTempFile("brandedPDF-", ".cs")

				if(pdf != null && pdf?.exists()) {
					// No location return original
					if(PDF_OVERLAY_LOCATION == null || "".equals(PDF_OVERLAY_LOCATION)) {
						log.info("Could not load PDF overlay file")
						return
					}

					try {
						pdfBoxDoc = PDDocument.load(pdf)
						HashMap overlayMap = new HashMap<Integer, String>()

						for (int i = 1; i <= pdfBoxDoc.getNumberOfPages(); i++) {
							overlayMap.put(i, PDF_OVERLAY_LOCATION)
						}

						overlay.setInputPDF(pdfBoxDoc)
						overlay.setOverlayPosition(Overlay.Position.FOREGROUND)
						overlay.overlay(overlayMap)

						pdfBoxDoc.save(brandedPDF)
						// Add the file path
						payload.put("filePath", brandedPDF?.getAbsolutePath())
						// Delete the old unbranded PDF
						pdf.delete()
					} catch (Exception e) {
						log.error("Could not create branded overlay: " + e.message)
						// delete the new file
						brandedPDF.delete() // didnt work remove it
					} finally {
						overlay.close()
						pdfBoxDoc?.close()
					}
				}
			}else{
				log.error("Could not open PDF to brand")
			}
		}
	}

	String cleanTextContent(String text){
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", " ")
		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
		// removes non-printable characters from Unicode
//		text = text.replaceAll("\\p{C}", "") // This removes return characters

		return text.trim()
	}

	////////////////////////////////////////////////////
	// Process alert
	////////////////////////////////////////////////////

	// TODO - scale
	def processAlerts(def alertId){
		List alerts = []

		log.info("Processing Alerts...")

		// If we have an alert id process just that one if not process all live alerts
		if(alertId != null){
			Alert alert = Alert.get(alertId)
			alerts.add(alert)
		}else{
			alerts = Alert.findAllByStatus("live")
		}

		Iterator alertsItr = alerts.iterator()

		while(alertsItr.hasNext()){
			Alert alert = alertsItr.next()
			List regionIds = queryService.getAlertRegionsByAlertId(alert.id).collect{it.regionId}
			List phrases = queryService.getAlertPhrasesByAlertId(alert.id).collect{it.phrase}

			// Only do a group of regions at a time because of solr
			regionIds.collate(20).each{ subSetRIds ->
				phrases.each{ phrase ->
					log.info("processing Phrase: ${phrase} for geos: ${subSetRIds}")

					if(!"".equals(phrase)){
						// Create request
						Request searchRequest = new Request(phrase, subSetRIds)
						SolrQuery sq = searchRequest.createSolrQueryMin()
						QueryResponse queryResponse = searchService.request(sq)

						// Going to parse this and not use the Response obj
						if(queryResponse != null){
							SolrDocumentList solrDocList = queryResponse.getResults()
							Map hlMap = queryResponse.getHighlighting()

							// Iterate through the results
							for(SolrDocument sDoc : solrDocList){
								String id = sDoc.getFieldValue("id")
								// TODO - probably swap this for payload
								Document doc = Document.get(id)

								if(doc){
									Match match = Match.findByAlertAndDocument(alert, doc)

									// Match does not exist
									if(!match){
										match = new Match(document: doc, eventCreated: false)

										// Get the previews
										hlMap.get(id)?.get("content").each{ hl ->
											Preview preview = new Preview(phraseHit: phrase, text: hl)
											match.addToPreviews(preview)
										}

										// Creat the match
										alert.addToMatches(match)

										if(!alert.save(flush: true)){
											log.error("Could not update Alert:${alert.id} with matches"+ alert.errors)
										}
									// Match exists
									}else{
										// Does the match contain the phrase already
										if(!match.previews?.collect{it.phraseHit?.toLowerCase()}.contains(phrase.toLowerCase())){
											// Get the previews
											hlMap.get(id)?.get("content").each{ hl ->
												log.info("the preview"+hl)
												Preview preview = new Preview(phraseHit: phrase, text: hl)
												match.addToPreviews(preview)
											}

											// Creat the match
											alert.addToMatches(match)

											if(!alert.save(flush: true)){
												log.error("Could not update Alert:${alert.id} with matches"+ alert.errors)
											}
										}
									}
								}
							}
						}
					}

				}
			}
		}

		log.info("Finished Processing Alerts")
	}

	// TODO - scale
	def createEvents(def alertId){
		log.info("Creating events...")
		List matches = []

		// Create events for all alerts or a single alert
		if(alertId != null){
			Alert alert = Alert.get(alertId)
			matches = Match.findAllByAlertAndEventCreated(alert, false)
		}else{
			matches = Match.findAllByEventCreated(false)
		}


		log.info("Found ${matches.size()} matches to create events")

		Iterator matchesItr = matches.iterator()

		while(matchesItr.hasNext()) {
			Match match = matchesItr.next()
			UUID uuid = UUID.randomUUID()
			Event event = new Event(match: match,
									owner: match.alert.manager,
									dueDate: match.document.meetingDate,
									status: "open",
									description: "",
									uuid: uuid,
									isRemoved: false)

			log.info("Creating event members")

			// Assoc the alerts users to this event
			match.alert.members?.each{ member ->
				event.addToMembers(member)
			}

			if(!event.save(flush: true)){
				log.error("Could not create Event for Match:${match.id} "+ event.errors)
			}else{
				log.info("Create Event:${event.id} for Match:${match.id}")

				// Update the match
				match.eventCreated = true

				if(!match.save(flush: true)){
					log.error("Could not update EventCreated status for Match:${match.id} "+ match.errors)
				}
			}
		}

		log.info("Finished Creating events")
	}

	def notifications(){
		// Build notification data
		List<Map> messages = buildMessageData()
		// Send notifications
		messageService.sendNotifications(messages)
		// Mark events as sent
		markAsSent(messages)
	}

	def buildMessageData(){
		List<Map> messages = []
		DateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.US)
		Date today = new Date()

		// Start with active users
		User.findAllByEnabledAndEmailActive(true, true).each{ user ->
			log.info("Building notification data for user:${user.username}")
			List alerts = []

			// Get all active users alerts
			queryService.getAssocActiveAlertsByUser(user.id, "live").each{ alertId ->
				Alert alert = Alert.get(alertId)
				List events = []

				// Get the events for this active alert
				queryService.getUnemailedEventsByAlert(alertId).each{ eventId ->
					Map eventMap = [:]
					Event event = Event.get(eventId)

					// Id
					eventMap.put("id", event.id)
					// Title
					eventMap.put("title", event.match.document.title?.capitalize() ?: "No title")
					// Geo
					eventMap.put("geography", event.match.document.monitor.region?.name?.split(" ").collect{it.capitalize()}.join(" ")+", " + event.match.document.monitor.region?.state?.abbr?.toUpperCase())
					// Meeting Date
					eventMap.put("meetingDate", formatter.format(event.match.document.meetingDate))
					// Event uuid
					eventMap.put("uuid", event.uuid)
					// Document Type
					eventMap.put("documentType", Hibernate.getClass(event.match.document).getName().replaceAll("com.councilsearch.","")?.capitalize())
					// Preview - Only the first, if there is one
					if(event?.match?.previews?.size() > 0){
						Preview preview = event?.match.previews[0]
						eventMap.put("preview", preview.text)
					}else{
						eventMap.put("preview", "Preview is unavailable")
					}

					events.add(eventMap)
				}

				// If we have events build the alert map and add it to the message
				if(events?.size() > 0){
					Map alertMap = [:]

					// Alert Id
					alertMap.put("id", alert.id)
					// Alert Name
					alertMap.put("name", alert.name ?: "Un-named Alert")
					// Alerts new events
					alertMap.put("events", events)

					alerts.add(alertMap)
				}
			}

			// If we have any alerts add them to the message map
			if(alerts?.size() > 0){
				Map messageMap = [:]
				def eventsCount = 0

				// User id
				messageMap.put("userId", user.id)
				// User email
				messageMap.put("email", user.username) // User name is email
				// Date today, hopefully we dont get UTC-ed
				messageMap.put("dateToday", formatter.format(today))
				// alerts
				messageMap.put("alerts", alerts)

				// Find the number of new events
				alerts.each { alertMap ->
					eventsCount += alertMap.get("events")?.size()
				}

				messageMap.put("eventsCount", eventsCount)

				// Message subject
				messageMap.put("subject", "We found ${eventsCount} new event(s) - ${formatter.format(today)}")

				messages.add(messageMap)
			}
		}

		return messages
	}

	def markAsSent(List messages){
		Date now = new Date()
		List eventIds = []

		// Probably a groovy collect way to do it but dont have time
		messages.each { messageMap ->
			messageMap.get("alerts").each{ alertMap ->
				alertMap.get("events").each{ eventMap ->
					def eventId = eventMap.get("id")

					// Gather distinct event ids
					if(!eventIds.contains(eventId)){
						eventIds.add(eventId)
					}
				}
			}
		}

		// Mark as read
		eventIds.each{ eventId ->
			try{
				eventService.markEmailed(eventId, now)
			}catch(Exception e){
				log.error("ETL notifcation error: "+e)
			}
		}
	}
}
