/* 
 * S3Connection vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2005 - 2007 Three Rings Design, Inc.
 * Copyright (c) 2006 Amazon Digital Services, Inc. or its affiliates.
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

package com.threerings.s3.client;

import com.threerings.s3.client.acl.AccessControlList;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;

import org.xml.sax.SAXException;

/**
 * An interface into the S3 system.  It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate S3 data.
 *
 * S3Connection instances are thread-safe.
 */
public class S3Connection {
    /**
     * Create a new S3 client connection, with the given credentials and connection
     * host parameters.
     * 
     * Connections will be SSL encrypted.
     *
     * @param keyId Your unique AWS user id.
     * @param secretKey The secret string used to generate signatures
     *        for authentication.
     */
    public S3Connection (String keyId, String secretKey) {
        this(keyId, secretKey, S3Utils.createDefaultHostConfig());
    }

    /**
     * Create a new S3 client connection, with the given credentials and connection
     * host parameters.
     *
     * @param keyId The your user key into AWS
     * @param secretKey The secret string used to generate signatures for authentication.
     * @param hostConfig HttpClient HostConfig.
     */
    public S3Connection (String keyId, String secretKey,
        HostConfiguration hostConfig)
    {
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.httpClient = new HttpClient();
        this.httpClient.setHostConfiguration(hostConfig);

        /* Configure the multi-threaded connection manager. Default to MAX_INT (eg, unlimited) connections, as
         * S3 is intended to support such use */
        HttpConnectionManagerParams managerParam = new HttpConnectionManagerParams();
        MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
        managerParam.setDefaultMaxConnectionsPerHost(Integer.MAX_VALUE);
        managerParam.setMaxTotalConnections(Integer.MAX_VALUE);
        manager.setParams(managerParam);;
        this.httpClient.setHttpConnectionManager(manager);
    }

    /**
     * @deprecated Use {@link S3Connection#S3Connection(String, String, HostConfiguration)}
     */
    @Deprecated
    public S3Connection (String awsKeyId, String awsSecretKey, Protocol protocol) {
        this(awsKeyId, awsSecretKey, protocol, S3Utils.DEFAULT_HOST);
    }

    /**
     * @deprecated Use {@link S3Connection#S3Connection(String, String, HostConfiguration)}
     */
    @Deprecated
    public S3Connection (String awsKeyId, String awsSecretKey, Protocol protocol, String host) {
        this(awsKeyId, awsSecretKey, protocol, host, protocol.getDefaultPort());
    }
    
    /**
     * @deprecated Use {@link S3Connection#S3Connection(String, String, HostConfiguration)}
     */
    @Deprecated
    public S3Connection (String awsKeyId, String awsSecretKey, Protocol protocol, String host, int port) {
        this(awsKeyId, awsSecretKey, S3Utils.createHostConfig(host, port, protocol));
    }

    /**
     * Creates a new bucket.
     * @param bucketName The name of the bucket to create.
     */
    public void createBucket (String bucketName)
        throws S3Exception
    {
        PutMethod method;
        try {
            method = new PutMethod("/" + _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }


    /**
     * List a bucket's contents. May return a truncated list.
     */
    public S3ObjectListing listObjects (String bucketName)
        throws S3Exception
    {
        return listObjects(bucketName, null, null, 0, null);
    }

    /**
     * List a bucket's contents, with a maximum number of
     * returned entries.
     * 
     * @param marker Indicates where in the bucket to begin listing. The
     *  list will only include keys that occur lexiocgraphically after marker.
     *  Specify null for no marker.
     * @param maxKeys Maximum number of keys to return. The server may return
     *  fewer keys, but never more. Specify 0 for no limit.
     */
    public S3ObjectListing listObjects (String bucketName, String marker, int maxKeys)
        throws S3Exception
    {
        return listObjects(bucketName, null, marker, maxKeys, null);
    }

    /**
     * List a bucket's contents.
     * @param prefix Limits response to keys beginning with the provided prefix.
     *  Specify null for no prefix.
     * @param marker Indicates where in the bucket to begin listing. The list
     *  will only include keys that occur lexicographically after marker.
     *  Specify null for no marker.
     * @param maxKeys Maximum number of keys to return. The server may return
     *  fewer keys, but never more. Specify 0 for no limit.
     * @param delimiter Keys that contain the same string between the prefix
     *  and the first occurence of the delimiter will be rolled up into a
     *  single result element in the CommonPrefixes data. Specify null for no
     *  delimiter.
     */
    public S3ObjectListing listObjects (String bucketName, String prefix, String marker, int maxKeys, String delimiter)
        throws S3Exception
    {
        GetMethod method;
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(4);

        try {
            method = new GetMethod("/" + _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);            
        }
    
        if (prefix != null) {
            parameters.add(new NameValuePair(LIST_PREFIX_PARAMETER, prefix));
        }

        if (marker != null) {
            parameters.add(new NameValuePair(LIST_MARKER_PARAMETER, marker));
        }

        if (maxKeys != 0) {
            parameters.add(new NameValuePair(LIST_MAXKEYS_PARAMETER, Integer.toString(maxKeys)));
        }

        if (delimiter != null) {
            parameters.add(new NameValuePair(LIST_DELIMITER_PARAMETER, delimiter));
        }

        if (parameters.size() > 0) {
            method.setQueryString(
                (NameValuePair[]) parameters.toArray(new NameValuePair[parameters.size()])
            );
        }

        try {
            executeS3Method(method);
            return new S3ObjectListing(method.getResponseBodyAsStream());          
        } catch (SAXException se) {
            throw new S3ClientException("Error parsing bucket GET response: " + se.getMessage(), se);
        } catch (IOException ioe) {
            throw new S3ClientException.NetworkException("Error receiving bucket GET response: " +
                ioe.getMessage(), ioe);
        } finally {
            method.releaseConnection();
        }
    }


    /**
     * Deletes a bucket.
     * @param bucketName The name of the bucket to delete.
     */
    public void deleteBucket (String bucketName)
        throws S3Exception
    {
        DeleteMethod method;
        try {
            method = new DeleteMethod("/" +
                _urlEncoder.encode(bucketName));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + ": " + e);
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Upload an S3 Object, using a PRIVATE access policy.
     * Equivalent to calling putObject(bucketName, object, AccessControlList.StandardPolicy.PRIVATE)
     *
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     */
    public void putObject (String bucketName, S3Object object)
        throws S3Exception
    {
        putObject(bucketName, object, AccessControlList.StandardPolicy.PRIVATE);
    }

    /**
     * Upload an S3 Object.
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     * @param accessPolicy S3 Object's access policy. 
     */
    public void putObject (String bucketName, S3Object object,
        AccessControlList.StandardPolicy accessPolicy)
        throws S3Exception
    {
        putObject(bucketName, object, accessPolicy, new HashMap<String,String>());
    }

    /**
     * Upload an S3 Object.
     * @param bucketName Destination bucket.
     * @param object S3 Object.
     * @param accessPolicy S3 Object's access policy. 
	 * @param headers http headers to be served with the object.
     */
    public void putObject (String bucketName, S3Object object,
        AccessControlList.StandardPolicy accessPolicy, Map<String,String> headers)
        throws S3Exception
    {
        PutMethod method;
        byte[] checksum;

        try {
            method = new PutMethod("/" + _urlEncoder.encode(bucketName) +
                "/" + _urlEncoder.encode(object.getKey()));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + " and key " +
                object.getKey() + ": " + e);
        }

        // Set the request entity
        method.setRequestEntity(new InputStreamRequestEntity(
            object.getInputStream(), object.length(), object.getMimeType()));

        // Set the access policy
        method.setRequestHeader(S3Utils.ACL_HEADER, accessPolicy.toString());

        // add any headers that were supplied
        for (Map.Entry<String,String> header : headers.entrySet()) {
            method.setRequestHeader(header.getKey(), header.getValue());
        }

        // Compute and set the content-md5 value (base64 of 128bit digest)
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
        try {
            checksum = Base64.encodeBase64(object.getMD5());
            method.setRequestHeader(CONTENT_MD5_HEADER, new String(checksum, "ascii"));            
        } catch (UnsupportedEncodingException uee) {
            // ASCII must always be supported.
            throw new RuntimeException("Missing ASCII encoding");
        }

        // Set any metadata fields
        for (Map.Entry<String,String> entry : object.getMetadata().entrySet()) {
            String header = S3_METADATA_PREFIX + entry.getKey();
            method.setRequestHeader(header, entry.getValue());
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Retrieve an S3Object, using the provided HttpMethodBase.
     * 
     * @param objectKey The object key request, used to instantiate the returned S3Object.
     * @param method The HTTP method to execute.
     * @param hasBody Set to true if a response body is expected (eg, for an HTTP GET request)
     */
    private S3Object getObject (String objectKey, HttpMethodBase method, boolean hasBody)
    	throws S3Exception
    {
        final InputStream response;
        final HashMap<String,String> metadata;
        final String mimeType;
        final byte digest[];
        final long length;
        boolean success = false;
        long lastModified = 0L;

        /* Attempt the GET, and release the held method connection on failure */
        try {
            // Execute the get request and retrieve all metadata from the response
            executeS3Method(method);

            // Mime type
            mimeType = getResponseHeader(method, CONTENT_TYPE_HEADER, true);
        
            // Last modified
            final String dateString = getResponseHeader(method, LAST_MODIFIED_HEADER, false);
            try {
                if (dateString != null)
                    lastModified = DateUtil.parseDate(dateString).getTime();
            } catch (DateParseException e) {
                lastModified = 0L;
            }
        
            // Data length
            length = method.getResponseContentLength();
            if (length == -1) {
                throw new S3Exception("S3 failed to supply the Content-Length header");            
            }

            // MD5 Checksum. S3 returns this as the standard 128bit hex string, enclosed
            // in quotes.
            try {
                String hex;
            
                hex = getResponseHeader(method, S3_MD5_HEADER, true);
                // Strip the surrounding quotes
                hex = hex.substring(1, hex.length() - 1);
                digest = new Hex().decode(hex.getBytes("utf8"));
            } catch (DecoderException de) {
                throw new S3Exception("S3 returned an invalid " + S3_MD5_HEADER + " header: " +
                    de);
            } catch (UnsupportedEncodingException uee) {
                // UTF8 must always be supported.
                throw new RuntimeException("Missing UTF8 encoding");
            }

            // Retrieve metadata
            metadata = new HashMap<String,String>();
            for (Header header : method.getResponseHeaders()) {
                String name;

                name = header.getName();
                if (name.startsWith(S3_METADATA_PREFIX)) {
                    // Strip the S3 prefix
                    String key = name.substring(S3_METADATA_PREFIX.length());
                    metadata.put(key, header.getValue());
                }
            }

            if (hasBody) {
                // Get the response body as an "auto closing" stream -- it will close the HTTP connection
                // when the stream is closed, the end of the stream is reached, or finalization occurs.
                try {
                    InputStream s = method.getResponseBodyAsStream();
                    response = new HttpInputStream(s, method);
                } catch (IOException ioe) {
                    throw new S3ClientException.NetworkException("Error receiving object " + method.getName() +
                    	"response: " + ioe.getMessage(), ioe);
                }

                if (response == null) {
                    // A body was expected
                    throw new S3Exception("S3 failed to return any document body");
                }

                /* Finished successfully */
                success = true;
                return new S3StreamObject(objectKey, mimeType, length, digest, metadata, response, lastModified);        
            } else {
            	return new S3EmptyObject(objectKey, mimeType, length, digest, metadata, lastModified);
            }
        } finally {
            /* If a body was requested and the request was successful, cleanup will be handled by
             * the HttpInputStream. Otherwise, return the method now. */
            if (hasBody && success) {
                // Concluded successfully
            } else {
                method.releaseConnection();
            }
        }
    }

    /**
     * Retrieve a S3Object. The object's data streams directly from the remote
     * server, and thus may be invalidated.
     *
     * @param bucketName Source bucket.
     * @param objectKey Object key.
     */
    public S3Object getObject (String bucketName, String objectKey)
        throws S3Exception
    {
        GetMethod method;

        try {
            method = new GetMethod("/" + _urlEncoder.encode(bucketName) +
                "/" + _urlEncoder.encode(objectKey));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + " and key " +
                objectKey + ": " + e);
        }

        return getObject(objectKey, method, true);
    }

    /**
     * Retrieve an S3Object's metadata. The data stream is not retrieved (a HEAD request is
     * performed). Any attempt to read() the returned S3Object's input stream will throw
     * an IOException.
     *
     * @param bucketName Source bucket.
     * @param objectKey Object key.
     */
    public S3Object getObjectMetadata (String bucketName, String objectKey)
        throws S3Exception
    {
        HeadMethod method;

        try {
            method = new HeadMethod("/" + _urlEncoder.encode(bucketName) +
                "/" + _urlEncoder.encode(objectKey));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
                "Encoding error for bucket " + bucketName + " and key " +
                objectKey + ": " + e);
        }

        return getObject(objectKey, method, false);
    }

    /**
     * Delete a remote S3 Object.
     * @param bucketName Remote bucket.
     * @param objectKey S3 object key.
     */
    public void deleteObject (String bucketName, String objectKey)
        throws S3Exception
    {
        DeleteMethod method;
        try {
            method = new DeleteMethod("/" +
                _urlEncoder.encode(bucketName) + "/" +
                _urlEncoder.encode(objectKey));
        } catch (EncoderException e) {
            throw new S3ClientException.InvalidURIException(
            "Encoding error for bucket " + bucketName + " and key " +
            objectKey + ": " + e);
        }

        try {
            executeS3Method(method);            
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Execute the provided method, translating any error response into the
     * appropriate S3Exception.
     * @param method HTTP method to execute.
     */
    private void executeS3Method (HttpMethod method)
        throws S3Exception
    {
        int statusCode;
        
        // Sign the request
        S3Utils.signAWSRequest(keyId, secretKey, method, null);
        
        // Execute the request
        try {
            statusCode = httpClient.executeMethod(method);            
        } catch (IOException ioe) {
            throw new S3ClientException.NetworkException("Network error executing S3 method: " +
                ioe.getMessage(), ioe);
        }

        if (!(statusCode >= HttpStatus.SC_OK &&
            statusCode < HttpStatus.SC_MULTIPLE_CHOICES)) {
            // Request failed, throw exception
            InputStream stream;
            byte[] errorDoc = new byte[S3_MAX_ERROR_SIZE];

            try {
                stream = method.getResponseBodyAsStream();
                if (stream == null) {
                    // We should always receive a response!
                    throw new S3Exception("S3 failed to return an error " +
                        "response for HTTP status code: "+ statusCode);
                }

                stream.read(errorDoc, 0, errorDoc.length);
            } catch (IOException ioe) {
                throw new S3ClientException.NetworkException("Network error receiving S3 error response: " + ioe.getMessage(), ioe);
            }

            throw S3ServerException.exceptionForS3Error(new String(errorDoc).trim());
        }
    }

    /**
     * Pull the header value out of the HTTP method response.
     */
    private String getResponseHeader (HttpMethod method, String name, boolean required)
        throws S3Exception
    {
        Header header;

        header = method.getResponseHeader(name);
        if (header == null) {
            if (required) {
                throw new S3Exception("S3 failed to return a " + name + " header");
            } else {
                return null;
            }
        }

        return header.getValue();
    }
    
    /** AWS Access ID. */
    private final String keyId;
    
    /** AWS Access Key. */
    private final String secretKey;
    
    /** S3 HTTP client. */
    private final HttpClient httpClient;
    
    /** URL encoder. */
    private final URLCodec _urlEncoder = new URLCodec();

    /** Prefix parameter. */
    private static final String LIST_PREFIX_PARAMETER = "prefix";

    /** Marker parameter. */
    private static final String LIST_MARKER_PARAMETER = "marker";
    
    /** Max Keys parameter. */
    private static final String LIST_MAXKEYS_PARAMETER = "max-keys";

    /** Delimiter parameter. */
    private static final String LIST_DELIMITER_PARAMETER = "delimiter";

    /** Maximum size of S3's error output. Should never be larger than 2k!!! */
    private static final int S3_MAX_ERROR_SIZE = 2048;

    /** Header for MD5 checksum validation. */
    private static final String CONTENT_MD5_HEADER = "Content-MD5";
    
    /** Last-Modified date header. */
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";

    /** Mime Type Header. */
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    /** Header for the MD5 digest in S3 GET responses. Not to be confused
     * with the Content-MD5 header that we use in PUT requests. */
    private static final String S3_MD5_HEADER = "ETag";

    /** Header prefix for object metadata. */
    private static final String S3_METADATA_PREFIX = "x-amz-meta-";
}