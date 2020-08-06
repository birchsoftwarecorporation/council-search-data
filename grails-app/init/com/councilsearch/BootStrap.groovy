package com.councilsearch

class BootStrap {

    def init = { servletContext ->
		// Create default roles
		def adminRole = Role.findOrCreateByAuthority('ROLE_ADMIN').save()
		def userRole = Role.findOrCreateByAuthority('ROLE_USER').save()
		def anonymousRole = Role.findOrCreateByAuthority('IS_AUTHENTICATED_ANONYMOUSLY').save()
		def managerRole = Role.findOrCreateByAuthority('ROLE_MANAGER').save()

		// Organization
		def org = Organization.findOrCreateByName("Council Search")

		User admin = User.findByUsername("admin@councilsearch.com")

		if(!admin){
			admin = new User(
					firstName: "The",
					lastName: "Admin",
					username: "admin@councilsearch.com",
					password: "kireland")
			admin.save(true)
			// Add to org
			org.addToEmployees(admin)
			org.save()
	//		UserRole.create(admin,adminRole,true)
		}

		User sean = User.findByUsername("sean@councilsearch.com")

		if(!sean){
			sean = new User(
					firstName: "Sean",
					lastName: "Ogden",
					username: "sean@councilsearch.com",
					password: "kireland")

			// Add to org
			org.addToEmployees(sean)
			org.save()
		}

		User justin = User.findByUsername("sean@councilsearch.com")

		if(!justin){
			justin = new User(
					firstName: "Justin",
					lastName: "Ogden",
					username: "justin@councilsearch.com",
					password: "kireland")

			// Add to org
			org.addToEmployees(justin)
			org.save()
		}

    }
    def destroy = {
    }
}
