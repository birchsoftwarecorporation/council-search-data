package com.councilsearch

import grails.gorm.transactions.Transactional

@Transactional
class AlertService {
	def queryService

    def create(def alertJson, User manager) {
		if(alertJson.name == null || "".equals(alertJson.name)){
			log.error("Could not create alert for User:${manager.id} name field missing")
			return null
		}

		Alert alert = new Alert(name: alertJson.name, status: "Live", manager: manager)

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
			User member = User.findById(mId)

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

	def getAllAlerts(User manager){
		List alerts = []
		List managerAlertIds = queryService.getAlertIdsByUser(manager.id)
		List collabAlertIds = queryService.getAlertIdsByMember(manager.id)

		// Build the alert list - managed
		managerAlertIds.each { alertRow ->
			Alert alert = Alert.get(alertRow.get("alertId"))
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

			alerts.add(alertMap)
		}

		// Build the alert list - member
		// TODO - little redundant but whatever fix it later
		collabAlertIds.each { alertRow ->
			Alert alert = Alert.get(alertRow.get("alertId"))
			Map alertMap = [:]

			alertMap.put("id", alert.id)
			alertMap.put("name", alert.name)
			alertMap.put("isManager", false)
			alertMap.put("status", alert.status)
			alertMap.put("dateCreated", alert.dateCreated)
			alertMap.put("lastUpdated", alert.lastUpdated)
			alertMap.put("manager", alert.manager.firstName+" "+alert.manager.lastName)
			alertMap.put("numMembers", alert.members?.size())
			alertMap.put("numRegions", alert.regions?.size())
			alertMap.put("numPhrases", alert.phrases?.size())

			alerts.add(alertMap)
		}

		return alerts
	}
}
