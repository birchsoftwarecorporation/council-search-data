package com.councilsearch

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
@EqualsAndHashCode(includes='username')
@ToString(includes='username', includeNames=true, includePackage=false)
class User implements Serializable {

    private static final long serialVersionUID = 1

    String username
	String password
	boolean enabled = true
	boolean accountExpired
	boolean accountLocked
	boolean passwordExpired
	String firstName
	String lastName
	boolean emailActive

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    static constraints = {
		username nullable: false, blank: false, unique: true
        password nullable: false, blank: false, password: true
		emailActive defaultValue: true
    }

    static mapping = {
	    password column: '`password`'
    }

	static belongsTo =  [organization: Organization]

//	static hasMany = [alerts: Alert]
}
