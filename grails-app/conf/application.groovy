// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.rest.login.endpointUrl='/api/login'
grails.plugin.springsecurity.rest.logout.endpointUrl = '/api/logout'
grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.rest.token.validation.useBearerToken = true
grails.plugin.springsecurity.rest.login.useJsonCredentials=true
grails.plugin.springsecurity.rest.token.validation.enableAnonymousAccess = true
grails.plugin.springsecurity.rest.login.active=true
grails.plugin.springsecurity.rest.login.failureStatusCode=401
grails.plugin.springsecurity.rest.token.storage.jwt.secret='fuckinsecretkeylkjsadfsdfklsdjgibberish980923howtheprobabyhavetochange2309840932'
grails.plugin.springsecurity.providerNames=[ 'daoAuthenticationProvider','anonymousAuthenticationProvider']
grails.plugin.springsecurity.userLookup.userDomainClassName = 'com.councilsearch.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'com.councilsearch.UserRole'
grails.plugin.springsecurity.authority.className = 'com.councilsearch.Role'
grails.plugin.springsecurity.securityConfigType = 'InterceptUrlMap'

grails.plugin.springsecurity.interceptUrlMap = [
		[pattern: '/api/guest/**',   access: ['permitAll']],
		[pattern: '/api/login', 	 access: ['permitAll']],
		[pattern: '/api/logout',     access: ['permitAll']],
		[pattern: '/api/**',     access: ['ROLE_USER', 'ROLE_ADMIN']],
		[pattern: '/error',          access: ['permitAll']],
		[pattern: '/oauth/**',   access: ['permitAll']],
		[pattern: '/**',             access: []]
]

grails.plugin.springsecurity.filterChain.chainMap = [
		[pattern: '/api/guest/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
		[pattern: '/api/**', filters:'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter'],
		[pattern: '/**', filters:'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter']
]
