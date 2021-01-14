package com.councilsearch

import grails.gorm.transactions.Transactional
import grails.gsp.PageRenderer
import org.springframework.beans.factory.InitializingBean

@Transactional
class MessageService implements InitializingBean {

	def amazonWebService

	PageRenderer groovyPageRenderer

	String EMAIL_BASE_URL
	Boolean EMAIL_ENABLED
	Boolean EMAIL_OVERRIDE
	String OVERRIDE_ADDRESS
	String FROM_ADDRESS
	String BCC_ADDRESS
	String CONTACT_US_ADDRESS

	public void afterPropertiesSet() throws Exception {
		// Email stuff
		EMAIL_BASE_URL = CustomConfig.findByName("EMAIL_BASE_URL")?.getValue() ?: "https://www.councilsearch.com"
		EMAIL_ENABLED = CustomConfig.findByName("EMAIL_ENABLED")?.getValue() as Boolean ?: false
		EMAIL_OVERRIDE = CustomConfig.findByName("EMAIL_OVERRIDE")?.getValue() as Boolean ?: true
		OVERRIDE_ADDRESS = CustomConfig.findByName("OVERRIDE_ADDRESS")?.getValue() ?: "sean@councilsearch.com"
		FROM_ADDRESS = CustomConfig.findByName("FROM_ADDRESS")?.getValue() ?: "sean@councilsearch.com"
		BCC_ADDRESS = CustomConfig.findByName("BCC_ADDRESS")?.getValue() ?: "sean@councilsearch.com"
		CONTACT_US_ADDRESS = CustomConfig.findByName("CONTACT_US_ADDRESS")?.getValue() ?: "sean@councilsearch.com"
	}

	def contact(Lead lead) {
		log.info("Sending notification for Lead:${lead.id}")
		def subject = "Website form submitted by: ${lead.name}"

		try{
			amazonWebService.email(CONTACT_US_ADDRESS, null, FROM_ADDRESS, subject, lead.toString())
		}catch(Exception e){
			log.error("Could not send contact email for Lead:${lead.id} - "+e)
		}
    }

	def sendNotifications(List messages){
		if(!EMAIL_ENABLED){
			log.info("Email is disabled for this system")
			return
		}

		Iterator mItr = messages.iterator()

		while(mItr.hasNext()){
			Map messageMap = mItr.next()
			def toAddress = messageMap.get("email")
			def messageBody = groovyPageRenderer.render(view: "/emails/notifications/eventNotification", model: [baseUrl: EMAIL_BASE_URL, messageMap: messageMap])

			// Override - mostly for testing
			if(EMAIL_OVERRIDE){
				log.info("Using email override address: ${OVERRIDE_ADDRESS}")
				toAddress = OVERRIDE_ADDRESS
			}

			try{
				amazonWebService.email(toAddress, BCC_ADDRESS, FROM_ADDRESS, messageMap.get("subject"), messageBody)
			}catch(Exception e){
				log.error("Could not send notification: "+e)
			}
		}

	}
}
