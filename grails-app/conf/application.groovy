// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'com.councilsearch.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'com.councilsearch.UserRole'
grails.plugin.springsecurity.authority.className = 'com.councilsearch.Role'
grails.plugin.springsecurity.userLookup.usernamePropertyName = 'email'
grails.plugin.springsecurity.rest.login.useJsonCredentials = true
grails.plugin.springsecurity.rest.login.usernamePropertyName = 'email'
grails.plugin.springsecurity.rest.login.passwordPropertyName = 'password'
grails.plugin.springsecurity.rest.token.validation.enableAnonymousAccess = true
grails.plugin.springsecurity.rest.token.storage.jwt.secret = 'fuckinsecretkeylkjsadfsdfklsdjgibberish980923howtheprobabyhavetochange2309840932'
grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"
grails.plugin.springsecurity.interceptUrlMap = [
		[pattern: '/error',          access: ['permitAll']],
		[pattern: '/api/alert',   access: ['ROLE_USER', 'ROLE_ADMIN']],
		[pattern: '/oauth/**',   access: ['permitAll']],
		[pattern: '/api/oauth/**',   access: ['permitAll']],
		[pattern: '/api/login', 	 access: ['permitAll']],
		[pattern: '/api/logout',     access: ['isFullyAuthenticated()']],
		[pattern: '/**',             access: ['ROLE_USER', 'ROLE_ADMIN']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
	[pattern: '/api/guest/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
	[pattern: '/api/**', filters:'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter'],
	[pattern: '/**', filters: 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter']
]
