import ch.qos.logback.classic.Level
import ch.qos.logback.core.util.FileSize

// Set the home directory
CS_HOME = System.getenv('CS_HOME')

if(CS_HOME == null){
	CS_HOME = "/data/council-search"
	System.out.println("CS_HOME environment variable is empty using default: ${CS_HOME}")
}

// Create log file and levels
def LOG_DIR = CS_HOME+System.getProperty("file.separator")+"logs"
def LOG_NAME = "council-search.log"
def PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
def ROOT_LOG_LEVEL
def APP_LOG_LEVEL
def ROOT_LOG_LEVEL_STR = System.getenv('ROOT_LOG_LEVEL')
def APP_LOG_LEVEL_STR = System.getenv('APP_LOG_LEVEL')

// Config root log level from env vars
if("ERROR".equalsIgnoreCase(ROOT_LOG_LEVEL_STR)){
	ROOT_LOG_LEVEL = ERROR
}else if("WARN".equalsIgnoreCase(ROOT_LOG_LEVEL_STR)){
	ROOT_LOG_LEVEL = WARN
}else if("DEBUG".equalsIgnoreCase(ROOT_LOG_LEVEL_STR)){
	ROOT_LOG_LEVEL = DEBUG
}else{
	ROOT_LOG_LEVEL = INFO
}

// Config root log level from env vars
if("ERROR".equalsIgnoreCase(APP_LOG_LEVEL_STR)){
	APP_LOG_LEVEL = ERROR
}else if("WARN".equalsIgnoreCase(APP_LOG_LEVEL_STR)){
	APP_LOG_LEVEL = WARN
}else if("DEBUG".equalsIgnoreCase(APP_LOG_LEVEL_STR)){
	APP_LOG_LEVEL = DEBUG
}else{
	APP_LOG_LEVEL = INFO
}

System.out.println("Using log directory: ${LOG_DIR}")

// APPENDERS - Define appenders available
appender('STDOUT', ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = PATTERN
	}
}
appender("ROLLING", RollingFileAppender) {
	file = LOG_DIR+System.getProperty("file.separator")+LOG_NAME
	append = true
	encoder(PatternLayoutEncoder) {
		pattern = PATTERN
	}
	rollingPolicy(TimeBasedRollingPolicy) {
		// TODO - Produces a base file with a date which the logging services need to be reconfigured for
		fileNamePattern = LOG_DIR+System.getProperty("file.separator")+LOG_NAME+".%d{yyyy-MM-dd}"
		maxHistory = 30
		totalSizeCap = FileSize.valueOf("2GB")
	}
}

logger('org.hibernate', OFF, ['ROLLING'], false)
logger('com.gargoylesoftware.htmlunit', OFF, ['ROLLING'], false)
logger('o.a.p.pdmodel.font.PDTrueTypeFont', OFF, ['ROLLING'], false)
logger('org.apache.fontbox.ttf.CmapSubtable', OFF, ['ROLLING'], false)
logger('o.a.pdfbox.pdmodel.font.PDSimpleFont', OFF, ['ROLLING'], false)

logger('com.councilsearch', APP_LOG_LEVEL, ['ROLLING'], false)
root(ROOT_LOG_LEVEL, ['ROLLING', 'STDOUT'])