package com.councilsearch

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.SSLContextBuilder
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.beans.factory.InitializingBean

import javax.net.ssl.SSLContext
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class RequestService implements InitializingBean{
	Integer REQUEST_TIME_OUT // Milli
	String DEFAULT_SSL_VERSION
	Integer MAX_REDIRECT

	public void afterPropertiesSet() throws Exception {
		REQUEST_TIME_OUT = CustomConfig.findByName("REQUEST_TIME_OUT")?.getValue() as Integer ?: 300000
		DEFAULT_SSL_VERSION = CustomConfig.findByName("DEFAULT_SSL_VERSION")?.getValue() ?: "TLSv1.2"
		MAX_REDIRECT = CustomConfig.findByName("MAX_REDIRECT")?.getValue() as Integer ?: 5
	}

	// https://avaldes.com/apache-http-client-example/
	// Get
	HttpResponse executeGet(String URLAddress, Map<String, String> headerParams, Integer redirectCount) throws MalformedURLException {
		log.info "Requesting: ${URLAddress} with redirectCount: ${redirectCount}"
		HttpResponse response

		if(redirectCount <= MAX_REDIRECT){
			URI uri = buildURI(URLAddress)

			if(uri == null){
				log.error("Could not execute request for: ${URLAddress} - URI is null")
				return
			}

			// Build the Request object
			RequestConfig.Builder rb = RequestConfig.custom()
			// Set the time outs
			rb.setConnectTimeout(REQUEST_TIME_OUT)
			rb.setConnectionRequestTimeout(REQUEST_TIME_OUT)
			rb.setSocketTimeout(REQUEST_TIME_OUT)

			HttpClientBuilder hcb = HttpClientBuilder.create()
			// Set the Request config
			hcb.setDefaultRequestConfig(rb.build())
			// Disable auto redirect
			hcb = hcb.disableRedirectHandling()
			// No retries
			hcb = hcb.disableAutomaticRetries()

			// Build the Client
			HttpClient client = hcb.build()

			HttpGet httpGet = new HttpGet(uri)

			if (headerParams != null) {
				for (String header : headerParams.keySet()) {
					httpGet.addHeader(header, headerParams.get(header))
				}
			}

			try {
				response = client.execute(httpGet)

				// Handle redirects because they give us shit URLs sometimes
				if(response?.getStatusLine()?.getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT ||
						response?.getStatusLine()?.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY ||
						response?.getStatusLine()?.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY){
					def redirectAddress = response.getFirstHeader("Location").getValue()

					if(redirectAddress.toUpperCase().startsWith("HTTPS")){
						log.info("Redirecting to Absolute URL: ${redirectAddress}")
						response = executeSSLGet(redirectAddress, headerParams, null, redirectCount+1)
					}else if(redirectAddress.toUpperCase().startsWith("HTTP")){
						log.info("Redirecting to Absolute URL: ${redirectAddress}")
						response = executeGet(redirectAddress, headerParams, redirectCount+1)
					}else if(redirectAddress.toUpperCase().startsWith("/")){
						log.info("Redirecting to Relative URL: ${redirectAddress}")
						response = executeGet(buildAbsoluteURL(URLAddress, redirectAddress), headerParams, redirectCount+1)
					}else{
						log.error "Invalid redirect URL: "+redirectAddress
					}
				}
			} catch (Exception e) {
				log.error("Could not request URL: ${URLAddress}\t"+e)
			}
		}else{
			log.warn("Hit maximum number of redirects: "+MAX_REDIRECT)
		}

		return response
	}

	// Get SSL
	HttpResponse executeSSLGet(String URLAddress, Map<String, String> headerParams, String sslVersion, Integer redirectCount){
		log.info "Requesting: ${URLAddress} with redirectCount: ${redirectCount}"
		HttpResponse response

		if(redirectCount <= MAX_REDIRECT){
			URI uri = buildURI(URLAddress)

			if(uri == null){
				log.error("Could not execute request for: ${URLAddress} - URI is null")
				return
			}

			sslVersion = sslVersion ?: DEFAULT_SSL_VERSION

			// Build the Request object
			RequestConfig.Builder rb = RequestConfig.custom()
			// Set the time outs
			rb.setConnectTimeout(REQUEST_TIME_OUT)
			rb.setConnectionRequestTimeout(REQUEST_TIME_OUT)
			rb.setSocketTimeout(REQUEST_TIME_OUT)

			HttpClientBuilder hcb = HttpClientBuilder.create()
			// Set the Request config
			hcb.setDefaultRequestConfig(rb.build())
			// Disable auto redirect
			hcb = hcb.disableRedirectHandling()
			// No retries
			hcb = hcb.disableAutomaticRetries()

			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException { return true }
			}).build()

			String[] SSLvs = [sslVersion]
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLvs, null, new NoopHostnameVerifier())
			hcb = hcb.setSSLSocketFactory(sslsf)

			// Build the Client
			HttpClient client = hcb.build()

			HttpGet httpGet = new HttpGet(uri)

			if (headerParams != null) {
				for (String header : headerParams.keySet()) {
					httpGet.addHeader(header, headerParams.get(header))
				}
			}

			try {
				response = client.execute(httpGet)

				// Handle redirects because they give us shit URLs sometimes
				if(response?.getStatusLine()?.getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT ||
						response?.getStatusLine()?.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY ||
						response?.getStatusLine()?.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY){
					def redirectAddress = response.getFirstHeader("Location").getValue()

					if(redirectAddress.toUpperCase().startsWith("HTTPS")){
						log.info("Redirecting to Absolute URL: ${redirectAddress}")
						response = executeSSLGet(redirectAddress, headerParams, sslVersion, redirectCount+1)
					}else if(redirectAddress.toUpperCase().startsWith("HTTP")){
						log.info("Redirecting to Absolute URL: ${redirectAddress}")
						response = executeGet(redirectAddress, headerParams, redirectCount+1)
					}else if(redirectAddress.toUpperCase().startsWith("/")){
						log.info("Redirecting to Relative URL: ${redirectAddress}")
						response = executeGet(buildAbsoluteURL(URLAddress, redirectAddress), headerParams, redirectCount+1)
					}else{
						log.error "Invalid redirect URL: "+redirectAddress
					}
				}
			} catch (Exception e) {
				log.error("Could not request URL: ${URLAddress}\t"+e)
			}
		}else{
			log.warn("Hit maximum number of redirects: "+MAX_REDIRECT)
		}

		return response
	}

	// Post

	// Post SSL

	def buildURI(String urlStr){
		URI uri

		try{
			urlStr = URLDecoder.decode(urlStr, "UTF-8")


			// If google viewer
			if(urlStr.startsWith("https://docs.google.com/gview")){
				// ex: https://docs.google.com/gview?url=https%3A%2F%2Fjacksonvillenc.granicus.com%2FDocumentViewer.php%3Ffile%3Djacksonvillenc_fdeaeab77fe184cb80db72c4dff09344.pdf%26view%3D1&embedded=true
				urlStr = urlStr.replace("https://docs.google.com/gview?url=","")
				urlStr = urlStr.replace("&view=1&embedded=true","")
			}

			URL url = new URL(urlStr)

			// URL Encoder class is garabage just do a couple common ones
			if(url?.path != null){
				url.path = url.path?.replaceAll("\\s", "%20")// Encode white space
									.replaceAll("\\{", "")// Remove illegal bracket
									.replaceAll("}", "")
									.replaceAll("\\[", "%5B") // Encode normal brackets
									.replaceAll("]", "%5D")
									.replaceAll("\\\\", "/") // Replace \ with /
									.replaceAll("//","/") // Remove double slash
			}

			uri = url.toURI()
		}catch(Exception e){
			log.error("Could not build URI from URL String: ${urlStr} - "+e)
		}

		return  uri
	}

	def buildAbsoluteURL(String orginalURL, String relativeURL){
		URI originalURI = new URI(orginalURL)
		URI relativeURI = originalURI.resolve(relativeURL?.replaceAll("\\s", "%20"))

		return relativeURI.toString()
	}
}
