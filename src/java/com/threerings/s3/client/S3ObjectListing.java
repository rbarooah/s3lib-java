//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

package com.amazon.s3;

import java.io.InputStream;
import java.io.IOException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.SimpleTimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;


/**
 * Returned by S3Connection.listBucket()
 */
public class S3ObjectListing {

    /**
     * The name of the bucket being listed.
     */
    public String name;

    /**
     * The prefix echoed back from the request.
     */
    public String prefix;

    /**
     * The marker echoed back from the request.
     */
    public String marker;

    /**
     * The delimiter echoed back from the request.  Null if not specified in
     * the request.
     */
    public String delimiter = null;

    /**
     * The maxKeys echoed back from the request if specified.
     */
    public int maxKeys;

    /**
     * Indicates if there are more results to the list.  True if the current
     * list results have been truncated.
     */
    public boolean isTruncated;

    /**
     * Indicates what to use as a marker for subsequent list requests in the event
     * that the results are truncated.  Present only when a delimiter is specified.  
     */
    public String nextMarker = null;

    /**
     * A List of objects in the given bucket.  
     */
    public List<S3ObjectEntry> entries;

    /**
     * A List of prefixes representing the common prefixes of the
     * keys that matched up to the delimiter.
     */
    public List<String> commonPrefixes;

    public S3ObjectListing (InputStream dataStream)
        throws IOException, SAXException
    {
        XMLReader xr = XMLReaderFactory.createXMLReader();
        ObjectListingHandler handler = new ObjectListingHandler();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        xr.parse(new InputSource(dataStream));

        this.name = handler.getName();
        this.prefix = handler.getPrefix();
        this.marker = handler.getMarker();
        this.delimiter = handler.getDelimiter();
        this.maxKeys = handler.getMaxKeys();
        this.isTruncated = handler.getIsTruncated();
        this.nextMarker = handler.getNextMarker();
        this.entries = handler.getKeyEntries();
        this.commonPrefixes = handler.getCommonPrefixes();
    }

    protected class ObjectListingHandler extends DefaultHandler {

        private String name = null;
        private String prefix = null;
        private String marker = null;
        private String delimiter = null;
        private int maxKeys = 0;
        private boolean isTruncated = false;
        private String nextMarker = null;
        private boolean isEchoedPrefix = false;
        private List<S3ObjectEntry> keyEntries = null;
        private S3ObjectEntry keyEntry = null;
        private List<String> commonPrefixes = null;
        private String commonPrefix = null;
        private StringBuffer currText = null;
        private SimpleDateFormat iso8601Parser = null;

        public ObjectListingHandler () {
            super();
            keyEntries = new ArrayList<S3ObjectEntry>();
            commonPrefixes = new ArrayList<String>();
            this.iso8601Parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            this.iso8601Parser.setTimeZone(new SimpleTimeZone(0, "GMT"));
            this.currText = new StringBuffer();
        }

        public void startDocument () {
            this.isEchoedPrefix = true;
        }

        public void endDocument () {
            // ignore
        }

        public void startElement (String uri, String name, String qName, Attributes attrs) {
            if (name.equals("Contents")) {
                this.keyEntry = new S3ObjectEntry();
            } else if (name.equals("Owner")) {
                this.keyEntry.owner = new S3Owner();
            }
        }

        public void endElement (String uri, String name, String qName)
            throws SAXException
        {
            if (name.equals("Name")) {
                this.name = this.currText.toString();
            } 
            // this prefix is the one we echo back from the request
            else if (name.equals("Prefix") && this.isEchoedPrefix) {
                this.prefix = this.currText.toString();
                this.isEchoedPrefix = false;
            } else if (name.equals("Marker")) {
                this.marker = this.currText.toString();
            } else if (name.equals("MaxKeys")) {
                this.maxKeys = Integer.parseInt(this.currText.toString());
            } else if (name.equals("Delimiter")) {
                this.delimiter = this.currText.toString();
            } else if (name.equals("IsTruncated")) {
                this.isTruncated = Boolean.parseBoolean(this.currText.toString());
            } else if (name.equals("NextMarker")) {
                this.nextMarker = this.currText.toString();
            } else if (name.equals("Contents")) {
                this.keyEntries.add(this.keyEntry);
            } else if (name.equals("Key")) {
                this.keyEntry.key = this.currText.toString();
            } else if (name.equals("LastModified")) {
                try {
                    this.keyEntry.lastModified = this.iso8601Parser.parse(this.currText.toString());
                } catch (ParseException e) {
                    throw new SAXException("Unexpected date format in list bucket output", e);
                }
            } else if (name.equals("ETag")) {
                this.keyEntry.eTag = this.currText.toString();
            } else if (name.equals("Size")) {
                this.keyEntry.size = Long.parseLong(this.currText.toString());
            } else if (name.equals("StorageClass")) {
                this.keyEntry.storageClass = this.currText.toString();
            } else if (name.equals("ID")) {
                this.keyEntry.owner.id = this.currText.toString();
            } else if (name.equals("DisplayName")) {
                this.keyEntry.owner.displayName = this.currText.toString();
            } else if (name.equals("CommonPrefixes")) {
                this.commonPrefixes.add(this.commonPrefix);
            }
            // this is the common prefix for keys that match up to the delimiter 
            else if (name.equals("Prefix")) {
                this.commonPrefix = this.currText.toString();
            }
            if(this.currText.length() != 0)
                this.currText = new StringBuffer();
        }

        public void characters(char ch[], int start, int length) {
            this.currText.append(ch, start, length);
        }

        public String getName () {
            return this.name;
        }

        public String getPrefix () {
            return this.prefix;
        }

        public String getMarker () {
            return this.marker;
        }

        public String getDelimiter () {
            return this.delimiter;
        }

        public int getMaxKeys () {
            return this.maxKeys;
        }

        public boolean getIsTruncated () {
            return this.isTruncated;
        }

        public String getNextMarker () {
            return this.nextMarker;
        }

        public List<S3ObjectEntry> getKeyEntries () {
            return this.keyEntries;
        }

        public List<String> getCommonPrefixes () {
            return this.commonPrefixes;
        }
    }
}