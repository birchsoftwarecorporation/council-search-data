package com.councilsearch

import com.redfin.sitemapgenerator.ChangeFreq
import com.redfin.sitemapgenerator.SitemapIndexGenerator
import com.redfin.sitemapgenerator.WebSitemapGenerator
import com.redfin.sitemapgenerator.WebSitemapUrl
import groovy.sql.Sql
import org.springframework.beans.factory.InitializingBean
import org.apache.commons.io.FileUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class SitemapService implements InitializingBean {
	def queryService
	def dataSource

	String SITEMAP_DIR
	String SITEMAP_BASE_URL
	DateTimeFormatter MYSQL_DATE_FORMAT
	DateTimeFormatter SITEMAP_DATE_FORMAT

	public void afterPropertiesSet() throws Exception {
		SITEMAP_DIR = CustomConfig.findByName("SITEMAP_DIR")?.getValue() ?: "/data/sitemaps"
		SITEMAP_BASE_URL = CustomConfig.findByName("SITEMAP_BASE_URL")?.getValue() ?: "http://www.councilsearch.com"
		MYSQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
		SITEMAP_DATE_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd")
	}

	boolean create(){
		Sql sql = new Sql(dataSource)
		boolean success = true
		List<String> siteMapFileNames = []
		List<String> staticSites = ["", "search", "contact"]
		File sitemapDir = new File(SITEMAP_DIR)

		if(sitemapDir == null || !sitemapDir.exists()){
			log.error("Sitemap directory:${SITEMAP_DIR} does not exist")
			return false
		}

		log.info("Finding sitemap documents newer than: 2020-01-01")

		// Generate the master file (default pages home/contact/search
		siteMapFileNames.add(generateStaticSiteMap(sitemapDir, staticSites))

		// Grab all monitor data and the latest document modified date
		List monitorData = queryService.distinctMonitorIds(sql)
		Iterator mDataItr = monitorData.iterator()

		while(mDataItr.hasNext()){
			def mMap = mDataItr.next()

			// Need to column
			if(mMap.size() != 1){
				log.warn("Incorrect number of mMap columns")
				continue
			}

			def monitorId = mMap[0]

			// Grab this monitors document data
			List sMapInfo = queryService.getSitemapInfo(sql, monitorId)

			// Generate the Sitemap file
			String siteMapFileName = generateSiteMap(monitorId, sitemapDir, sMapInfo)

			if(siteMapFileName != null && !"".equals(siteMapFileName)){
				siteMapFileNames.add(siteMapFileName)
			}
		}

		generateSitemapIndex(siteMapFileNames)

		return success
	}

	def generateStaticSiteMap(File siteMapDir, List staticPages){
		log.info("Creating static sitemap")
		LocalDate today = LocalDate.now()
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
		String todayStr = formatter.format(today)
		String fileNamePrefix = "sitemap-static"
		String sitemapFileName = fileNamePrefix+".xml.gz"

		WebSitemapGenerator wsg = WebSitemapGenerator.builder(SITEMAP_BASE_URL, siteMapDir)
				.fileNamePrefix(fileNamePrefix)
				.gzip(true)
				.build()

		// Build the static page references
		for(def staticPageName : staticPages){
			log.info("Processing entry: "+staticPageName)
			String url = SITEMAP_BASE_URL

			try{
				// Doing this to include the base url
				if(!"".equalsIgnoreCase(staticPageName)){
					url = url+"/${staticPageName}"
				}

				// Create the entry
				WebSitemapUrl wsu = new WebSitemapUrl.Options(url)
						.lastMod(todayStr)
						.priority(0.8) // default
						.changeFreq(ChangeFreq.DAILY)
						.build()
				wsg.addUrl(wsu)
			}catch(Exception e){
				log.error("Could not create static sitemap entry "+e)
			}
		}

		// Write the static file
		try{
			wsg.write()
		}catch(Exception e){
			sitemapFileName = null // Failed to create file
			log.error("Could not create static Sitemap file "+e)
		}

		return sitemapFileName
	}

	def generateSiteMap(def monitorId, File siteMapDir, def sMapInfo){
		int urlCnt = 0
		String fileNamePrefix = "sitemap-${monitorId}"
		String sitemapFileName = fileNamePrefix+".xml.gz"
		log.info("Creating for Monitor:${monitorId} with ${sMapInfo.size()} items")

		// Assumption - never going to have 50,000 documents to one monitor
		WebSitemapGenerator wsg = WebSitemapGenerator.builder(SITEMAP_BASE_URL, siteMapDir)
				.fileNamePrefix(fileNamePrefix)
				.gzip(true)
				.build()

		Iterator sMapItr = sMapInfo.iterator()

		while(sMapItr.hasNext()) {
			def sMap = sMapItr.next()

			// Need exact column size
			if (sMap.size() != 6) {
				log.warn("Incorrect number of sMap columns")
				continue
			}

			def state = sMap[0]
			def region = sMap[1]
			String dateCreatedStr = sMap[2]
			String meetingDateStr = sMap[3]
			def docType = sMap[4]
			def uuid = sMap[5]

			// Create the dates
			LocalDate dateCreated = LocalDate.parse(dateCreatedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

			String url = "${SITEMAP_BASE_URL}/document/${uuid}"

			// Create the entry
			WebSitemapUrl wsu = new WebSitemapUrl.Options(url)
					.lastMod(dateCreated.format(SITEMAP_DATE_FORMAT))
					.priority(0.6) // default
					.changeFreq(ChangeFreq.MONTHLY)
					.build()
			wsg.addUrl(wsu)
			urlCnt++
		}

		// Occasionally no urls are added and an error is thrown by the Sitemap lib
		if(urlCnt > 0){
			try{
				wsg.write()
			}catch(Exception e){
				sitemapFileName = null // Failed to create file
				log.error("Could not create Sitemap file for Monitor:${monitorId} "+e)
			}
		}else{
			sitemapFileName = null // Failed to create file
			log.warn("Could not create Sitemap file with no urls for Monitor:${monitorId}")
		}

		return sitemapFileName
	}

	def generateSitemapIndex(List siteMapFileNames){
		File sitemapIndexFile = new File(SITEMAP_DIR+File.separator+"sitemap-index.xml")
		SitemapIndexGenerator sig = new SitemapIndexGenerator(SITEMAP_BASE_URL, sitemapIndexFile)

		siteMapFileNames.each { siteMapFileName ->
			sig.addUrl(SITEMAP_BASE_URL+"/${siteMapFileName}")
		}

		sig.write()
	}

	File getSitemap(String fileName){
		File file
		String filePath = SITEMAP_DIR+File.separator+fileName
		log.info("Retrieving Sitemap file: "+filePath)

		try{
			file = new File(filePath)
		}catch(Exception e){
			log.error("Could not retrieve Sitemap file: "+filePath+" "+e)
		}

		return file
	}

	boolean clear(){
		boolean isCleared = true

		try{
			File dir = new File(SITEMAP_DIR)
			FileUtils.cleanDirectory(dir)
		}catch(Exception e){
			log.error("Could not clear Sitemap directory: "+SITEMAP_DIR+" "+e)
			isCleared = false
		}

		return isCleared
	}

}
