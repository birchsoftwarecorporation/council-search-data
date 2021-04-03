package com.councilsearch

import grails.gorm.transactions.Transactional

@Transactional
class ActivityService {

    def create(String name, String details, String ipAddress) throws Exception {
		log.info("Creating activity ${name} - ${details} - ${ipAddress}")

		Activity activity = new Activity(name: name, details: details, ipAddress: ipAddress)

		if(!activity.save(flush: true)){
			log.error("Could not create Activity: "+ activity.errors)
			throw new Exception("Could not create Activity")
		}

		return activity
    }
}
