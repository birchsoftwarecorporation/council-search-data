package com.councilsearch

import grails.gorm.transactions.Transactional

@Transactional
class MatchService {

    def listRecent(User owner, String stateAbbr, String template, int count) {
		Alert alert = Alert.findByManagerAndName(owner, "${stateAbbr?.toLowerCase()}-${template?.toLowerCase()}")
		List matches = Match.findAllByAlert(alert, [sort: 'dateCreated', order: 'desc', max: count])

		return matches
    }
}
