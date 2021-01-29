package com.councilsearch

import grails.converters.JSON

class RegionController {

	def list() {
		def abbr = params.abbr
		List regions = []
		def converter

		if(!abbr){
			response.status = 400
			render ([error: "State abbreviation not specified"] as JSON)
			return
		}

		State state = State.findByAbbr(abbr)

		if(!state){
			response.status = 400
			render ([error: "State with abbreviation:${abbr} not found"] as JSON)
			return
		}

		regions += Agency.findAllByState(state)
		regions += County.findAllByState(state)
		regions += Place.findAllByState(state)

		regions.sort{ it.name }

		converter = regions as JSON

		// Exclude relationships properties
		converter.setExcludes(Region.class, ["state", "monitors"])

		converter.render(response)
	}

	def show() {
		def id = params.id

		if(!id){
			response.status = 400
			render ([error: "Region Id not specified"] as JSON)
			return
		}

		Region region = Region.get(id)

		if(!region){
			response.status = 400
			render ([error: "Region with id:${id} not found"] as JSON)
			return
		}

		render region as JSON
	}


	def error(){
		response.status = 403
		render ([error: "Action not allowed"] as JSON)
		return
	}

	// Legacy import data
//	def importPlace(){
//		def jsonSlurper = new JsonSlurper()
//		File sql = new File("c:\\Users\\SEAN\\Desktop\\place.sql")
//		def data = jsonSlurper.parseText(new File("c:\\Users\\SEAN\\Desktop\\placeJson.json").text)
//		Map stateMap = ["Alabama":1,
//						"Alaska":2,
//						"Arizona":3,
//						"Arkansas":4,
//						"California":5,
//						"Colorado":6,
//						"Connecticut":7,
//						"Delaware":8,
//						"Florida":9,
//						"Georgia":10,
//						"Hawaii":11,
//						"Idaho":12,
//						"Illinois":13,
//						"Indiana":14,
//						"Iowa":15,
//						"Kansas":16,
//						"Kentucky":17,
//						"Louisiana":18,
//						"Maine":19,
//						"Maryland":20,
//						"Massachusetts":21,
//						"Michigan":22,
//						"Minnesota":23,
//						"Mississippi":24,
//						"Missouri":25,
//						"Montana":26,
//						"Nebraska":27,
//						"Nevada":28,
//						"New Hampshire":29,
//						"New Jersey":30,
//						"New Mexico":31,
//						"New York":32,
//						"North Carolina":33,
//						"North Dakota":34,
//						"Ohio":35,
//						"Oklahoma":36,
//						"Oregon":37,
//						"Pennsylvania":38,
//						"Rhode Island":39,
//						"South Carolina":40,
//						"South Dakota":41,
//						"Tennessee":42,
//						"Texas":43,
//						"Utah":44,
//						"Vermont":45,
//						"Virginia":46,
//						"Washington":47,
//						"West Virginia":48,
//						"Wisconsin":49,
//						"Wyoming":50]
//
//
//		data.each{
//			if(!"72".equals(it[2])) {
//				def fips = "${it[2]}${it[3]}"
//				def fullName = it[0].split(",")
//				def placeName = fullName[0]?.replaceAll("'","").trim()
//				def stateName = fullName[1].trim()
//				def pop = it[1]
//				def stateId = stateMap.get(stateName) ?: "NONE"
//				def censusType = "sean0689"
//
//				if (placeName.contains("city")) {
//					placeName = placeName.replace("city", "").trim()
//					censusType = "city"
//				} else if (placeName.contains("town")) {
//					placeName = placeName.replace("town", "").trim()
//					censusType = "town"
//				} else if (placeName.contains("CDP")) {
//					placeName = placeName.replace("CDP", "").trim()
//					censusType = "CDP"
//				} else if (placeName.contains("borough")) {
//					placeName = placeName.replace("borough", "").trim()
//					censusType = "borough"
//				} else if (placeName.contains("village")) {
//					placeName = placeName.replace("village", "").trim()
//					censusType = "village"
//				} else if (placeName.contains("municipality")) {
//					placeName = placeName.replace("municipality", "").trim()
//					censusType = "municipality"
//				} else if (placeName.contains("consolidated government")) {
//					placeName = placeName.replace("consolidated government", "").trim()
//					censusType = "consolidated government"
//				} else if (placeName.contains("unified government")) {
//					placeName = placeName.replace("unified government", "").trim()
//					censusType = "unified government"
//				} else if (placeName.contains("metropolitan government")) {
//					placeName = placeName.replace("metropolitan government", "").trim()
//					censusType = "metropolitan government"
//				}else if (placeName.contains("urban county")) {
//					placeName = placeName.replace("urban county", "").trim()
//					censusType = "urban county"
//				}else {
//					censusType = "Not Available"
//				}
//
//				// (`fips_code`,`name`,`census_type`,`population`,`state_id`,`class`, `version`,`date_created`,`last_modified`)
//				sql << "(${fips}, '${placeName}', '${censusType}', ${pop}, ${stateId}, 'com.birchsoftware.Place', 1, now(), now()),\n"
//			}
//		}
//
//		render "done"
//	}
//
//	def importCounty(){
//		def jsonSlurper = new JsonSlurper()
//		File sql = new File("c:\\Users\\SEAN\\Desktop\\counties.sql")
//		def data = jsonSlurper.parseText(new File("c:\\Users\\SEAN\\Desktop\\counties.json").text)
//		Map stateMap = ["Alabama":1,
//						"Alaska":2,
//						"Arizona":3,
//						"Arkansas":4,
//						"California":5,
//						"Colorado":6,
//						"Connecticut":7,
//						"Delaware":8,
//						"Florida":9,
//						"Georgia":10,
//						"Hawaii":11,
//						"Idaho":12,
//						"Illinois":13,
//						"Indiana":14,
//						"Iowa":15,
//						"Kansas":16,
//						"Kentucky":17,
//						"Louisiana":18,
//						"Maine":19,
//						"Maryland":20,
//						"Massachusetts":21,
//						"Michigan":22,
//						"Minnesota":23,
//						"Mississippi":24,
//						"Missouri":25,
//						"Montana":26,
//						"Nebraska":27,
//						"Nevada":28,
//						"New Hampshire":29,
//						"New Jersey":30,
//						"New Mexico":31,
//						"New York":32,
//						"North Carolina":33,
//						"North Dakota":34,
//						"Ohio":35,
//						"Oklahoma":36,
//						"Oregon":37,
//						"Pennsylvania":38,
//						"Rhode Island":39,
//						"South Carolina":40,
//						"South Dakota":41,
//						"Tennessee":42,
//						"Texas":43,
//						"Utah":44,
//						"Vermont":45,
//						"Virginia":46,
//						"Washington":47,
//						"West Virginia":48,
//						"Wisconsin":49,
//						"Wyoming":50]
//
//
//		data.each{
//			if(!"72".equals(it[2])) {
//				def fips = "${it[2]}${it[3]}"
//				def fullName = it[0].split(",")
//				def placeName = fullName[0]?.replaceAll("'","").trim()
//				def stateName = fullName[1].trim()
//				def pop = it[1]
//				def stateId = stateMap.get(stateName) ?: "NONE"
//				def censusType = "County"
//
//				// (`fips_code`,`name`,`census_type`,`population`,`state_id`,`class`, `version`,`date_created`,`last_modified`)
//				sql << "(${fips}, '${placeName}', '${censusType}', ${pop}, ${stateId}, 'com.birchsoftware.County', 1, now(), now()),\n"
//			}
//		}
//
//		render "done"
//	}
}
