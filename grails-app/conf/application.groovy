// Added by the Spring Security Core plugin:

grails.plugin.springsecurity.rest.login.endpointUrl='/api/login'
grails.plugin.springsecurity.rest.logout.endpointUrl = '/api/logout'
grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.rest.token.validation.useBearerToken = true
grails.plugin.springsecurity.rest.token.validation.headerName = 'X-Auth-Token'
grails.plugin.springsecurity.rest.login.useJsonCredentials=true
grails.plugin.springsecurity.rest.token.validation.enableAnonymousAccess = true
grails.plugin.springsecurity.rest.login.usernamePropertyName='email'
grails.plugin.springsecurity.rest.login.passwordPropertyName='password'
grails.plugin.springsecurity.rest.login.active=true
grails.plugin.springsecurity.rest.login.failureStatusCode=401
grails.plugin.springsecurity.rest.token.storage.jwt.secret='fuckinsecretkeylkjsadfsdfklsdjgibberish980923howtheprobabyhavetochange2309840932'

grails.plugin.springsecurity.providerNames=[ 'daoAuthenticationProvider','anonymousAuthenticationProvider']
grails.plugin.springsecurity.userLookup.userDomainClassName = 'com.councilsearch.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'com.councilsearch.UserRole'
grails.plugin.springsecurity.authority.className = 'com.councilsearch.Role'

//grails.plugin.springsecurity.rest.token.storage.useMemcached = true
//grails.plugin.springsecurity.rest.token.storage.memcached.hosts = 'localhost:11211'
//grails.plugin.springsecurity.rest.token.storage.memcached.expiration = 86400
//grails.plugin.springsecurity.rest.token.storage.memcached.username = ''
//grails.plugin.springsecurity.rest.token.storage.memcached.password = ''

grails.plugin.springsecurity.controllerAnnotations.staticRules = [
		[pattern: '/', access: ['ROLE_ADMIN']],
		[pattern: '/auth/**', access: ['permitAll']],
		[pattern: '/oauth/**', access: ['permitAll']],
		[pattern: '/api/**', access: ['isFullyAuthenticated()']],
		[pattern: '/**', access: ['isFullyAuthenticated()']],
]

grails.plugin.springsecurity.filterChain.chainMap = [
		[pattern: '/api/**', filters:'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter'],
		[pattern: '/**', filters:'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter']
]

//
//grails.plugin.springsecurity.userLookup.userDomainClassName = 'com.councilsearch.User'
//grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'com.councilsearch.UserRole'
//grails.plugin.springsecurity.authority.className = 'com.councilsearch.Role'
//grails.plugin.springsecurity.userLookup.usernamePropertyName = 'email'
//grails.plugin.springsecurity.rest.login.useJsonCredentials = true
//grails.plugin.springsecurity.rest.login.usernamePropertyName = 'email'
//grails.plugin.springsecurity.rest.login.passwordPropertyName = 'password'
//grails.plugin.springsecurity.rest.token.validation.enableAnonymousAccess = true
//grails.plugin.springsecurity.rest.token.storage.jwt.secret = 'fuckinsecretkeylkjsadfsdfklsdjgibberish980923howtheprobabyhavetochange2309840932'
//grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"


//
//grails.plugin.springsecurity.interceptUrlMap = [
//		[pattern: '/error',          access: ['permitAll']],
//
////		[pattern: '/api/alert',   access: ['ROLE_USER', 'ROLE_ADMIN']],
////		[pattern: '/api/alert',   access: ['isFullyAuthenticated()']],
//		[pattern: '/oauth/**',   access: ['permitAll']],
//		[pattern: '/api/oauth/**',   access: ['permitAll']],
//		[pattern: '/api/login', 	 access: ['permitAll']],
//		[pattern: '/api/logout',     access: ['isFullyAuthenticated()']],
//		[pattern: '/**',             access: ['ROLE_USER', 'ROLE_ADMIN']]
//]


//
//grails.plugin.springsecurity.filterChain.chainMap = [
//	[pattern: '/api/guest/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
////	[pattern: '/api/**', filters:'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter'],
//	[pattern: '/api/**', filters:'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter'],
//	[pattern: '/oauth/**', filters:'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
//	[pattern: '/**', filters: 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter']
//]
