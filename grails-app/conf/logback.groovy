String theLog = System.getenv('CS_LOG') ?: "/data/tomcat/logs/council-search-data.log"
println "Using log: ${theLog}"

appender("File-Appender", FileAppender) {
	file = theLog
	encoder(PatternLayoutEncoder) {
		pattern = "%d [%thread] %-5level %logger{36} - %msg%n"
		outputPatternAsHeader = true
	}
}

root(ERROR, ["File-Appender"])
logger("com.councilsearch", INFO, ["File-Appender"], false)
logger('grails.plugins', TRACE)
