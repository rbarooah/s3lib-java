/* 
 * S3ServerException vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2005 - 2007 Three Rings Design, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright owner nor the names of contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

// Most of this class is autogenerated from the included awk scripts, and thusly,
// fantastically ugly.

package com.threerings.s3.client;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.Constructor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** 
 * An exception that indicates a generic S3 error.
 */
public class S3ServerException extends S3Exception
{
    // Documentation inherited
    public S3ServerException () {
        this((String) null);
    }
    
    // Documentation inherited
    public S3ServerException (String message) {
        this(message, null, null);
    }

    /**
     * Initialize an AWS exception with the associated message, AWS S3 requestId,
     * and AWS S3 hostId.
     * @param message Error message provided by S3.
     * @param requestId Request ID provided by S3.
     * @param hostId Host ID provided by S3;
     */
    public S3ServerException (String message, String requestId, String hostId) {
        super(message);
        _requestId = requestId;
        _hostId = hostId;
    }

    /**
     * Extract the child node's text.
     * @param node Paren node.
     */
    private static String _extractXmlChildText(Node node)
    {
        Node textNode = node.getFirstChild();
        if (textNode == null)
            return null;
        return textNode.getNodeValue();
    }

    /**
     * Convert an S3 XML error document into a S3ServerException instance.
     * @param documentString A string containing the XML error document.
     */
    public static S3ServerException exceptionForS3Error (String documentString) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc;

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(documentString)));
        } catch (ParserConfigurationException e) {
            // This should not happen. Return a generic S3 exception
            return new S3ServerException("Error (" + e + ") parsing S3 error " +
                "document: '" + documentString + "'");
        } catch (SAXException e) {
            // Return a generic exception
            return new S3ServerException("Error (" + e + ") parsing S3 error " +
                "document: '" + documentString + "'");
        } catch (IOException e) {
            // This is not really possible
            return new S3ServerException("I/O error (" + e + ") parsing S3 error " +
                "document: '" + documentString + "'");
        }
        
        // Extract the error data. We ignore elements that we don't understand,
        // and the document structure API should be stable.
        Node node;
        String code = null;
        String errorMessage = null;
        String requestId = null;
        String hostId = null;

        for (node = doc.getDocumentElement().getFirstChild();
            node != null;
            node = node.getNextSibling()) {
            
            if (node.getNodeName().toLowerCase().equals("code")) {
                code = _extractXmlChildText(node);
                continue;
            }
            
            if (node.getNodeName().toLowerCase().equals("message")) {
                errorMessage = _extractXmlChildText(node);
                continue;
            }
            
            if (node.getNodeName().toLowerCase().equals("requestid")) {
                requestId = _extractXmlChildText(node);
                continue;
            }
            
            if (node.getNodeName().toLowerCase().equals("hostid")) {
                hostId = _extractXmlChildText(node);
                continue;
            }
        }
        
        // Try to instantiate an exception
        // This could use a static mapping, but exceptions are rare
        // conditions and there's enough ugly generated code in here as it is.
        try {
            Constructor<? extends S3ServerException> construct;
            Class<? extends S3ServerException> cls;
        	final Class<?> loadedClass;

            // This Class.forName('$') usage for static subclasses is broken, but necessary. See:
            //     http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4378381
            loadedClass = Class.forName("com.threerings.s3.client.S3ServerException$" + code + "Exception");
            cls = loadedClass.asSubclass(S3ServerException.class);

            // Grab the constructor
            construct = cls.getConstructor(new Class[] {String.class, String.class, String.class});

            return construct.newInstance(new Object[] {errorMessage, requestId, hostId});
        } catch (Exception e) {
            return new S3ServerException("An unhandled S3 error code was returned: " + code, requestId, hostId);
        }                
    }

    /** Get the Amazon S3 request ID. */
    public String getRequestId ()
    {
        return _requestId;
    }

    /** Get the Amazon S3 host ID. */
    public String getHostId ()
    {
        return _hostId;
    }

    /** Amazon S3 Request ID */
    private String _requestId;
    
    /** Amazon S3 Host ID */
    private String _hostId;


    // The following was autogenerated from the list of exceptions, copied from
    // http://docs.amazonwebservices.com/AmazonS3/2006-03-01/ErrorCodeList.html
    // using the included genexceptions.awk:
    //     awk -F '\t' -f genexceptions.awk <allerrors.txt
    
    /** Access Denied   */
    public static class AccessDeniedException extends S3ServerException {
        public AccessDeniedException (String message) {
            this(message, null, null);
        }

        public AccessDeniedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** There is a problem with your AWS account that prevents the operation from completing successfully. Please contact customer service at webservices@amazon.com.  */
    public static class AccountProblemException extends S3ServerException {
        public AccountProblemException (String message) {
            this(message, null, null);
        }

        public AccountProblemException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** All access to this object has been disabled.  */
    public static class AllAccessDisabledException extends S3ServerException {
        public AllAccessDisabledException (String message) {
            this(message, null, null);
        }

        public AllAccessDisabledException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The e-mail address you provided is associated with more than one account.  */
    public static class AmbiguousGrantByEmailAddressException extends S3ServerException {
        public AmbiguousGrantByEmailAddressException (String message) {
            this(message, null, null);
        }

        public AmbiguousGrantByEmailAddressException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** A conflicting conditional operation is currently in progress against this resource. Please try again.  */
    public static class OperationAbortedException extends S3ServerException {
        public OperationAbortedException (String message) {
            this(message, null, null);
        }

        public OperationAbortedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The Content-MD5 you specified did not match what we received.  */
    public static class BadDigestException extends S3ServerException {
        public BadDigestException (String message) {
            this(message, null, null);
        }

        public BadDigestException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The named bucket you tried to create already exists.  */
    public static class BucketAlreadyExistsException extends S3ServerException {
        public BucketAlreadyExistsException (String message) {
            this(message, null, null);
        }

        public BucketAlreadyExistsException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The bucket you tried to delete is not empty.  */
    public static class BucketNotEmptyException extends S3ServerException {
        public BucketNotEmptyException (String message) {
            this(message, null, null);
        }

        public BucketNotEmptyException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** This request does not support credentials.  */
    public static class CredentialsNotSupportedException extends S3ServerException {
        public CredentialsNotSupportedException (String message) {
            this(message, null, null);
        }

        public CredentialsNotSupportedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your proposed upload exceeds the maximum allowed object size.  */
    public static class EntityTooLargeException extends S3ServerException {
        public EntityTooLargeException (String message) {
            this(message, null, null);
        }

        public EntityTooLargeException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** You did not provide the number of bytes specified by the Content-Length HTTP header  */
    public static class IncompleteBodyException extends S3ServerException {
        public IncompleteBodyException (String message) {
            this(message, null, null);
        }

        public IncompleteBodyException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** We encountered an internal error. Please try again.  */
    public static class InternalErrorException extends S3ServerException {
        public InternalErrorException (String message) {
            this(message, null, null);
        }

        public InternalErrorException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The AWS Access Key Id you provided does not exist in our records.  */
    public static class InvalidAccessKeyIdException extends S3ServerException {
        public InvalidAccessKeyIdException (String message) {
            this(message, null, null);
        }

        public InvalidAccessKeyIdException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** You must specify the Anonymous role.  */
    public static class InvalidAddressingHeaderException extends S3ServerException {
        public InvalidAddressingHeaderException (String message) {
            this(message, null, null);
        }

        public InvalidAddressingHeaderException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Invalid Argument  */
    public static class InvalidArgumentException extends S3ServerException {
        public InvalidArgumentException (String message) {
            this(message, null, null);
        }

        public InvalidArgumentException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The specified bucket is not valid.  */
    public static class InvalidBucketNameException extends S3ServerException {
        public InvalidBucketNameException (String message) {
            this(message, null, null);
        }

        public InvalidBucketNameException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The Content-MD5 you specified was an invalid.  */
    public static class InvalidDigestException extends S3ServerException {
        public InvalidDigestException (String message) {
            this(message, null, null);
        }

        public InvalidDigestException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The requested range is not satisfiable.  */
    public static class InvalidRangeException extends S3ServerException {
        public InvalidRangeException (String message) {
            this(message, null, null);
        }

        public InvalidRangeException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The provided security credentials are not valid.  */
    public static class InvalidSecurityException extends S3ServerException {
        public InvalidSecurityException (String message) {
            this(message, null, null);
        }

        public InvalidSecurityException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The SOAP request body is invalid.  */
    public static class InvalidSOAPRequestException extends S3ServerException {
        public InvalidSOAPRequestException (String message) {
            this(message, null, null);
        }

        public InvalidSOAPRequestException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The storage class you specified is not valid.  */
    public static class InvalidStorageClassException extends S3ServerException {
        public InvalidStorageClassException (String message) {
            this(message, null, null);
        }

        public InvalidStorageClassException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The target bucket for logging does not exist or is not owned by you.  */
    public static class InvalidTargetBucketForLoggingException extends S3ServerException {
        public InvalidTargetBucketForLoggingException (String message) {
            this(message, null, null);
        }

        public InvalidTargetBucketForLoggingException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your key is too long.  */
    public static class KeyTooLongException extends S3ServerException {
        public KeyTooLongException (String message) {
            this(message, null, null);
        }

        public KeyTooLongException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Couldn't parse the specified URI.  */
    public static class InvalidURIException extends S3ServerException {
        public InvalidURIException (String message) {
            this(message, null, null);
        }

        public InvalidURIException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The XML you provided was not well-formed or did not validate against our published schema.  */
    public static class MalformedACLErrorException extends S3ServerException {
        public MalformedACLErrorException (String message) {
            this(message, null, null);
        }

        public MalformedACLErrorException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The XML you provided was not well-formed or did not validate against our published schema.  */
    public static class MalformedXMLErrorException extends S3ServerException {
        public MalformedXMLErrorException (String message) {
            this(message, null, null);
        }

        public MalformedXMLErrorException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your request was too big.  */
    public static class MaxMessageLengthExceededException extends S3ServerException {
        public MaxMessageLengthExceededException (String message) {
            this(message, null, null);
        }

        public MaxMessageLengthExceededException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your metadata headers exceed the maximum allowed metadata size.  */
    public static class MetadataTooLargeException extends S3ServerException {
        public MetadataTooLargeException (String message) {
            this(message, null, null);
        }

        public MetadataTooLargeException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The specified method is not allowed against this resource.  */
    public static class MethodNotAllowedException extends S3ServerException {
        public MethodNotAllowedException (String message) {
            this(message, null, null);
        }

        public MethodNotAllowedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** A SOAP attachment was expected, but none were found.  */
    public static class MissingAttachmentException extends S3ServerException {
        public MissingAttachmentException (String message) {
            this(message, null, null);
        }

        public MissingAttachmentException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** You must provide the Content-Length HTTP header.  */
    public static class MissingContentLengthException extends S3ServerException {
        public MissingContentLengthException (String message) {
            this(message, null, null);
        }

        public MissingContentLengthException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The SOAP 1.1 request is missing a security element.  */
    public static class MissingSecurityElementException extends S3ServerException {
        public MissingSecurityElementException (String message) {
            this(message, null, null);
        }

        public MissingSecurityElementException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your request was missing a required header.  */
    public static class MissingSecurityHeaderException extends S3ServerException {
        public MissingSecurityHeaderException (String message) {
            this(message, null, null);
        }

        public MissingSecurityHeaderException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** There is no such thing as a logging status sub-resource for a key.  */
    public static class NoLoggingStatusForKeyException extends S3ServerException {
        public NoLoggingStatusForKeyException (String message) {
            this(message, null, null);
        }

        public NoLoggingStatusForKeyException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The specified bucket does not exist.  */
    public static class NoSuchBucketException extends S3ServerException {
        public NoSuchBucketException (String message) {
            this(message, null, null);
        }

        public NoSuchBucketException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The specified key does not exist.  */
    public static class NoSuchKeyException extends S3ServerException {
        public NoSuchKeyException (String message) {
            this(message, null, null);
        }

        public NoSuchKeyException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** A header you provided implies functionality that is not implemented.  */
    public static class NotImplementedException extends S3ServerException {
        public NotImplementedException (String message) {
            this(message, null, null);
        }

        public NotImplementedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your account is not signed up for the S3 service. You must sign up before you can use S3. You can sign up at the following URL: http://aws.amazon.com/s3  */
    public static class NotSignedUpException extends S3ServerException {
        public NotSignedUpException (String message) {
            this(message, null, null);
        }

        public NotSignedUpException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** At least one of the pre-conditions you specified did not hold.  */
    public static class PreconditionFailedException extends S3ServerException {
        public PreconditionFailedException (String message) {
            this(message, null, null);
        }

        public PreconditionFailedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Your socket connection to the server was not read from or written to within the timeout period.  */
    public static class RequestTimeoutException extends S3ServerException {
        public RequestTimeoutException (String message) {
            this(message, null, null);
        }

        public RequestTimeoutException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The difference between the request time and the server's time is too large.  */
    public static class RequestTimeTooSkewedException extends S3ServerException {
        public RequestTimeTooSkewedException (String message) {
            this(message, null, null);
        }

        public RequestTimeTooSkewedException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** Requesting the torrent file of a bucket is not permitted.  */
    public static class RequestTorrentOfBucketErrorException extends S3ServerException {
        public RequestTorrentOfBucketErrorException (String message) {
            this(message, null, null);
        }

        public RequestTorrentOfBucketErrorException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }

    /** The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method. Consult the documentation under Authenticating REST Requests and Authenticating SOAP Requests for details.  */
    public static class SignatureDoesNotMatchException extends S3ServerException {
        public SignatureDoesNotMatchException (String message) {
            this(message, null, null);
        }

        public SignatureDoesNotMatchException (String message, String requestId, String hostId) {
            super(message, requestId, hostId);
        }
    }
}