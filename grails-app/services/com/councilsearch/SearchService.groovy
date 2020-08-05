package com.councilsearch

import com.councilsearch.search.Request
import com.councilsearch.search.Response
import grails.gorm.transactions.NotTransactional
import groovy.sql.Sql
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.springframework.beans.factory.InitializingBean

class SearchService implements InitializingBean {
	static transactional = false

	def dataSource

	def queryService

	String SOLR_BASE_URL
	String SOLR_PORT
	String SOLR_CLUSTER_NAME
	Integer SEARCH_INDEX_BATCH_SIZE
	HttpSolrClient SOLR_CLIENT_REQUEST
	ConcurrentUpdateSolrClient SOLR_CLIENT_UPDATE

	public void afterPropertiesSet() throws Exception {
		SOLR_BASE_URL = CustomConfig.findByName("SOLR_BASE_URL")?.getValue() ?: "http://localhost"
		SOLR_PORT = CustomConfig.findByName("SOLR_PORT")?.getValue() ?: "8983"
		SOLR_CLUSTER_NAME = CustomConfig.findByName("SOLR_CLUSTER_NAME")?.getValue() ?: "documents"
		SEARCH_INDEX_BATCH_SIZE = CustomConfig.findByName("SEARCH_INDEX_BATCH_SIZE")?.getValue() as Integer ?: 50

		try{
			log.info("Building SOLR request client")
			SOLR_CLIENT_REQUEST = new HttpSolrClient.Builder("${SOLR_BASE_URL}:${SOLR_PORT}/solr")
					.withConnectionTimeout(10000)
					.withSocketTimeout(60000)
					.build()
		}catch(Exception e){
			log.error("Could not connect to Solr Index with url: ${SOLR_BASE_URL}:${SOLR_PORT}/solr")
		}

		try{
			log.info("Building SOLR update client")
			SOLR_CLIENT_UPDATE = new ConcurrentUpdateSolrClient.Builder("${SOLR_BASE_URL}:${SOLR_PORT}/solr")
					.withConnectionTimeout(10000)
					.withSocketTimeout(60000)
					.build()
		}catch(Exception e){
			log.error("Could not connect to Solr Index with url: ${SOLR_BASE_URL}:${SOLR_PORT}/solr")
		}
	}

	@NotTransactional
	def update(def id){
		log.info("Updating search index for Document:${id}")
		UpdateResponse ur

		if(!id){
			log.error("Can't update search index for Document with no Id")
			return ur
		}

		Document doc = Document.get(id)

		if(!doc){
			log.error("Can't update search index with Document:${id}")
			return ur
		}

		try{
			SolrInputDocument sDoc = new SolrInputDocument()
			sDoc.addField("id", doc.id)
			sDoc.addField("state", doc.monitor.region.state.name)
			sDoc.addField("region_id", doc.monitor.region.id)
			sDoc.addField("region", doc.monitor.region.name)
			sDoc.addField("region_type", doc.monitor.region.class.name?.replace("com.councilsearch.", "").toLowerCase())
			sDoc.addField("monitor_id", doc.monitor.id)
			sDoc.addField("title", doc.title)
			sDoc.addField("document_type", doc.getClass().name?.replace("com.councilsearch.","").toLowerCase())
			sDoc.addField("meeting_date", doc.meetingDate)
			sDoc.addField("date_created", doc.dateCreated)
			sDoc.addField("uuid", doc.uuid)
			sDoc.addField("content", doc.content.text ?: "")

			ur = SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)

			// Indexed documents must be committed
			SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
		}catch(Exception e){
			log.error("Could not perform update id:${id} - "+e)
		}

		return ur
	}

	/**
	 * This function has been crashing with OutofMemory exception
	 * bringing in the SQL to help streamline? No clue why think
	 * theres a document over 2gb
	 */
	@NotTransactional
	def update(){
		log.info("Fully updating Solr Index")
		Sql sql = new Sql(dataSource)
		int offset = 0
		def maxId = 0
		def maxIdQuery = """
			SELECT 
				id
			FROM
				document
			ORDER BY id DESC
		"""
		def dataQuery = """
			SELECT
				# Document
				d.id documentId,
				d.title title,
				replace(d.class, "com.councilsearch.", "") documentType,
				d.meeting_date meetingDate,
				d.date_created dateCreated,
				d.uuid uuid,
				d.success,
				c.text text,
				# Monitor
				m.id monitorId,
				# Region
				r.id regionId,
				r.name regionName,
				replace(r.class, "com.councilsearch.", "") regionType,
				# State
				s.name stateName,
				s.abbr stateAbbr
			FROM
				document d
				left join content c on d.id = c.document_id
				left join monitor m on m.id = d.monitor_id
				left join region r on r.id = m.region_id
				left join state s on s.id = r.state_id
		"""

		// Find the largest id for batching
		try {
			maxId = sql.rows(maxIdQuery, 0, 1)?.get(0)?.get("id")
		} catch (Exception e) {
			log.error("Could not query for Search Documents: " + e)
		}

		// Calculate batch number
		log.info("Found a maxId: ${maxId}")

		// Use maxId (or row) to calculate offset
		while(offset < maxId){
			log.info("Processing offset row: ${offset}")

			try {
				def dataItr = sql.rows(dataQuery, offset, SEARCH_INDEX_BATCH_SIZE).iterator()

				// Grab the list iterator and go through the maps
				while(dataItr.hasNext()){
					def data = dataItr.next()

					// Dont upload to Solr if the processing didnt end with a success
					if(!data.get("success")){
						continue
					}

					try{
						SolrInputDocument sDoc = new SolrInputDocument()

						sDoc.addField("id", data.get("documentId"))
						sDoc.addField("state", data.get("stateName"))
						sDoc.addField("state_abbr", data.get("stateAbbr"))
						sDoc.addField("region_id", data.get("regionId"))
						sDoc.addField("region", data.get("regionName"))
						sDoc.addField("region_type", data.get("regionType"))
						sDoc.addField("monitor_id", data.get("monitorId"))
						sDoc.addField("title", data.get("title") ?: "No Title")
						sDoc.addField("document_type", data.get("documentType").replace("com.councilsearch.",""))
						sDoc.addField("meeting_date", data.get("meetingDate"))
						sDoc.addField("date_created", data.get("dateCreated"))
						sDoc.addField("uuid", data.get("uuid"))
						sDoc.addField("content", data.get("text") ?: "")

						SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)
					}catch(Exception e){
						log.error("Could not perform Solr update all for batch:${offset} "+e)
					}

					// Remove it when we are done
					dataItr.remove()
				}
			} catch (Exception e) {
				log.error("Could not add batch of documents from MySQL to Solr: " + e)
			}

			// Commit the batch
			SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
			offset += SEARCH_INDEX_BATCH_SIZE
		}

		sql.close()
		log.info("Finished fully updating Solr Index")
	}

	def delete(String id){
		UpdateResponse ur

		try{
			if(id != null){
				log.info("Delete Search document with Id ${id}")
				SOLR_CLIENT_REQUEST.deleteById(SOLR_CLUSTER_NAME, id)
			}else{
				log.info("No Id specified clearing Solr Cluster ${SOLR_CLUSTER_NAME}")
				SOLR_CLIENT_REQUEST.deleteByQuery(SOLR_CLUSTER_NAME, "*:*")
			}

			ur = SOLR_CLIENT_REQUEST.commit(SOLR_CLUSTER_NAME)
		}catch(Exception e){
			log.error("Could not perform delete: "+e)
		}

		return ur
	}

	def bulkIndex(List docMaps){
		Iterator<Map> dItr = docMaps.iterator()

		while(dItr.hasNext()) {
			Map docMap = dItr.next()

			// Dont upload to Solr if the processing didnt end with a success
			if(!docMap.get("success")){
				continue
			}

			try{
				def id = docMap.get("id") // mandatory solr field
				if(id != null){
					SolrInputDocument sDoc = new SolrInputDocument()
					sDoc.addField("id", id)
					sDoc.addField("state", docMap.get("stateName"))
					sDoc.addField("state_abbr", docMap.get("stateAbbr"))
					sDoc.addField("region_id", docMap.get("regionId"))
					sDoc.addField("region", docMap.get("regionName"))
					sDoc.addField("region_type", docMap.get("regionType"))
					sDoc.addField("monitor_id", docMap.get("monitorId"))
					sDoc.addField("title", docMap.get("title") ?: "No Title")
					sDoc.addField("document_type", docMap.get("docType").replace("com.councilsearch.",""))
					sDoc.addField("meeting_date", docMap.get("date"))
					sDoc.addField("uuid", docMap.get("uuid"))
					sDoc.addField("date_created", new Date())
					sDoc.addField("content", docMap.get("content") ?: "")

					SOLR_CLIENT_UPDATE.add(SOLR_CLUSTER_NAME, sDoc)
				}else{
					log.error("Cannot update Solr index with null Id")
				}
			}catch(Exception e){
				log.error("Could not bulk ETL index: "+e)
			}
		}

		// Indexed documents must be committed
		SOLR_CLIENT_UPDATE.commit(SOLR_CLUSTER_NAME)
	}


	Response request(Request searchRequest){
		SolrQuery solrQuery = searchRequest.createSolrQuery()
		QueryResponse queryResponse

		try {
			queryResponse = SOLR_CLIENT_REQUEST.query(SOLR_CLUSTER_NAME, solrQuery)
		} catch (Exception e) {
			log.error("Could not query Solr"+ e)
		}

		Response response = new Response(queryResponse)

		return response
	}


























	//////////////Hijacking this project
//	def processPSTFile(File file){
//		File outputFile = new File("C:\\birch-home\\gig\\output.csv")
////		outputFile << "body,bodyHTML,bodyPrefix,clientSubmitTime,conversationId,conversationIndex,conversationTopic,displayBCC,displayCC,displayTo,inReplyToId,internetArticleNumber,internetMessageId,messageCcMe,messageClass,messageDeliveryTime,messageRecipMe,messageSize,messageToMe,nativeBodyType,nextSendAcct,numberOfAttachments,numberOfRecipients,objectType,originalDisplayBcc,originalDisplayCc,originalDisplayTo,originalSensitivity,originalSubject,primarySendAccount,priority,receivedByAddress,receivedByAddressType,receivedByName,recipientsString,recipientType,reminderSet,replyRecipientNames,responseRequested,responsibility,returnPath,rTFBody,senderAddrtype,senderEmailAddress,senderEntryId,senderName,sensitivity,sentRepresentingAddressType,sentRepresentingAddrtype,sentRepresentingEmailAddress,sentRepresentingName,subject,taskDueDate,taskStartDate,transportMessageHeaders,uRLCompName,uRLCompNamePostfix\n"
//		outputFile << "bodyPrefix,clientSubmitTime,conversationId,conversationIndex,conversationTopic,displayBCC,displayCC,displayTo,inReplyToId,internetArticleNumber,internetMessageId,messageCcMe,messageClass,messageDeliveryTime,messageRecipMe,messageSize,messageToMe,nativeBodyType,nextSendAcct,numberOfAttachments,numberOfRecipients,objectType,originalDisplayBcc,originalDisplayCc,originalDisplayTo,originalSensitivity,originalSubject,primarySendAccount,priority,receivedByAddress,receivedByAddressType,receivedByName,recipientsString,recipientType,reminderSet,replyRecipientNames,responseRequested,responsibility,returnPath,rTFBody,senderAddrtype,senderEmailAddress,senderEntryId,senderName,sensitivity,sentRepresentingAddressType,sentRepresentingAddrtype,sentRepresentingEmailAddress,sentRepresentingName,subject,taskDueDate,taskStartDate,transportMessageHeaders,uRLCompName,uRLCompNamePostfix\n"
//		try{
//			PSTFile pstFile = new PSTFile(file)
//			log.info "Display name: "+pstFile.getMessageStore().getDisplayName()
//			log.info("Processing Root folder")
//			PSTFolder folder = pstFile.getRootFolder()
//			processPSTFolder(folder, outputFile, 0)
//		}catch(Exception e){
//			log.error("Error reading PST file: "+e)
//		}
//	}
//
//	def processPSTFolder(folder, outputFile, depth){
////		if(pm2s.size() == 100){
////			log.info("Hundo!")
////		}
//		// and now the emails for this folder
//		if (folder != null && folder.getContentCount() > 0) {
//			PSTObject obj = folder.getNextChild()
//
//			while (obj != null) {
//				String className = obj.getClass()?.name?.replaceAll("com.pff.","")
//				try{
//					// "'TYPE','CREATED','LAST_MODIFIED','EMAIL'\n"
//					if("PSTMessage".equalsIgnoreCase(className)){
//						PSTMessage2 pm2 = createPM2(obj)
//						outputFile << pm2.toString()+"\n"
//					}
////				PSTContact, PSTAttachment, PSTFolder, PSTMessage, PSTMessageStore, PSTActivity, PSTAppointment
//					obj = folder.getNextChild()
//				}catch(Exception e){
//					log.error("Could not process PSTMessage: "+e)
//				}
//			}
//		}
//
//		// go through the sub folders...
//		if (folder.hasSubfolders()) {
//			Vector<PSTFolder> childFolders = folder.getSubFolders()
//			for (PSTFolder childFolder : childFolders) {
//				processPSTFolder(childFolder, outputFile, depth+1)
//			}
//		}
//	}
//
//	def createPM2(PSTMessage pm){
//		PSTMessage2 pm2 = new PSTMessage2()
//
//		//pm2.attachment = pm.getAttachment(int attachmentNumber)
//		pm2.body = pm.getBody()
//		pm2.bodyHTML = pm.getBodyHTML()
//		pm2.bodyPrefix  = pm.getBodyPrefix()
//		pm2.clientSubmitTime = pm.getClientSubmitTime()
//		pm2.conversationId  = pm.getConversationId()
//		pm2.conversationIndex  = pm.getConversationIndex()
//		pm2.conversationTopic = pm.getConversationTopic()
//		pm2.displayBCC = pm.getDisplayBCC()
//		pm2.displayCC = pm.getDisplayCC()
//		pm2.displayTo = pm.getDisplayTo()
//		pm2.inReplyToId = pm.getInReplyToId()
//		pm2.internetArticleNumber = pm.getInternetArticleNumber()
//		pm2.internetMessageId = pm.getInternetMessageId()
//		pm2.messageCcMe = pm.getMessageCcMe()
//		pm2.messageClass = pm.getMessageClass()
//		pm2.messageDeliveryTime = pm.getMessageDeliveryTime()
//		pm2.messageRecipMe = pm.getMessageRecipMe()
//		pm2.messageSize = pm.getMessageSize()
//		pm2.messageToMe = pm.getMessageToMe()
//		pm2.nativeBodyType = pm.getNativeBodyType()
//		pm2.nextSendAcct  = pm.getNextSendAcct()
//		pm2.numberOfAttachments = pm.getNumberOfAttachments()
//		pm2.numberOfRecipients = pm.getNumberOfRecipients()
//		pm2.objectType = pm.getObjectType()
//		pm2.originalDisplayBcc = pm.getOriginalDisplayBcc()
//		pm2.originalDisplayCc = pm.getOriginalDisplayCc()
//		pm2.originalDisplayTo = pm.getOriginalDisplayTo()
//		pm2.originalSensitivity = pm.getOriginalSensitivity()
//		pm2.originalSubject = pm.getOriginalSubject()
//		pm2.primarySendAccount  = pm.getPrimarySendAccount()
//		pm2.priority = pm.getPriority()
//		pm2.receivedByAddress = pm.getReceivedByAddress()
//		pm2.receivedByAddressType = pm.getReceivedByAddressType()
//		pm2.receivedByName = pm.getReceivedByName()
//		//pm2.recipient = pm.getRecipient(int recipientNumber)
//		pm2.recipientsString  = pm.getRecipientsString()
//		pm2.recipientType = pm.getRecipientType()
//		pm2.reminderSet = pm.getReminderSet()
//		pm2.replyRecipientNames = pm.getReplyRecipientNames()
//		pm2.responseRequested = pm.getResponseRequested()
//		pm2.responsibility = pm.getResponsibility()
//		pm2.returnPath = pm.getReturnPath()
//		pm2.rTFBody  = pm.getRTFBody()
//		pm2.senderAddrtype = pm.getSenderAddrtype()
//		pm2.senderEmailAddress = pm.getSenderEmailAddress()
//		pm2.senderEntryId  = pm.getSenderEntryId()
//		pm2.senderName = pm.getSenderName()
//		pm2.sensitivity = pm.getSensitivity()
//		pm2.sentRepresentingAddressType = pm.getSentRepresentingAddressType()
//		pm2.sentRepresentingAddrtype = pm.getSentRepresentingAddrtype()
//		pm2.sentRepresentingEmailAddress = pm.getSentRepresentingEmailAddress()
//		pm2.sentRepresentingName = pm.getSentRepresentingName()
//		pm2.subject = pm.getSubject()
//		pm2.taskDueDate = pm.getTaskDueDate()
//		pm2.taskStartDate = pm.getTaskStartDate()
//		pm2.transportMessageHeaders = pm.getTransportMessageHeaders()
//		pm2.uRLCompName = pm.getURLCompName()
//		pm2.uRLCompNamePostfix = pm.getURLCompNamePostfix()
//
//		return pm2
//	}

}
