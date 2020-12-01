package com.councilsearch

import grails.gorm.transactions.Transactional

@Transactional
class EventService {

	def queryService

	/* Grab all events with assoc details */
    def getEvents(User user) {
		List eventMaps = []

		queryService.getEventsByUser(user.id).unique().each{ eventId ->
			Event event = Event.get(eventId)

			// Move on if the Alert is marked "removed"
			if(event != null && !"removed".equalsIgnoreCase(event.match.alert.status)){
				Document document = event.match?.document
				Map eventMap = [:]

				eventMap.put("eventId", event.id)
				eventMap.put("status", event.status)
				eventMap.put("description", event.description)
				eventMap.put("uuid", event.uuid)
				eventMap.put("meetingDate", document.meetingDate)
				eventMap.put("documentId", document.id)
				eventMap.put("documentTitle", document.title)
				eventMap.put("documentType", document.getClass().name?.replace("com.councilsearch.",""))
				eventMap.put("alertName", event.match.alert.name)
				eventMap.put("matchCount", event.match.previews?.size())
				eventMap.put("commentCount", event.comments?.size())
				eventMap.put("regionName", event.match.document.monitor?.region.name)
				eventMap.put("stateAbbr", event.match.document.monitor?.region.state.abbr?.toUpperCase())

				// Add the owner
				Map uMap = [:]

				uMap.put("id", event.owner.id)
				uMap.put("firstName", event.owner.firstName)
				uMap.put("lastName", event.owner.lastName)
				uMap.put("email", event.owner.username)

				eventMap.put("owner", uMap)

				// Add the previews
				eventMap.put("previews", groupPreviews(event.match.previews))

				eventMaps.add(eventMap)
			}
		}

		log.info("Found ${eventMaps.size()} events for User:${user.username}")

		return eventMaps
    }

	def getEvent(def uuid){
		Map eventMap = [:]
		Event event = Event.findByUuid(uuid)

		// If we dont have an even return null so we can error in the controller properly
		if(event == null){
			return null
		}

		Document document = event.match?.document

		eventMap.put("eventId", event.id)
		eventMap.put("status", event.status)
		eventMap.put("description", event.description ?: "")
		eventMap.put("uuid", event.uuid)
		eventMap.put("meetingDate", document.meetingDate)
		eventMap.put("documentUUID", document.uuid)
		eventMap.put("documentTitle", document.title)
		eventMap.put("documentType", document.getClass().name?.replace("com.councilsearch.",""))
		eventMap.put("alertName", event.match.alert.name)
		eventMap.put("regionName", event.match.document.monitor?.region.name)
		eventMap.put("stateAbbr", event.match.document.monitor?.region.state.abbr?.toUpperCase())

		// Add the previews
		eventMap.put("previews", groupPreviews(event.match.previews))

		// Add the owner
		Map uMap = [:]

		uMap.put("id", event.owner.id)
		uMap.put("firstName", event.owner.firstName)
		uMap.put("lastName", event.owner.lastName)
		uMap.put("email", event.owner.username)

		eventMap.put("owner", uMap)

		// Add the comments
		List comments = []

		event.comments.sort{ it.dateCreated }.reverse().each { comment ->
			Map cMap = [:]
			cMap.put("id", comment.owner.id)
			cMap.put("firstName", comment.owner.firstName)
			cMap.put("lastName", comment.owner.lastName)
			cMap.put("dateCreated", comment.dateCreated)
			cMap.put("text", comment.text)

			comments.add(cMap)
		}

		eventMap.put("comments", comments)

		return eventMap
	}

	def groupPreviews(def previews){
		Map pMap = [:]

		// Lets group the previews by phrase
		previews.each { preview ->
			// Exists
			if(pMap.containsKey(preview.phraseHit?.toLowerCase())){
				// Get the previously addeds
				List texts = pMap.get(preview.phraseHit.toLowerCase())
				// Add the new
				texts.add(preview.text.toLowerCase())
				// Re add to map
				pMap.put(preview.phraseHit.toLowerCase(), texts)
			}else{ // create it
				List texts = []
				texts.add(preview.text.toLowerCase())
				pMap.put(preview.phraseHit.toLowerCase(), texts)
			}
		}

		return pMap
	}

	def updateDescription(def eventUUID, def text) throws Exception{
		Event event = Event.findByUuid(eventUUID)

		if(!event){
			throw new Exception("Could not find Event:${eventUUID}")
		}

		event.description = text

		if(!event.save(flush: true)){
			throw new Exception("Could not update Event:${eventUUID} with new description "+event.errors)
		}

	}

	def createComment(User user, def eventUUID, def text) throws Exception {
		log.info("Creating Comment for User:${user.id} and Event:${eventUUID} with data:${text}")

		if(eventUUID == null){
			throw new Exception("Event UUID not found")
		}

		Event event = Event.findByUuid(eventUUID)

		if(!event){
			throw new Exception("Event:${eventUUID} not found")
		}

		Comment comment = new Comment(owner: user, text: text)
		event.addToComments(comment)

		if(!event.save(flush: true)){
			throw new Exception("Could not create Comment for Event:${event.uuid} and User:${user.id} "+event.errors)
		}

		return comment
	}

	def getMembers(def eventUUID) throws Exception {
		List members = []

		if(eventUUID == null){
			throw new Exception("Event UUID not found")
		}

		Event event = Event.findByUuid(eventUUID)

		if(!event){
			throw new Exception("Event:${eventUUID} not found")
		}

		Map uMap = [:]

		// Add the owner
		if(event.owner.enabled){
			uMap.put("id", event.owner.id)
			uMap.put("firstName", event.owner.firstName)
			uMap.put("lastName", event.owner.lastName)
			uMap.put("email", event.owner.username)

			members.add(uMap)
		}

		event.members.each{ member ->
			uMap = [:]

			if(member.enabled){
				uMap.put("id", member.id)
				uMap.put("firstName", member.firstName)
				uMap.put("lastName", member.lastName)
				uMap.put("email", member.username)

				members.add(uMap)
			}
		}

		return members
	}

	def updateOwner(def eventUUID, def ownerId) throws Exception{
		Event event = Event.findByUuid(eventUUID)
		User owner = User.get(ownerId)

		if(!event){
			throw new Exception("Could not find Event:${eventUUID}")
		}

		if(!owner){
			throw new Exception("Could not find User:${ownerId}")
		}

		event.owner = owner

		if(!event.save(flush: true)){
			throw new Exception("Could not update Event:${eventUUID} with new owner "+event.errors)
		}

	}

	def updateStatus(def eventUUID, def status) throws Exception{
		Event event = Event.findByUuid(eventUUID)

		if(!event){
			throw new Exception("Could not find Event:${eventUUID}")
		}

		event.status = status

		if(!event.save(flush: true)){
			throw new Exception("Could not update Event:${eventUUID} with new status "+event.errors)
		}
	}

	def markEmailed(def eventId, Date date) throws Exception{
		log.info("Marking Event:${eventId} as emailed")
		Event event = Event.get(eventId)

		if(!event){
			throw new Exception("Could not mark Event:${eventId} as emailed, it doesnt exist")
		}

		event.emailed = date

		if(!event.save(flush: true)){
			throw new Exception("Could not mark Event:${eventId} as emailed "+event.errors)
		}
	}
}
