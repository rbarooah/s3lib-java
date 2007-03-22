//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code.
//
// (c) 2006 Three Rings Design, Inc.
// (c) 2006 Amazon Digital Services, Inc. or its affiliates.

package com.threerings.s3.client;

import com.threerings.s3.client.acl.AccessControlList;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.HashMap;

import org.apache.commons.codec.binary.Hex;

public class AWSAuthConnectionTest extends TestCase
{
    public AWSAuthConnectionTest (String name)
    {
        super(name);
    }
    
    public void setUp ()
        throws Exception
    {
        FileOutputStream fileOutput;

        _awsId = System.getProperty("aws.id");
        _awsKey = System.getProperty("aws.key");
        _conn = new AWSAuthConnection(_awsId, _awsKey);
        _testBucketName = "test-" + _awsId;
        _testFile = File.createTempFile("S3FileObjectTest", null);

        // Create a file object
        fileOutput = new FileOutputStream(_testFile);
        fileOutput.write(TEST_DATA.getBytes("utf8"));
        _fileObj = new S3FileObject("aKey", _testFile);
        
        // Create the test bucket
        _conn.createBucket(_testBucketName, null);
    }
    
    public void tearDown ()
        throws Exception
    {
        _conn.deleteBucket(_testBucketName, null);
        _testFile.delete();
    }

    public void testCreateBucket ()
        throws Exception
    {
        // No exception, all is well.
        _conn.createBucket(_testBucketName + "testCreateBucket", null);
        _conn.deleteBucket(_testBucketName + "testCreateBucket", null);
    }
    
    public void testPutObject ()
        throws Exception
    {
        // Send it to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);

        // Hey, you can't have that!
        _conn.deleteObject(_testBucketName, _fileObj);
    }


    public void testGetObject ()
        throws Exception
    {
        S3Object remote;

        // Send it to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);

        try {
            // Fetch it back out again
            S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());

            // Ensure that it is equal to the object we uploaded
            S3ObjectTest.testEquals(_fileObj, obj, this);

            // Validate the object file data, too.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream input = obj.getInputStream();
            byte[] data = new byte[1024];
            int nread;

            while ((nread = input.read(data)) > 0) {
                output.write(data, 0, nread);

                // Sanity check. We didn't upload more than 2 megs!
                if (output.size() > 2048) {
                    break;
                }
            }

            input.close();
            assertEquals(TEST_DATA, output.toString("utf8"));
        } finally {
            // Hey, you can't have that!
            _conn.deleteObject(_testBucketName, _fileObj);            
        }
    }

    public void testObjectMetadata ()
        throws Exception
    {
        HashMap<String,String> metadata;
        
        // Some test data
        metadata = new HashMap<String,String>();
        metadata.put("meta", "value whitespace");
        _fileObj.setMetadata(metadata);

        // Send it to the mother ship
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);

        try {
            // Fetch it back out again and validate the metadata
            S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());

            // Ensure that it is equal to the object we uploaded
            S3ObjectTest.testEquals(_fileObj, obj, this);
        } finally {
            // Hey, you can't have that!
            _conn.deleteObject(_testBucketName, _fileObj);            
        }
    }

    public void testErrorHandling ()
        throws Exception
    {
        AWSAuthConnection badConn = new AWSAuthConnection(_awsId, "bad key");
        try {
            badConn.createBucket(_testBucketName, null);
            fail("Did not throw S3SignatureDoesNotMatchException");            
        } catch (S3ServerException.SignatureDoesNotMatchException e) {
            // Do nothing
        }
    }
    
    /** Amazon S3 Authenticated Connection */
    protected AWSAuthConnection _conn;
    
    /** Amazon Web Services ID */
    protected String _awsId;
    
    /** Amazon Web Services Key */
    protected String _awsKey;
    
    /** Test bucket */
    protected String _testBucketName;
    
    /** Test file. */
    protected File _testFile;
    
    /** Test object. */
    protected S3FileObject _fileObj;

    /** Test data. */
    protected static final String TEST_DATA = "Hello, World!";
}