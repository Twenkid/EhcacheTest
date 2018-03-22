/* 
// Ehache/Nimbus test

// Author: Todor "Tosh" Arnaudov, 2012
*/

package com.nimbusds.sso.store;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.sf.ehcache.Element;

import org.junit.Test;

import com.sun.mail.iap.ParsingException;

public class RMIDistributedStoreTestNew {

	@Test
	public final void testInit() throws StoreException {
		
		RMIDistributedStore storeTester = new RMIDistributedStore();
		try { 
			  net.sf.ehcache.config.Configuration config = null;
			  storeTester.init(new File("ehcache-testA.xml"));
		}
		catch (StoreException e){
		  //assertEquals("Result", 50, tester.multiply(10, 5));
			fail("StoreException!");
		}
	   Set<EntryEventListener> listeners = storeTester.getEntryEventListeners(new MapName("sessions"));
	   assertEquals("There are no listeners, the set must have 0 items", 0, listeners.size());
	   
	   storeTester.shutdown();
	}

	@Test
	public final void testAddEntryEventListener() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetEntryEventListeners() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRemoveEntryEventListener() {
		fail("Not yet implemented"); // TODO
	}


	//* Test Passed!
	@Test
	public final void testCreateMapIfAbsent() throws IllegalStateException, StoreException {
		//createMapIfAbsent
		RMIDistributedStore store = new RMIDistributedStore();
		//store.init(new File("W:\\Java\\Web\\war-test1\\ehcache-testA.xml"));
		store.init(new File("ehcache-testA.xml"));
		//store.init(new File("ehcache-distributed1.xml"));
		//store.init(new File("ehcache.xml"));
		
		assertTrue("sessions map must exist!", store.getCacheManager().cacheExists("sessions"));
		assertTrue("openid map must exist!", store.getCacheManager().cacheExists("openid"));
		
		store.createMapIfAbsent(new MapName("newmap"));
		assertTrue("newmap must exist!", store.getCacheManager().cacheExists("newmap"));
		
		//A map shouldn't be created if there's one with that name already created
		 String[] cacheNames = store.getCacheManager().getCacheNames();
		 int cachesNumberBefore = cacheNames.length;
		 		
		 store.createMapIfAbsent(new MapName("newmap"));
		 
		 int cachesNumberAfter = store.getCacheManager().getCacheNames().length;
		 
		 String message = "The number of caches after attempt of creation with the same name as an existing one shouldn't change!";
		 assertTrue(message, cachesNumberBefore == cachesNumberAfter);
		
		 store.shutdown();			
	}
   

	@Test
	public final void testGetMapNames() throws StoreException {
		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-testA.xml"));
		
		HashSet<MapName> mapNames = (HashSet<MapName>) store.getMapNames();
		
		HashSet<String> mapNamesString = new HashSet<String>();
		
		for(MapName map : mapNames){
		  mapNamesString.add(map.toString());
		}
		
		assertTrue("getMapNames must return a set of mapnames, including *sessions*!", mapNamesString.contains("sessions"));
		assertTrue("getMapNames must return a set of mapnames, including *openid*!", mapNamesString.contains("openid"));
		
						
		store.createMapIfAbsent(new MapName("newmap"));
		
		mapNamesString.clear();
		mapNames.clear();		
		mapNames = (HashSet<MapName>) store.getMapNames();
		
		for(MapName map : mapNames){
			  mapNamesString.add(map.toString());
			}
				
		assertTrue("getMapNames must return a set of mapnames, including *newmap*!", mapNamesString.contains("newmap"));
		
		store.shutdown();
	}

	@Test
	public final void testDeleteMap() throws StoreException {
		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-testA.xml"));
		
		int startNumber = store.getMapNames().size(); //Number of caches initially 
		
		store.deleteMap(new MapName("sessions"));				
		assertTrue("After deletion, the number of maps must be one less from " + startNumber, store.getMapNames().size()== startNumber - 1); //After deletion of one, the number should decrease
		
		int numberAfterDeletion = store.getMapNames().size();
		
		MapName mapNameAfterDeletion = new MapName("mapCreatedAfterDeletion");
		store.createMapIfAbsent(mapNameAfterDeletion);
		
		assertTrue("After creation of a new map, the number of maps must increase with one" + startNumber, store.getMapNames().size()==(numberAfterDeletion + 1)); //After deletion of one, the number should decrease
		
		Element el = new Element("Plovdiv", "Bulgaria");
		store.putEntry(mapNameAfterDeletion, (String) el.getKey(), (String) el.getValue(), 5, 5); //timeToLive= 5 & toIdle = 5 seconds
		
		boolean quiet = false;
		
		String readValue = (String) store.getEntryValue(mapNameAfterDeletion, (String) el.getKey(), quiet);
		
		assertEquals("Value doesn't match to the stored one!", (String) el.getValue(), readValue);
		
					
		store.shutdown();		
		//fail("Not yet implemented"); // TODO
 	}
   
	@Test
	public final void testPutEntry() throws StoreException {
		
		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-testA.xml"));
		
		MapName mapNameSessions = new MapName("sessions");
		MapName mapNameOpenID = new MapName("openid");
		
		Element el = new Element("Goshko", "1994");
		store.putEntry(mapNameSessions, (String) el.getKey(), (String) el.getValue(), 5, 5); //timeToLive= 5 & toIdle = 5 seconds
		
		boolean quiet = false;		
		
		String readValue = (String) store.getEntryValue(mapNameSessions, (String) el.getKey(), quiet);
				
		assertEquals("Value doesn't match to the stored one, using getEntryValue!", (String) el.getValue(), readValue);
		
		store.shutdown();
	}

	/* WARNING: putIfAbsent can't be used with current configuration!
	 * 
	 * It fails to work on the Ehcache size.
	 * 
	 * et.sf.ehcache.CacheException: You have configured the cache with a replication scheme that cannot properly support CAS operation guarantees.
	 *  at net.sf.ehcache.Cache.checkCASOperationSupported(Cache.java:3970)
	 * at net.sf.ehcache.Cache.putIfAbsent(Cache.java:3854)
	 * at com.nimbusds.sso.store.RMIDistributedStoreTestNew.testPutEntryIfAbsent(RMIDistributedStoreTestNew.java:255)
	 * See: http://ehcache.org/documentation/get-started/consistency-options
	 * 
	 * Attempt to resolve it setting replication as "synchronous" - failed.  
	 * 
	 * Proable directions for finding a solution:
	 *  -- BlockingCaches (involves CacheDecorator), Transactional Operations?
	 *    
	 *    
	 *  Note: If using the same configuration file in other tests, after a test crashes,
	 *  the other tests would throw exceptions related to that configuration file, 
	 *  because the store wasn't shut down properly - comment the failing test for the next run,
	 *  or use physically different configuration files per each test.  
	 */ 	
	
	/*
	@Test
	public final void testPutEntryIfAbsent() throws StoreException {

		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-testA.xml"));
		
		MapName mapNameSessions = new MapName("sessions");
		//MapName mapNameOpenID = new MapName("openid");
		
		
		Element el = new Element("Goshko", "1994");
		store.putEntry(mapNameSessions, (String) el.getKey(), (String) el.getValue(), 5, 5); //timeToLive= 5 & toIdle = 5 seconds
		
		int timeToLive = 7;
		int timeToIdle = 7;
		String values[] = {"Plovdiv", "Todor Kableshkov", "Stambolyiski", "Pazardzhik", "Septemvri",  "Ihtiman", "Elin Pelin"};
				
		for (int i=0; i<values.length; i++){
			store.putEntry(mapNameSessions, new Integer(i).toString(), values[i], timeToLive, timeToIdle);
		}
		
		boolean quiet = false;
		
		//int startSize = store.getCacheManager().getCache(mapNameSessions.toString()).getSize() ; //Number of caches initially 
		
		for (int i=0; i<values.length; i++){
            //			
			String getEntryValue = (String) store.getEntryValue(mapNameSessions, new Integer(i).toString(), quiet);
			assertEquals("If the key existed, putEntryIfAbsent must return the existing value!", getEntryValue,  values[i]);
		}
		

		for (int i=0; i<values.length; i++){            		
			String returned = (String) store.putEntryIfAbsent(mapNameSessions, new Integer(i).toString(), "Cucu", timeToLive, timeToIdle);
			assertEquals("If the key existed, putEntryIfAbsent must return the existing value!", returned, values[i]);
			
			
			String getEntryValue = (String) store.getEntryValue(mapNameSessions, new Integer(i).toString(), quiet);
			assertEquals("If the key existed, getEntryValue must return the existing value, not the one sent via putIfAbsent", getEntryValue,  values[i]);					
		}		
		
		String valuesNew[] = new String[values.length];
		for (int i=0; i<values.length; i++){
			valuesNew[i] = "New-" + values[i];
		}
		
		for (int i=0; i<valuesNew.length; i++){
			String keyNew =  new Integer(100+i).toString();
			String returned = (String) store.putEntryIfAbsent(mapNameSessions, keyNew, valuesNew[i], timeToLive, timeToIdle);			
			assertEquals("If the key didn't existed, returned value should be null and the key,value pair -- added", null,  returned);
			
			//String getEntryValue = (String) store.getEntryValue(mapNameSessions, new Integer(100+i).toString(), quiet);
			
			//Element getElement = store.getCacheManager().getCache(mapNameSessions.toString()).get(keyNew);
			Element getElement = store.getCacheManager().getCache(mapNameSessions.toString()).get(new Integer(i).toString());
			//Element getElement = store.getCacheManager().getCache(mapNameSessions.toString()).get(new Integer(100+i).toString());			
			System.out.println("Key, value: " + getElement.getKey().toString() + ", " + getElement.getValue().toString());
			
			        getElement = store.getCacheManager().getCache(mapNameSessions.toString()).get(new Integer(100+i).toString());
			//Element getElement = store.getCacheManager().getCache(mapNameSessions.toString()).get(new Integer(100+i).toString());
			 if (getElement == null) System.out.println("Key, value: NULL!");
			 else
			     System.out.println("Key, value: " + getElement.getKey().toString() + ", " + getElement.getValue().toString());						 		
			 }
		
		List<String> getKeys = store.getCacheManager().getCache(mapNameSessions.toString()).getKeys();
		 for(String s : getKeys){
			  System.out.println("Keys, PutIfAbsent::: " + s);			 
			//String getEntryValue = (String) store.getEntryValue(mapNameSessions, new Integer(100+i).toString(), quiet);
			//assertEquals("If the key didn't existed, returned value should be null and the key,value pair - added", valuesNew[i],  getEntryValue);			
		}
		 
		 // OK
		 //for (int i=0; i<valuesNew.length; i++){
		//		String keyNew =  new Integer(100+i).toString();
		//		store.putEntry(mapNameSessions, keyNew, valuesNew[i], timeToLive, timeToIdle);							
		// }		
		 
		 boolean isEternal = false;
		 for (int i=0; i<valuesNew.length; i++){
				String keyNew =  new Integer(100+i).toString();
				store.getCacheManager().getCache(mapNameSessions.toString()).putIfAbsent(new Element(keyNew, valuesNew[i], isEternal, timeToLive, timeToIdle));							
		 }
		 
		 
		 //putIfAbsent in Ehcache -- 
		 getKeys = store.getCacheManager().getCache(mapNameSessions.toString()).getKeys();
		 for(String s : getKeys){
			  System.out.println("Keys, PutEntry::: " + s);			 
			//String getEntryValue = (String) store.getEntryValue(mapNameSessions, new Integer(100+i).toString(), quiet);
			//assertEquals("If the key didn't existed, returned value should be null and the key,value pair - added", valuesNew[i],  getEntryValue);			
		}				 		
							    			
		store.shutdown();		
	}
	//*/
	
	/*
	 * getEntryValue is tested during putEntryValue and putEnryIfAbsent!
	 */
	@Test
	public final void testGetEntryValue() throws StoreException {								
		
		//fail("Not yet implemented"); // TODO
	}

	///* OK
	@Test
	public final void testRemoveEntry() throws StoreException {
		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-testA.xml"));		
		MapName mapNameSessions = new MapName("sessions");
		
		int timeToLive = 7;
		int timeToIdle = 7;
		String values[] = {"Plovdiv", "Todor Kableshkov", "Stambolyiski", "Pazardzhik", "Septemvri",  "Ihtiman", "Elin Pelin"};
				
		for (int i=0; i<values.length; i++){
			store.putEntry(mapNameSessions, new Integer(i).toString(), values[i], timeToLive, timeToIdle);
		}
		
		boolean quiet = false;
		
		int removedItems = 3;
		
		for (int i=0; i<removedItems; i++){
			store.removeEntry(mapNameSessions, new Integer(i).toString()); // values[i], timeToLive, timeToIdle);
		}
		
		List<String> getKeys = store.getCacheManager().getCache(mapNameSessions.toString()).getKeys();
		 for(String s : getKeys){
			  System.out.println("testRemoveEntry: Keys: " + s);			 				
		}
		 
		 assertEquals("After keys are removed down to the 3-rd, only 3 elements should remain!", values.length -  removedItems, getKeys.size());
		 
		 store.shutdown();
		
		//fail("Not yet implemented"); // TODO
	}

	/* OK -- Todor: refreshEntry is an extension of the basic KeyValueStore Interface
	 * In Ehcache, "put" of an already existing key, value pair refreshes it/updates the expiration and clears the statistics,
	 * but the sender should provide also the value. With "refreshEntry", only key has to be known, which might be useful, for example
	 * in situations where the value is changed independently from different locations.   
	 */	
	@Test
	public final void testRefreshEntry() throws StoreException, InterruptedException  {
		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-testA.xml"));		
		MapName mapNameSessions = new MapName("sessions");
		
		int timeToLive = 5;
		int timeToIdle = 5;
		String values[] = {"Plovdiv", "Todor Kableshkov", "Stambolyiski", "Pazardzhik", "Septemvri",  "Ihtiman", "Elin Pelin", "Rarara", "Babbaa", "Dsjfidsjfids", "SDewedfgf", "Af3fdfew", "2djkewif", "dfSDsdfsfe"};
				
		for (int i=0; i<values.length; i++){
			store.putEntry(mapNameSessions, new Integer(i).toString(), values[i], timeToLive, timeToIdle);
		}
		
		//Theremore refreshEntry in the IF!
		//Use put for it
		
  		store.refreshEntry(mapNameSessions, "0", 100); //The key is the numerical, not the Names!
		
		Thread.sleep(7000);
		//Now all but Plovdiv should be expired
		
		System.out.println("All but Plovdiv should have been expired:");
		List<String> getKeys = store.getCacheManager().getCache(mapNameSessions.toString()).getKeysWithExpiryCheck();
		for(String s : getKeys){
			  System.out.println("Key: " + s);			 				
		}
		
		assertEquals(1, getKeys.size()); //expected, actual
		
		store.shutdown();
		
	}
  // */
   
	@Test
	public final void testGetEntryCount() throws StoreException, InterruptedException {
		RMIDistributedStore store = new RMIDistributedStore();		
		store.init(new File("ehcache-test-noDiskStore.xml"));		
		MapName mapNameSessions = new MapName("sessions");
		
		int timeToLive = 5;
		int timeToIdle = 5;
		String values[] = {"Plovdiv", "Todor Kableshkov", "Stambolyiski", "Pazardzhik", "Septemvri",  "Ihtiman", "Elin Pelin", "Rarara", "Babbaa", "Dsjfidsjfids", "SDewedfgf", "Af3fdfew", "2djkewif", "dfSDsdfsfe"};
			
	  for (int j=0; j<5; j++){
		for (int i=0; i<values.length; i++){
			store.putEntry(mapNameSessions, new Integer(i+j*values.length).toString(), values[i], timeToLive+j*3, timeToIdle+j*3);
		}
	  }
	  
	  int idleFor = 3000;
	  int timePassed = 0;
	  for (int k=0; k<12; k++, timePassed+=idleFor)
	  {
		 int entryCountWithgetKeys = store.getCacheManager().getCache(mapNameSessions.toString()).getKeysWithExpiryCheck().size();
		 System.out.println("GetEntryCount: store.getEntryCount(mapNameSessions) (" + timePassed + ")"  + store.getEntryCount(mapNameSessions));
		 System.out.println("Using getKeysWithExpiryCheck == " + entryCountWithgetKeys);
		 Thread.sleep(idleFor);
		 store.removeEntry(mapNameSessions, new Integer(k).toString());		 				 
	  }		  
	}

	/*
	 * shutdown is tested implicitly during all tests!
	 */
	@Test	
	public final void testShutdown() throws StoreException {
		
	}

}
