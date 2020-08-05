package com.councilsearch.importio

import com.fasterxml.jackson.annotation.JsonProperty

public class ImportioResponse{
	ImportioResponse(){ }

	@JsonProperty("result")
	ImportioResult result
}

class ImportioResult {
	ImportioResult(){}

	@JsonProperty("extractorData")
	ExtractorData extractorData
}

class ExtractorData{
	ExtractorData(){}

	@JsonProperty("data")
	List<Data> datas
}

class Data{
	Data(){}

	@JsonProperty("group")
	List<Group> groups
}

class Group{
	Group(){}

	@JsonProperty("title")
	List<Entity> titles

	@JsonProperty("date")
	List<Entity> dates

	@JsonProperty("agenda")
	List<Entity> agendas

	@JsonProperty("agenda_content")
	List<Entity> agenda_contents

	@JsonProperty("agenda_sup")
	List<Entity> agenda_sups

	@JsonProperty("minute")
	List<Entity> minutes

	@JsonProperty("minute_content")
	List<Entity> minute_contents

	@JsonProperty("minute_sup")
	List<Entity> minute_sups
}

class Entity{
	Entity(){}

	@JsonProperty("text")
	String text
	@JsonProperty("href")
	String href
	@JsonProperty("xpath")
	String xpath
}
