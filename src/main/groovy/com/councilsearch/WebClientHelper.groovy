package com.councilsearch

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler
import com.gargoylesoftware.htmlunit.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebClientHelper {

	private static final Logger log = LoggerFactory.getLogger(WebClientHelper.class)

	// Set up the client
	static WebClient buildClient(){
		log.info("Building HTMLUnit WebClient")
		WebClient client = new WebClient(BrowserVersion.FIREFOX)

		// TODO - Make configurable based on site
		// Set general settings
		client.getOptions().setTimeout(10000)
		client.getOptions().setUseInsecureSSL(true)
		client.getOptions().setThrowExceptionOnFailingStatusCode(false)
		client.getOptions().setRedirectEnabled(true)
		client.getOptions().setAppletEnabled(false)
		client.getOptions().setPopupBlockerEnabled(true)
		// Set Javascript settings
		client.getOptions().setJavaScriptEnabled(true)
		client.setJavaScriptTimeout(10000)
		client.setAjaxController(new NicelyResynchronizingAjaxController())
		client.getOptions().setThrowExceptionOnScriptError(false)
		client.waitForBackgroundJavaScript(60000)
		// Set CSS
		client.getOptions().setCssEnabled(true)
		client.setCssErrorHandler(new SilentCssErrorHandler())

		return client
	}
}
