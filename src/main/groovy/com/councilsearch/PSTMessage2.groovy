package com.councilsearch

class PSTMessage2 {
	//def attachment
	def body
	def bodyHTML
//	def bodyPrefix // Prob not useful
	Date clientSubmitTime
	def conversationId  // Byte Stream?
	def conversationIndex
	def conversationTopic
	def displayBCC
	def displayCC
	def displayTo
	def inReplyToId
	Integer internetArticleNumber
	def internetMessageId
	Boolean messageCcMe
	def messageClass
	Date messageDeliveryTime
	Boolean messageRecipMe
	Long messageSize
	Boolean messageToMe
	Integer nativeBodyType
	def nextSendAcct
	Integer numberOfAttachments
	Integer numberOfRecipients
	Integer objectType
	def originalDisplayBcc
	def originalDisplayCc
	def originalDisplayTo
	Integer originalSensitivity
	def originalSubject
	def primarySendAccount
	Integer priority
	def receivedByAddress
	def receivedByAddressType
	def receivedByName
	//def recipient
	def recipientsString
	Integer recipientType
	Boolean reminderSet
	def replyRecipientNames
	Boolean responseRequested
	Boolean responsibility
	def returnPath
	def rTFBody
	def senderAddrtype
	def senderEmailAddress
	def senderEntryId // Byte stream?
	def senderName
	Integer sensitivity
	def sentRepresentingAddressType
	def sentRepresentingAddrtype
	def sentRepresentingEmailAddress
	def sentRepresentingName
	def subject
	Date taskDueDate
	Date taskStartDate
	def transportMessageHeaders
	def uRLCompName
	Integer uRLCompNamePostfix

	String toString(){
		String theString = "\""+
//				this.body?.replaceAll("\"","")?.replaceAll("\n","")+"\",\""+
//				this.bodyHTML?.replaceAll("\"","")?.replaceAll("\n","")+"\",\""+
				this.bodyPrefix?.replaceAll("\"","")+"\",\""+
				this.clientSubmitTime+"\",\""+
				this.conversationId+"\",\""+
				this.conversationIndex+"\",\""+
				this.conversationTopic?.replaceAll("\"","")+"\",\""+
				this.displayBCC?.replaceAll("\"","")+"\",\""+
				this.displayCC?.replaceAll("\"","")+"\",\""+
				this.displayTo?.replaceAll("\"","")+"\",\""+
				this.inReplyToId?.replaceAll("\"","")+"\",\""+
				this.internetArticleNumber+"\",\""+
				this.internetMessageId?.replaceAll("\"","")+"\",\""+
				this.messageCcMe+"\",\""+
				this.messageClass?.replaceAll("\"","")+"\",\""+
				this.messageDeliveryTime+"\",\""+
				this.messageRecipMe+"\",\""+
				this.messageSize+"\",\""+
				this.messageToMe+"\",\""+
				this.nativeBodyType+"\",\""+
				this.nextSendAcct?.replaceAll("\"","")+"\",\""+
				this.numberOfAttachments+"\",\""+
				this.numberOfRecipients+"\",\""+
				this.objectType+"\",\""+
				this.originalDisplayBcc?.replaceAll("\"","")+"\",\""+
				this.originalDisplayCc?.replaceAll("\"","")+"\",\""+
				this.originalDisplayTo?.replaceAll("\"","")+"\",\""+
				this.originalSensitivity+"\",\""+
				this.originalSubject?.replaceAll("\"","")+"\",\""+
				this.primarySendAccount?.replaceAll("\"","")+"\",\""+
				this.priority+"\",\""+
				this.receivedByAddress?.replaceAll("\"","")+"\",\""+
				this.receivedByAddressType?.replaceAll("\"","")+"\",\""+
				this.receivedByName?.replaceAll("\"","")+"\",\""+
				this.recipientsString?.replaceAll("\"","")+"\",\""+
				this.recipientType+"\",\""+
				this.reminderSet+"\",\""+
				this.replyRecipientNames?.replaceAll("\"","")+"\",\""+
				this.responseRequested+"\",\""+
				this.responsibility+"\",\""+
				this.returnPath?.replaceAll("\"","")+"\",\""+
				this.rTFBody?.replaceAll("\"","")+"\",\""+
				this.senderAddrtype?.replaceAll("\"","")+"\",\""+
				this.senderEmailAddress?.replaceAll("\"","")+"\",\""+
				this.senderEntryId+"\",\""+
				this.senderName?.replaceAll("\"","")+"\",\""+
				this.sensitivity+"\",\""+
				this.sentRepresentingAddressType?.replaceAll("\"","")+"\",\""+
				this.sentRepresentingAddrtype?.replaceAll("\"","")+"\",\""+
				this.sentRepresentingEmailAddress?.replaceAll("\"","")+"\",\""+
				this.sentRepresentingName?.replaceAll("\"","")+"\",\""+
				this.subject?.replaceAll("\"","")+"\",\""+
				this.taskDueDate+"\",\""+
				this.taskStartDate+"\",\""+
				this.transportMessageHeaders?.replaceAll("\"","")+"\",\""+
				this.uRLCompName?.replaceAll("\"","")+"\",\""+
				this.uRLCompNamePostfix+"\""

		return theString
	}
}
