package com.councilsearch

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import grails.gorm.transactions.Transactional
import grails.gsp.PageRenderer
import org.springframework.beans.factory.InitializingBean

@Transactional
class AmazonWebService implements InitializingBean {

	// AWS S3
	String S3_BUCKET
	String S3_DOCUMENTS_FOLDER
	String S3_ACCESS_KEY
	String S3_SECRET_KEY
	AmazonS3 S3_CLIENT

	// AWS SES
	AmazonSimpleEmailService SES_CLIENT

	public void afterPropertiesSet() throws Exception {
		// AWS S3
		S3_BUCKET = CustomConfig.findByName("S3_BUCKET")?.getValue()
		S3_DOCUMENTS_FOLDER = CustomConfig.findByName("S3_DOCUMENTS_FOLDER")?.getValue()
		S3_ACCESS_KEY = CustomConfig.findByName("S3_ACCESS_KEY")?.getValue()
		S3_SECRET_KEY = CustomConfig.findByName("S3_SECRET_KEY")?.getValue()
		AWSCredentials credentials = new BasicAWSCredentials(S3_ACCESS_KEY, S3_SECRET_KEY)
		S3_CLIENT = AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(credentials))
						.withRegion(Regions.US_EAST_2)
						.build()

		// AWS SES
		try{
			// TODO - Move AWS Keys from ENV vars to DB...if possible
			SES_CLIENT = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_EAST_1).build()
		}catch(Exception e){
			log.fatal("Could not create AWS SES Client...please restart application")
		}
	}

	def uploadDocuments(List docPayloads){
		Iterator<Map> dItr = docPayloads.iterator()

		while(dItr.hasNext()) {
			Map docPayload = dItr.next()

			// Dont upload to AWS if the processing didnt end with a success
			if(!docPayload.get("success")){
				continue
			}

			try{
				String filePath = docPayload.get("filePath")

				// Getting random File(null) error
				if(filePath == null || "".equals(filePath?.trim())){
					log.error("Cannot upload File with empty filePath")
					continue
				}

				File file = new File(filePath)
				def uuid = docPayload.get("uuid")

				PutObjectResult result = s3Upload(uuid, file)

				if(result == null){
					log.error("AWS S3 upload result is null for Document:${uuid}")
				}
			}catch(Exception e) {
				log.error("Could not upload document: ${docPayload.get("uuid")} to AWS S3" + e)
			}
		}
	}

	PutObjectResult s3Upload(def uuid, File file) throws Exception{
		PutObjectResult result

		// If the file exists
		if (file != null && file?.exists() && uuid != null) {
			result = S3_CLIENT.putObject(S3_BUCKET, S3_DOCUMENTS_FOLDER+"/"+uuid, file)
		}else{
			log.error("File or uuid do not exist")
		}

		return result
	}

	def s3Download(def id) throws Exception{
		return S3_CLIENT.getObject(S3_BUCKET, S3_DOCUMENTS_FOLDER+"/"+id).getObjectContent()
	}

	void s3Delete(def id) throws Exception{
		S3_CLIENT.deleteObject(new DeleteObjectRequest(S3_BUCKET, S3_DOCUMENTS_FOLDER+"/"+id))
	}

	Boolean s3ObjectExists(def uuid) throws Exception{
		return S3_CLIENT.doesObjectExist(S3_BUCKET, S3_DOCUMENTS_FOLDER+"/"+uuid)
	}

	boolean email(def toAddress, def bccAddress, def fromAddress, def subject, def content) throws Exception{
		try {
			// Build the 'to'
			Destination destination = new Destination().withToAddresses(toAddress)

			// BCC if we have it
			if(bccAddress != null){
				destination = destination.withBccAddresses(bccAddress)
			}

			// Build the Message
			com.amazonaws.services.simpleemail.model.Message message = new com.amazonaws.services.simpleemail.model.Message()
			// Add the subject
			message.withSubject(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(subject))
			// Add the content
			message.withBody(new Body().withHtml(new com.amazonaws.services.simpleemail.model.Content()
					.withCharset("UTF-8").withData(content)))

			// Put it all together
			SendEmailRequest request = new SendEmailRequest()
					.withSource(fromAddress)
					.withDestination(destination)
					.withMessage(message)

			SES_CLIENT.sendEmail(request)
		} catch (Exception ex) {
			throw ex
		}
	}
}
