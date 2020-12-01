package com.councilsearch


import grails.rest.*
import grails.converters.*

class CommentController {
	static responseFormats = ['json', 'xml']

	def springSecurityService
	def commentService


}
