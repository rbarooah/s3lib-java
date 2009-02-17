/* 
 * S3Connection vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2009 Plausible Labs Cooperative, Inc.
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

package com.threerings.s3.client;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.httpclient.HttpMethod;

import java.io.InputStream;
import java.io.IOException;

/**
 * Auto-closing HTTP connection input stream. The method will automatically
 * be closed, and associated resources returned when the end of the stream
 * is reached, the stream is closed, or finalization of the object occurs.
 */
class HttpInputStream extends AutoCloseInputStream {
    /** Wrapped HTTP method */
    private final HttpMethod method;

    /**
     * Wrap the provided body stream. 
     */
    public HttpInputStream (InputStream stream, HttpMethod method) {
        super(stream);

        this.method = method;
    }

    /**
     * Handle finalization. Release the connection and
     * return the client instance to the object pool
     */
    @Override
    public void close () throws IOException {
        try {
            super.close();
        } finally {
            method.releaseConnection();
        }
    }
}