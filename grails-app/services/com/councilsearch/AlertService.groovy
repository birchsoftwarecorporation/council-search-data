package com.councilsearch

import grails.gorm.transactions.Transactional

@Transactional
class AlertService {
	def queryService
	def extractTransferLoadService

    def create(def alertJson, User manager) {
		if(alertJson.name == null || "".equals(alertJson.name)){
			log.error("Could not create alert for User:${manager.id} name field missing")
			return null
		}

		Integer imgNum = Math.abs(new Random().nextInt() % 10) + 1
		Alert alert = new Alert(name: alertJson.name, status: "live", manager: manager, image: imgNum.toString())

		// Add phrases
		if(alertJson.phrases == null || alertJson.phrases?.size() < 1){
			log.error("Could not create alert for User:${manager.id} with no phrases")
			return null
		}

		alertJson.phrases?.collect{ it.toLowerCase() }?.unique()?.each { p ->
			// Clean it a bit
			p = p.replaceAll("[^a-zA-Z0-9,&\\)\\(]+",' ').replaceAll("\\s+"," ").trim()
			Phrase phrase = new Phrase(name: p)
			alert.addToPhrases(phrase)
		}

		// Add Regions
		if(alertJson.regions == null || alertJson.regions?.size() < 1){
			log.error("Could not create alert for User:${manager.id} with no Regions")
			return null
		}

		alertJson.regions?.unique()?.each{ rId ->
			Region region = Region.findById(rId)

			if(region){
				alert.addToRegions(region)
			}
		}

		// Add the other members of this alert
		alertJson.members?.unique()?.each{ mId ->
			User member = User.get(mId)

			// Only members of the same organization can share
			if(member != null && manager.organization == member.organization){
				alert.addToMembers(member)
			}
		}

		if(!alert.save()){
			log.error("Could not create alert for User:${manager.id} "+alert.errors)
			return null
		}

		return alert
	}

	// We dont delete we only hide it
	def remove(def alertId){
		Alert alert = Alert.get(alertId)

		alert.status = "removed"

		if(!alert.save()){
			log.error("Could not mark Alert:${alertId} as removed "+alert.errors)
		}
	}

	def getAllAlerts(User manager){
		List alerts = []
		List managerAlertIds = queryService.getAlertIdsByUser(manager.id)
		List collabAlertIds = queryService.getAlertIdsByMember(manager.id)

		// Build the alert list - managed
		managerAlertIds.each { alertRow ->
			Alert alert = Alert.get(alertRow.get("alertId"))

			// Skip the ones marked as removed
			if(!"removed".equalsIgnoreCase(alert.status)){
				Map alertMap = [:]

				alertMap.put("id", alert.id)
				alertMap.put("name", alert.name)
				alertMap.put("isManager", true)
				alertMap.put("status", alert.status)
				alertMap.put("dateCreated", alert.dateCreated)
				alertMap.put("lastUpdated", alert.lastUpdated)
				alertMap.put("manager", alert.manager.firstName+" "+alert.manager.lastName)
				alertMap.put("numMembers", alert.members?.size())
				alertMap.put("numRegions", alert.regions?.size())
				alertMap.put("numPhrases", alert.phrases?.size())
				alertMap.put("image", alert.image)

				alerts.add(alertMap)
			}
		}

		// Build the alert list - member
		// TODO - little redundant but whatever fix it later
		collabAlertIds.each { alertRow ->
			Alert alert = Alert.get(alertRow.get("alertId"))

			// Skip the ones marked as removed
			if(!"removed".equalsIgnoreCase(alert.status)) {
				Map alertMap = [:]

				alertMap.put("id", alert.id)
				alertMap.put("name", alert.name)
				alertMap.put("isManager", false)
				alertMap.put("status", alert.status)
				alertMap.put("dateCreated", alert.dateCreated)
				alertMap.put("lastUpdated", alert.lastUpdated)
				alertMap.put("manager", alert.manager.firstName + " " + alert.manager.lastName)
				alertMap.put("numMembers", alert.members?.size())
				alertMap.put("numRegions", alert.regions?.size())
				alertMap.put("numPhrases", alert.phrases?.size())
				alertMap.put("image", alert.image)

				alerts.add(alertMap)
			}
		}

		return alerts
	}

	def process(def alertId){
		// Process the alert
		extractTransferLoadService.processAlerts(alertId)
		// Create the new events
		extractTransferLoadService.createEvents(alertId)
	}
}
