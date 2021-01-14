package com.councilsearch

import com.councilsearch.importio.ImportioResponse
import com.councilsearch.search.Request
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gsp.PageRenderer
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils
import com.google.common.hash.Hashing
import org.apache.pdfbox.multipdf.Overlay
import org.apache.pdfbox.tools.TextToPDF
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import java.nio.charset.Charset
import org.springframework.beans.factory.InitializingBean
import org.jsoup.Jsoup
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.regex.Matcher

class ExtractTransferLoadService implements InitializingBean {

	def queryService
	def requestService
	def searchService
	def eventService
	def messageService
	def amazonWebService

	PageRenderer groovyPageRenderer

	String PDF_OVERLAY_LOCATION
	String IMPORTIO_BASE_URL
	String IMPORTIO_PATH
	String IMPORTIO_API_KEY
	String TEMP_DIR
	Integer ETL_REGION_THREAD
	Integer ETL_DOCUMENT_BATCH_SIZE

	// TODO - Separate this to its own microservice

	public void afterPropertiesSet() throws Exception {
		IMPORTIO_BASE_URL = CustomConfig.findByName("IMPORTIO_BASE_URL")?.getValue() ?: "https://data.import.io"
		IMPORTIO_PATH = CustomConfig.findByName("IMPORTIO_PATH")?.getValue() ?: "/extractor/EXTRACTOR_ID/json/latest"
		IMPORTIO_API_KEY = CustomConfig.findByName("IMPORTIO_API_KEY")?.getValue() ?: ""
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

		// Organize them by region to not crash their website...
		Map docsByRegion = [:]

		Iterator<Map> mItr = monitors.iterator()

		// Grab the documents from the monitor and add them by region so we dont
		// crush a single muni server
		while(mItr.hasNext()){
			Map monitor = mItr.next()
			def regionId = monitor.regionId
			List docPayloads = processMonitor(monitor)
			sleep(500) // Importio server limit

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

				// Take the list of documents and batch them too to commit them
				docPayloads.collate(ETL_DOCUMENT_BATCH_SIZE).each { docPayloadsBatch ->
					count = count + ETL_DOCUMENT_BATCH_SIZE // Just for logging info
					log.debug("Processing Region:${regionId} document batch:${count} of ${docPayloads?.size()}")
					requestDocuments(docPayloadsBatch)
					extractDocuments(docPayloadsBatch)
					generateHash(docPayloadsBatch)
					generateUUID(docPayloadsBatch)
					extractDate(docPayloadsBatch)
					// removePhrases() // Things like "tune into Cox channel 9"
					queryService.createDocuments(docPayloadsBatch)
					brandPDF(docPayloadsBatch)
					amazonWebService.uploadDocuments(docPayloadsBatch)
					searchService.bulkIndex(docPayloadsBatch)
					cleanPayloads(docPayloadsBatch)
				}
			}
		}

		log.info "Finished ETL: ${monitorId ?: "All"}"
		return docsByRegion
	}

	def processMonitor(Map monitor){
		boolean textDedup = monitor.hashDedup ?: false
		log.info "Gathering Monitor:${monitor.monitorId} data from import.io"
		List docMaps = getImportData(monitor)
		log.info "Monitor:${monitor.monitorId} found ${docMaps?.size()} documents"

		// Dont dedup urls will dedup based on the text downstream
		if(!textDedup){
			docMaps = deduplicateDocumentByURL(monitor, docMaps)
		}

		log.info "Monitor:${monitor.monitorId} found ${docMaps?.size()} documents after deduplication"
		log.info "End Gathering Monitor:${monitor.monitorId} data"

		return docMaps
	}

	List<Map> getImportData(Map monitor){
		log.info "Requesting Importio data"
		List<Map> docMaps = []

		ImportioResponse ir = requestImportio(monitor)
		docMaps = getDocumentMapList(ir, monitor)

		return docMaps
	}

	ImportioResponse requestImportio(def monitor){
		ImportioResponse ir
		// Build the Importio URL
		String url = "${IMPORTIO_BASE_URL}${IMPORTIO_PATH?.replace("EXTRACTOR_ID", monitor.extractorId)}?_apikey=${IMPORTIO_API_KEY}"
		// Request the data from the import.io server
		HttpResponse response = requestService.executeSSLGet(url, null, null, 0)

		// Something didnt go right
		if(response == null || response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
			return ir
		}

		// Get response from the server
		String importResponseStr = EntityUtils.toString(response.getEntity())

		// Automap it to the Importio Response object ignoring values we dont need
		ObjectMapper om = new ObjectMapper()
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

		ir = om.readValue(importResponseStr, ImportioResponse.class)

		return ir
	}

	List<Map> getDocumentMapList(ImportioResponse ir, def monitor){
		List docMaps = []
		List datas = ir?.result?.extractorData?.datas

		// Iterate through the raw import results
		datas?.each { data ->
			data?.groups?.each { group ->
				String title = group.titles?.collect{ it.text }?.join(" ")
				String dateStr = group.dates?.collect{ it.text }?.join(" ")
				String aContent = group.agenda_contents?.collect{ it.text }?.join("\n")
				String mContent = group.agenda_contents?.collect{ it.text }?.join("\n")

				// Processing documents

				// Agendas
				// If we have content dont process links
				if(aContent != null && !"".equals(aContent)){
					Map docMap = [:]
					docMap.put("stateName", monitor.stateName)
					docMap.put("regionId", monitor.regionId)
					docMap.put("regionName", monitor.regionName)
					docMap.put("title", title)
					docMap.put("dateStr", dateStr)
					docMap.put("content", aContent)
					docMap.put("monitorId", monitor.monitorId)
					docMap.put("sslVersion", monitor.sslVersion)
					docMap.put("userAgent", monitor.userAgent)
					docMap.put("cookie", monitor.cookie)
					docMap.put("docType", "com.councilsearch.Agenda")
					docMaps.add(docMap)
				}else{ // No content grab the links for processing
					group.agendas.each { agenda ->
						Map docMap = [:]
						docMap.put("stateName", monitor.stateName)
						docMap.put("regionId", monitor.regionId)
						docMap.put("regionName", monitor.regionName)
						docMap.put("title", title)
						docMap.put("dateStr", dateStr)
						docMap.put("monitorId", monitor.monitorId)
						docMap.put("sslVersion", monitor.sslVersion)
						docMap.put("userAgent", monitor.userAgent)
						docMap.put("cookie", monitor.cookie)
						docMap.put("docType", "com.councilsearch.Agenda")

						def url = findImportioUrl(agenda.href, agenda.text)

						if(url != null && !"".equals(url)){
							log.debug("Adding url agenda: "+url)
							docMap.put("url", url)
							docMaps.add(docMap)
						}
					}
				}

				// Agendas supplemental info
				// No content for supps grab the links for processing
				group.agenda_sups.each { agendaSup ->
					Map docMap = [:]
					docMap.put("stateName", monitor.stateName)
					docMap.put("regionId", monitor.regionId)
					docMap.put("regionName", monitor.regionName)
					docMap.put("title", title)
					docMap.put("dateStr", dateStr)
					docMap.put("monitorId", monitor.monitorId)
					docMap.put("sslVersion", monitor.sslVersion)
					docMap.put("userAgent", monitor.userAgent)
					docMap.put("cookie", monitor.cookie)
					docMap.put("docType", "com.councilsearch.AgendaSupplement")

					def url = findImportioUrl(agendaSup.href, agendaSup.text)

					if(url != null && !"".equals(url)){
						log.debug("Adding url agenda sup: "+url)
						docMap.put("url", url)
						docMaps.add(docMap)
					}
				}

				// Minutes
				// If we have content dont process links
				if(mContent != null && !"".equals(mContent)){
					Map docMap = [:]
					docMap.put("stateName", monitor.stateName)
					docMap.put("regionId", monitor.regionId)
					docMap.put("regionName", monitor.regionName)
					docMap.put("title", title)
					docMap.put("dateStr", dateStr)
					docMap.put("monitorId", monitor.monitorId)
					docMap.put("sslVersion", monitor.sslVersion)
					docMap.put("userAgent", monitor.userAgent)
					docMap.put("cookie", monitor.cookie)
					docMap.put("content", mContent)
					docMap.put("docType", "com.councilsearch.Minute")
				}else{ // No content grab the links for processing
					group.minutes.each { minute ->
						Map docMap = [:]
						docMap.put("stateName", monitor.stateName)
						docMap.put("regionId", monitor.regionId)
						docMap.put("regionName", monitor.regionName)
						docMap.put("title", title)
						docMap.put("dateStr", dateStr)
						docMap.put("monitorId", monitor.monitorId)
						docMap.put("sslVersion", monitor.sslVersion)
						docMap.put("userAgent", monitor.userAgent)
						docMap.put("cookie", monitor.cookie)
						docMap.put("docType", "com.councilsearch.Minute")

						def url = findImportioUrl(minute.href, minute.text)

						if(url != null && !"".equals(url)){
							log.debug("Adding url minute: "+url)
							docMap.put("url", url)
							docMaps.add(docMap)
						}
					}
				}

				// Minutes Supplemental
				// No content for supps grab the links for processing
				group.minute_sups.each { minuteSup ->
					Map docMap = [:]
					docMap.put("title", title)
					docMap.put("dateStr", dateStr)
					docMap.put("monitorId", monitor.monitorId)
					docMap.put("sslVersion", monitor.sslVersion)
					docMap.put("userAgent", monitor.userAgent)
					docMap.put("cookie", monitor.cookie)
					docMap.put("docType", "com.councilsearch.MinuteSupplement")

					def url = findImportioUrl(minuteSup.href, minuteSup.text)

					if(url != null && !"".equals(url)){
						log.debug("Adding url minute sup: "+url)
						docMap.put("url", url)
						docMaps.add(docMap)
					}
				}

			}
		}

		return docMaps
	}

	def findImportioUrl(String href, String text){
		String url

		// Grab the link
		if(href != null && !"".equals(href)){
			url = href
		}else if(text != null && text?.startsWith("http")){
			url = text
		}

		return url
	}

	List<Map> deduplicateDocumentByURL(Map monitor, List<Map> docMaps){
		def monitorId = monitor.get("monitorId")
		log.info("Deduplicating Monitor:${monitorId} document URLs initial count: "+docMaps.size())
		List existingUrls = queryService.getDocumentURLsByMonitorId(monitorId)
		// Step 1 - Dedup urls from Importio
		docMaps.unique{ docMap ->
			docMap.url
		}
		log.info("Monitor:${monitorId} now has ${docMaps.size()} documents after Step 1 url dedup")
		// Step 2 - Dedup urls that already exist
		Iterator<Map> dItr = docMaps.iterator()

		while(dItr.hasNext()){
			Map docMap = dItr.next()

			// Remove it the docMap if the url already exists
			if(existingUrls.contains(docMap.url)){
				dItr.remove()
			}
		}
		log.info("Monitor:${monitorId} now has ${docMaps.size()} documents after Step 2 url dedup")
		log.info("Finished deduplicating Monitor:${monitorId} document URLs new count: "+docMaps.size())
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

	def extractDate(List<Map> payloads){
		Date date
		Iterator<Map> dItr = payloads.iterator()

		while(dItr.hasNext()) {
			Map payload = dItr.next()
			String dateStr = payload.get("dateStr")

			// If the dateStr is empty grab some content
			if(dateStr == null || "".equals(dateStr.trim())){
				dateStr = payload.get("content")?.take(500) ?: ""
			}

			// Clean some characters
			dateStr = dateStr.replaceAll("\r", " ")
								.replaceAll("\n", " ")
								.replaceAll(",", " ")
								.replaceAll("/", " ")
								.replaceAll("/", " ")
								.replaceAll("-", " ")
								.replaceAll("_", " ")
								.replaceAll("\\.", " ")

			switch(dateStr) {
				// MM dd yyyy
				case ~/.*(\b\d{1,2}\s+\d{1,2}\s+\d{4}).*/:
					Matcher m = Matcher.lastMatcher
					date = stringToDate("MM dd yyyy", m.group(1))
					break;
				// yyyy MM dd
				case ~/.*(\b\d{4}\s+\d{1,2}\s+\d{1,2}\b).*/:
					Matcher m = Matcher.lastMatcher
					date = stringToDate("yyyy MM dd", m.group(1))
					break;
				// MMM dd yyyy
				case ~/.*(\b\w+\s+\d{1,2}\s+\d{4}).*/:
					Matcher m = Matcher.lastMatcher
					date = stringToDate("MMM dd yyyy", m.group(1))
					break;
				// MM dd yy
				case ~/.*(\b\d{1,2}\s+\d{1,2}\s+\d{2}).*/:
					Matcher m = Matcher.lastMatcher
					date = stringToDate("MM dd yy", m.group(1))
					break;
				// MMddyyyy
				case ~/.*(\b\d{8}).*/:
					Matcher m = Matcher.lastMatcher
					date = stringToDate("MMddyyyy", m.group(1))
					break;
				default:
					log.warn("Could not process date from string: ${dateStr}")
					payload.put("message", "Could not process date from string: ${dateStr}")
					payload.put("success", false)
					break;
			}

			payload.put("date", date)
		}

	}

	Date stringToDate(String format, String dateStr){
		Date date
		DateFormat df = new SimpleDateFormat(format, Locale.ENGLISH);
		try {
			date = df.parse(dateStr)
		}catch (Exception e) {
			log.debug("Could not parse date: "+e.getMessage())
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
						QueryResponse queryResponse = searchService.request(searchRequest.createSolrQueryMin())

						// Going to parse this and not use the Repsonse obj
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
					eventMap.put("documentType", event.match.document.getClass().getName().replaceAll("com.councilsearch.","")?.capitalize())
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
