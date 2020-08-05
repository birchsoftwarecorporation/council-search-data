package com.councilsearch.search

class Filter {
	String name
	Long count
	Boolean selected

	Filter(String name, Long count){
		this.name = name
		this.count = count
		this.selected = false
	}
}
