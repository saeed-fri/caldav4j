/*
 * Copyright 2005 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.caldav4j;
import java.io.InputStream;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.osaf.caldav4j.credential.CaldavCredential;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;
import org.osaf.caldav4j.methods.DeleteMethod;
import org.osaf.caldav4j.methods.HttpClient;
import org.osaf.caldav4j.methods.MkCalendarMethod;
import org.osaf.caldav4j.methods.PutMethod;
import org.osaf.caldav4j.util.CaldavStatus;
import org.osaf.caldav4j.util.UrlUtils;

public abstract class BaseTestCase   implements TestConstants {
    protected static final Log log = LogFactory.getLog(BaseTestCase.class);
    protected HttpClient testHttpClient;
    protected HttpClient httpClient;

    protected HostConfiguration hostConfig;
    protected CaldavCredential caldavCredential = new CaldavCredential();
    public  String COLLECTION_PATH;
    protected CalDAV4JMethodFactory methodFactory = new CalDAV4JMethodFactory();


    @Before
    public void setUp() throws Exception {
        COLLECTION_PATH = caldavCredential.home + caldavCredential.collection;
        hostConfig = createHostConfiguration();
        testHttpClient  = createHttpClient();
        httpClient = createHttpClient();    	
    }
    
    @After
    public void tearDown() throws Exception {
    	
    }
    


    // constructor
    public BaseTestCase(String method) {
	}
    public BaseTestCase() {
	}

	public HttpClient createHttpClient(){
        HttpClient http = new HttpClient();

        Credentials credentials = new UsernamePasswordCredentials(caldavCredential.user, 
        		caldavCredential.password);
        http.getState().setCredentials(
        		new AuthScope(this.getCalDAVServerHost(), this.getCalDAVServerPort()),
        		credentials);
        http.getParams().setAuthenticationPreemptive(true);
        return http;
    }
	public static HttpClient createHttpClient(CaldavCredential caldavCredential){
        HttpClient http = new HttpClient();

        Credentials credentials = new UsernamePasswordCredentials(caldavCredential.user, 
        		caldavCredential.password);
        http.getState().setCredentials(
        		new AuthScope(caldavCredential.host, caldavCredential.port),
        		credentials);
        http.getParams().setAuthenticationPreemptive(true);
        return http;
    }
    public static HttpClient createHttpClientWithNoCredentials(){

        HttpClient http = new HttpClient();
        http.getParams().setAuthenticationPreemptive(true);
        return http;
    }
    public HostConfiguration createHostConfiguration(){
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(getCalDAVServerHost(), getCalDAVServerPort(), getCalDavSeverProtocol());
        return hostConfig;
    }
    public static HostConfiguration createHostConfiguration(CaldavCredential caldavCredential){
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(caldavCredential.host,caldavCredential.port, caldavCredential.protocol);
        return hostConfig;
    }
    


    // TODO testme
    public static Calendar getCalendarResource(String resourceName) {
        Calendar cal;

        InputStream stream = BaseTestCase.class.getClassLoader()
                .getResourceAsStream(resourceName);
        CalendarBuilder cb = new CalendarBuilder();
        
        try {
            cal = cb.build(stream);
        } catch (Exception e) {        	
            throw new RuntimeException("Problems opening file:" + resourceName + "\n" + e);
        }
        
        return cal;
    }    
    
    /***
     * FIXME this put updates automatically the timestamp of the event 
     * @param resourceFileName
     * @param path
     */
    protected void put(String resourceFileName, String path) {    	
        PutMethod put = methodFactory.createPutMethod();
        InputStream stream = this.getClass().getClassLoader()
        .getResourceAsStream(resourceFileName);
        String event = UrlUtils.parseISToString(stream);
        event = event.replaceAll("DTSTAMP:.*", "DTSTAMP:" + new DateTime(true).toString());
        log.debug(new DateTime(true).toString());
        //log.trace(event);        
        
        put.setRequestEntity(event);
        put.setPath(path);
    	log.debug("\nPUT " + put.getPath());
        try {
            testHttpClient.executeMethod(hostConfig, put);
            
            int statusCode =  put.getStatusCode();
            
            switch (statusCode) {
			case CaldavStatus.SC_CREATED:
			case CaldavStatus.SC_NO_CONTENT:
				break;
			case CaldavStatus.SC_PRECONDITION_FAILED:
				log.error("item exists?");
				break;
			case CaldavStatus.SC_CONFLICT:
				log.error("conflict: item still on server");
			default:
                log.error(put.getResponseBodyAsString());
				throw new Exception("trouble executing PUT of " +resourceFileName + "\nresponse:" + put.getResponseBodyAsString());

			}
        } catch (Exception e){
        	log.info("Error while put():" + e.getMessage());
            throw new RuntimeException(e);
        }

    }
    
    protected void del(String path){
        DeleteMethod delete = new DeleteMethod();
        delete.setPath(path);
        try {
        	testHttpClient.executeMethod(hostConfig, delete);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    
    protected void mkcalendar(String path){
        MkCalendarMethod mk = new MkCalendarMethod();
        mk.setPath(path);
        mk.addDescription(CALENDAR_DESCRIPTION, "en");
        try {
        	testHttpClient.executeMethod(hostConfig, mk);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    
	/**
	 * put an event on a caldav store using UID.ics
	 */
	 protected void caldavPut(String s) {    	 
		 Calendar cal = getCalendarResource(s);

		 String resPath = COLLECTION_PATH + "/" +cal.getComponent("VEVENT").getProperty("UID").getValue() + ".ics";
		 put (s, resPath );

	 }

	 /**
	  * remove an event on a caldav store using UID.ics
	  */
	 protected void caldavDel(String s) {
		 Calendar cal = getCalendarResource(s);
		 String delPath = COLLECTION_PATH + "/" +cal.getComponent("VEVENT").getProperty("UID").getValue() + ".ics";
		 log.debug("DEL " + delPath);
		 del(delPath);

	 }
	 
		protected CalDAVCollection createCalDAVCollection() {
			CalDAVCollection calendarCollection = new CalDAVCollection(
					COLLECTION_PATH, createHostConfiguration(), methodFactory,
					CalDAVConstants.PROC_ID_DEFAULT);
			return calendarCollection;
		}

		// getter+setter
	    public String getCalDAVServerHost() {
	        return caldavCredential.host;
	    }
	    
	    public int getCalDAVServerPort(){
	        return caldavCredential.port;
	    }
	    
	    public String getCalDavSeverProtocol(){
	        return caldavCredential.protocol;
	    }
	    
	    public String getCalDavSeverWebDAVRoot(){
	        return caldavCredential.home;
	    }
	    
	    public String getCalDavSeverUsername(){
	        return caldavCredential.user;
	    }
	    
	    public String getCalDavSeverPassword(){
	        return caldavCredential.password;
	    }
	        
}
