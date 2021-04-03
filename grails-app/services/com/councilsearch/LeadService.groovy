package com.councilsearch

import grails.gorm.transactions.Transactional

@Transactional
class LeadService {

	def messageService

    def save(def leadJSON) throws Exception {
		Lead lead = new Lead(leadJSON)

		if(!lead.save()){
			log.error("Unable to save Lead: "+lead.errors)
			throw new Exception("Unable to save Lead")
		}

		// Notify us of the inquiry
		messageService.contact(lead)

		return lead
    }
}
