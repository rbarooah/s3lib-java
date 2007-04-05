/* 
 * UploadStream.java vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2007 Three Rings Design, Inc.
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

package com.threerings.s3.pipe;

import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.S3ByteArrayObject;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;

import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

/*
 * Uploads streams as a series of S3Objects.
 * UploadStreams are re-usable, but not thread-safe.
 */
public class UploadStreamer {
    /*
     * Instantiate a new stream uploader.
     * S3 transactions are expensive -- the block size should be large.
     * @param connect: S3 Connection.
     * @param bucket: Destination S3 bucket.
     * @param blocksize: Upload block size, in bytes.
     */
    public UploadStreamer (S3Connection connection, String bucket,
        int blocksize)
    {
        _connection = connection;
        _bucket = bucket;
        _blocksize = blocksize;
    }

    /**
     * Upload a stream, using the given streamName.
     */
    public void upload (String streamName, InputStream stream)
        throws IOException
    {
        QueuedStreamReader reader;
        Thread readerThread;

        /* Create and start the stream reader. */
        reader = new QueuedStreamReader(stream, _blocksize, QUEUE_SIZE);
        readerThread = new Thread(reader, streamName + " Queue");
        readerThread.start();

        /* Read blocks off the queue. */
        try {
            ByteBuffer block;
            long blockId = 0;

            while ((block = reader.readBlock()) != null) {
                byte[] data;
                int length;
                
                /* Get access to the byte[] data. */
                length = block.limit();
                if (!block.hasArray()) {
                    /* No backing array, copy the data. */
                    data = new byte[length];
                    block.get(data);
                } else {
                    /* Backing array, no copying required. */
                    data = block.array();
                }

                /* Upload the S3 Object. */
                S3ByteArrayObject obj = new S3ByteArrayObject(streamName + "." +
                    Long.toString(blockId), data, 0, length);

                try {
                    _connection.putObject(_bucket, obj, AccessControlList.StandardPolicy.PRIVATE);                    
                } catch (S3Exception s3e) {
                    // XXX Retry
                    readerThread.interrupt();
                    throw new IOException("S3 Upload failure: " + s3e.getMessage());
                }

                /* Increment the block id. */
                blockId++;
            }
        } catch (InterruptedException ie) {
            // This will exit our loop, which is sufficient.
        }

        /* Check for error and exit */
        if (reader.getStreamError() != null) {
            throw new IOException("Failure reading input stream: " + reader.getStreamError());
        }
    }

    /** Queue size. */
    private final int QUEUE_SIZE = 4;

    /** S3 Connection. */
    private S3Connection _connection;

    /** S3 Bucket. */
    private String _bucket;

    /** Read block size. */
    private final int _blocksize;
}
