package com.councilsearch.search

class Facet {
	List<Filter> filters

	Facet(){
		filters = []
	}

//	Facet(String name){
//		this.name = name
//		filters = []
//	}

	void addFilter(Filter filter){
		filters.add(filter)
	}
}
