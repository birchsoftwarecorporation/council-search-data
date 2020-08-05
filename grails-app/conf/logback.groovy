String theLog = System.getenv('CS_LOG') ?: "/data/tomcat/logs/council-search-data.log"
println "Using dev log: ${theLog}"

appender("File-Appender", FileAppender) {
	file = theLog
	encoder(PatternLayoutEncoder) {
		pattern = "%d [%thread] %-5level %logger{36} - %msg%n"
		outputPatternAsHeader = true
	}
}

root(DEBUG, ["File-Appender"])
logger("com.councilsearch", DEBUG, ["File-Appender"], false)
