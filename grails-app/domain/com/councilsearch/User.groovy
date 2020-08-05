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
    String email
	String password
	boolean enabled = true
	boolean accountExpired
	boolean accountLocked
	boolean passwordExpired
	String firstName
	String lastName
	boolean emailActive

	User(String username,String firstName, String lastName, String email, String password) {
		this()
		this.username = username
		this.firstName = firstName
		this.lastName = lastName
		this.email = email
		this.password = password
		// Defaults
		this.emailActive = true
		this.enabled = true
		this.accountExpired = false
		this.accountLocked = false
		this.passwordExpired = false
	}

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    static constraints = {
		username nullable: false, blank: false, unique: true
        password nullable: false, blank: false, password: true
		email nullable: false, blank: false, unique: true
		emailActive defaultValue: true
    }

    static mapping = {
	    password column: '`password`'
    }

	static belongsTo =  [organization: Organization]

//	static hasMany = [alerts: Alert]
}
