//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2006 Three Rings Design, Inc.
//

package com.threerings.s3.client;

/** 
 * An exception that indicates a generic client-side S3 error.
 */
public class S3ClientException extends S3Exception
{
    public S3ClientException (String message) {
        super(message);
    }

    public S3ClientException (String message, Throwable cause) {
        super(message, cause);
    }

    /** Couldn't parse the specified URI.  */
    public static class InvalidURIException extends S3ClientException {
        public InvalidURIException (String message) {
            super(message);
        }

        public InvalidURIException (String message, Throwable cause) {
            super(message, cause);
        }
    }
}