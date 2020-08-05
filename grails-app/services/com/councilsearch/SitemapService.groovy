package com.councilsearch

import com.redfin.sitemapgenerator.ChangeFreq
import com.redfin.sitemapgenerator.SitemapIndexGenerator
import com.redfin.sitemapgenerator.WebSitemapGenerator
import com.redfin.sitemapgenerator.WebSitemapUrl
import groovy.sql.Sql
import org.springframework.beans.factory.InitializingBean
import org.apache.commons.io.FileUtils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
		File sitemapDir = new File(SITEMAP_DIR)

		if(sitemapDir == null || !sitemapDir.exists()){
			log.error("Sitemap directory:${SITEMAP_DIR} does not exist")
			return false
		}

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

			try{
				log.debug("Processing entry: "+sMap)
				def state = sMap[0]
				def region = sMap[1]
				String dateCreatedStr = sMap[2] // being lazy
				String meetingDateStr = sMap[3]
				def docType = sMap[4]
				def uuid = sMap[5]

				// Create the dates
				LocalDate dateCreated = LocalDate.parse(dateCreatedStr, MYSQL_DATE_FORMAT)
				LocalDate meetingDate = LocalDate.parse(meetingDateStr, MYSQL_DATE_FORMAT)


				def url = createURL(state, region, "council", docType, meetingDate, uuid)

				// Create the entry
				WebSitemapUrl wsu = new WebSitemapUrl.Options(url)
						.lastMod(dateCreated.format(SITEMAP_DATE_FORMAT))
						.priority(0.9) // default
						.changeFreq(ChangeFreq.NEVER)
						.build()
				wsg.addUrl(wsu)
				urlCnt++
			}catch(Exception e){
				log.error("Could not create sitemap entry for Monitor:${monitorId} "+e)
			}
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

	// Creates a special SEO friendly unblocked URL
	def createURL(def state, def region, def meetingType, def docType, LocalDate meetingDate, def uuid){
		// state/region/meetingType/docType/year/month/day#uuid
		String url = "${SITEMAP_BASE_URL}/${docType?.toLowerCase()}/${state?.replaceAll("\\s","-")?.toLowerCase()}/"+
				"${region.replaceAll("\\s","-")?.toLowerCase()}/${meetingType?.toLowerCase()}/"+
				"${meetingDate.getYear()}/${meetingDate.month}/${meetingDate.getDayOfMonth()}#${uuid}"
		return url
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
