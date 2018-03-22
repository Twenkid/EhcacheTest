/**
Ehache/Nimbus test

Version: 21/5/2012
*/

package com.nimbusds.sso.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.event.NotificationScope;

import java.io.File;
import java.util.HashSet; //Thread Safeness? With synchronized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Date;
import java.util.Set;


import org.apache.log4j.Logger;


public class RMIDistributedStore implements KeyValueStore {

	/**
	 * CacheManager instance - holds the caches (named key/value maps)
	 */
	private net.sf.ehcache.CacheManager cacheManager;
    
	/*
	 * Native sso.store EventListeners are registered here,
	 * and mapped into native Ehcache listeners, extending
	 * CacheEventListenerAdapter from the Ehcache hiearchy. 
	 */	
	private EntryEventDispatcher eventDispatcher;
	
	
	/** 
	 * Default configuration file
	 */	
	final String DEFAULT_CONFIG_FILE = "ehcache.xml";
	/**
	 * The log.
	 */
	private final Logger log = Logger.getLogger(RMIDistributedStore.class);
	
	
	/**
	 * Initialises the key/value store with the specified configuration file. 
	 * The store becomes usable after that, if the configuration is not empty and has defined caches
	 * which are to be created (this is a usual case).
	 * NB, TBC: For distributed caches, it's desirable all nodes to have the same configuration.
	 * <p>For replicated or distributed stores, this method can be used to
	 * initialise the local node and establish connections to the remote 
	 * ones.
	 *	 
	 * @param config The configuration file (e.g. ehcache.xml).
	 * If null, the default ehcache.xml in the class path will be used.
	 * If the default configuration can't be found, an exception is thrown.
	 * @throws StoreException If the key/value store couldn't be initialised
	 *                        or if a general store exception was 
	 *                        encountered.
	 */
    public synchronized void init(final File configFile)  throws StoreException {
    	try{    		
    		if (configFile == null) cacheManager = new CacheManager(DEFAULT_CONFIG_FILE);
    		else
			    cacheManager = new CacheManager(configFile.getAbsolutePath());						
		}
		catch (CacheException e){
			 System.out.println("StoreException/CacheException when initializing from a file!");
			 if (configFile == null) log.error("StoreException/CacheException when initializing from the default config file(" + DEFAULT_CONFIG_FILE +" )!");
			 else 
				  log.error("StoreException/CacheException when initializing from file: " + configFile.getAbsolutePath() + "!");			 
			 throw new StoreException("StoreException/CacheException from a file! " + configFile.getAbsolutePath());			  
		}
    	    	
    	eventDispatcher = new EntryEventDispatcher(cacheManager);
    	if (eventDispatcher == null) throw new StoreException("Creation of eventDispatcher failed!");
    	    	
    }

	/**
	 * Adds a listener to receive {@link EntryExpiredEvent}s. Listeners may 
	 * be called in any particular order.
	 *
	 * Todor: See the notes about the implementation: {@link entryExpiredEventListeners}
	 * @param listener The entry expired event listener to add. Must not be
	 *                 {@code null}.
	 *
	 * @throws StoreException If the listener couldn't be added of if a 
	 *                        general store exception was encountered.
	 *  
	 *                        
	 */		
	public synchronized void addEntryEventListener(final MapName mapName, EntryEventListener listener)
			throws StoreException {
		if (mapName == null)  throw new NullPointerException("mapName must not be null!");
		if (listener == null) throw new NullPointerException("Listener must not be null!");
			
		eventDispatcher.addEntryEventListener(mapName, listener);				
	}
	
	
	/**
	 * Gets the registered entry event listeners for the specified map.
	 *
	 * @param mapName The map name. Must not be {@code null}.
	 *
	 * @return The entry event listeners (read-only set), empty set if none.
	 *
	 * @throws StoreException If the listeners couldn't be retrieved or if a
	 *                        general store exception exception was 
	 *                        encountered.
	 */
	public Set<EntryEventListener> getEntryEventListeners(final MapName mapName)
		throws StoreException {
		if (mapName == null)  throw new NullPointerException("mapName must not be null!");
	
		return eventDispatcher.getEntryEventListeners(mapName);
   }
	
	/**
	 * Removes an entry event listener for the specified map.
	 *
	 * @param mapName  The map name. Must not be {@code null}.
	 * @param listener The entry event listener to remove.
	 *
	 * @return {@code true} if the specified event listener existed and was
	 *         successfully removed, else {@code false}.
	 *
	 * @throws StoreException If the listener couldn't be removed or if a 
	 *                        general store exception was encountered.
	 */		
	public boolean removeEntryEventListener(MapName mapName,
			EntryEventListener listener) throws StoreException {
		
		return eventDispatcher.removeEntryEventListener(mapName, listener);		
	}
	
	
    /**
     * Create a map for storing sessions. Note that for replicated / distributed
	 * stores the map may already have been created by another node.
	 * TODO: Distributed Creation of Caches and sending the entire content?
	 * Check CacheManagerEventListener etc.!!!
	 * store.createMapIfAbsent("sessions");
	 * 
	 * Check: bootstrap
	 */
	public synchronized void createMapIfAbsent(final MapName mapName) throws StoreException {
		// TODO Auto-generated method stub
		try{    		
			if (mapName == null) throw new NullPointerException("Stores must have non-null names!");
			else
			    cacheManager.addCacheIfAbsent(mapName.toString());    							
		}
		catch (ObjectExistsException e){			
			 //This is not an error!
			throw new StoreException("A cache with that name already exists!");
		}
		catch (IllegalAccessError e){						 
			throw new StoreException("IllegalAccessError!");
		}
	}
	
	/**
	 * Returns the names of all maps.
	 *
	 * @return The map names, empty set if none.
	 *
	 * @throws StoreException If the map names couldn't be provided or if a 
	 *                        general store exception was encountered.
	 */
	public synchronized Set<MapName> getMapNames() throws StoreException {
				
		String[] cacheNamesString = cacheManager.getCacheNames();
  			
		HashSet<MapName> cacheNamesSet = new HashSet<MapName>(); //not thread safe? ... new ConcurrentHashMap(); /
		for(String name : cacheNamesString){
		    cacheNamesSet.add(new MapName(name));
		}
				
		return cacheNamesSet;
	}
	
	
	/**
	 * Deletes an existing key/value map with the specified name.
	 *
	 * @param mapName The map name. Must not be {@code null}.
	 *
	 * @throws StoreException If the map name is invalid, if the map 
	 *                        couldn't be deleted or if a general store
	 *                        exception was encountered.
	 */
	public synchronized void deleteMap(final MapName mapName) throws StoreException {
		
		try{
			if (mapName == null) throw new NullPointerException("Maps must have non-null names!");
			else
			    cacheManager.removeCache(mapName.toString());
			
			log.info("Deletion of " + mapName.toString() + " OK!");
		}
		catch (CacheException e){
			 //TODO: Use LOGGER!
			 log.error("Deletion of: " + mapName.toString() + " FAILED!");
			 throw new StoreException("StoreException - Deletion of " + mapName.toString() + " FAILED!");			 
			 }		
	}
	
	/**
	 * Puts a new key/value entry into the specified map. If the key exists
	 * the value and the previous expiration time will be overwritten.
	 *	
	 * <b>Todor's Remark:</b> Notice that Ehcache's <b>Element</b> has more powerful parameters set for fine grained configuration:
	 * element = new Element(key,  value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan, timeToLive, timeToIdle, lastUpdateTime)
	 * 
	 * @see com.nimbusds.sso.store.KeyValueStore#putEntry(java.lang.String, java.lang.String, java.lang.Object, java.util.Date)
	 * 
	 * @param mapName The map name. Must not be {@code null}.
	 * @param key     The entry key. Must not be {@code null}.
	 * @param value   The entry value. May be {@code null}.
	 * @param expirationPeriod     The entry expiration time. If {@code null} the entry
	 *                shall never expire.
	 *
	 * @throws StoreException If the map name is invalid, if the entry 
	 *                        couldn't be put, or if a general store 
	 *                        exception was encountered.
	 */
	public synchronized void putEntry(final MapName mapName, String key, Object value,  final int timeToIdle, final int timeToLive) throws StoreException { //int expirationPeriod)										
		   String errorMsg;
		try{
			Element element;			
			boolean neverExpire = (timeToIdle == 0) || (timeToLive == 0);
		
			//expirationPeriod sets both Echache Element parametrs: timeToIdle (between two accesses), timeToLive (from creation to deletion)
			element = new Element(key, value, neverExpire, timeToIdle, timeToLive); 
			cacheManager.getCache(mapName.toString()).put(element);
		}
		catch (IllegalStateException e){ //Thrown by cacheManager.getCache
			errorMsg = "StoreException - IllegalStateException on putting an entry (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);
			throw new StoreException(errorMsg);
			
		}
		catch (ClassCastException e){
			errorMsg = "StoreException - ClassCastException during  putting an entry (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);
			throw new StoreException(errorMsg);
			
		}		
		catch (IllegalArgumentException e){
			errorMsg = "StoreException - IllegalArgumentException during putting an entry (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);
			throw new StoreException(errorMsg);
		}
        catch (CacheException e){
        	errorMsg = "StoreException - CacheException during putting an entry (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);
			throw new StoreException(errorMsg);
		}
	}
	
	/**
	 * Extension by Todor: expirationPeriod
	 * 
	 * If an entry with the specified key doesn't exist, puts a new one. The
	 * action should be performed atomically (internally).
	 * 
	 * @param mapName The map name. Must not be {@code null}.
	 * @param key     The entry key. Must not be {@code null}.
	 * @param value   The entry value. May be {@code null}.
	 * @param exp     The entry expiration period in seconds after creation.
	 *                If {@code null} the entry shall never expire.
	 *
	 * @return If the entry key existed the associated value, else 
	 *         {@code null}. Note that entry values may be {@code null}.
	 *          <b>Todor's WARNING</b> - I don't think that's a good decision, NULL is returned either
	 *  if the element didn't existed and when it existed but had value of "null".
	 *
	 * @throws StoreException If the map name is invalid, if the entry 
	 *                        couldn't be put, or if a general store 
	 *                        exception was encountered.
	 */
	public synchronized Object putEntryIfAbsent(final MapName mapName, String key, Object value,
			final int timeToIdle, final int timeToLive) throws StoreException {
				   		    
		    String errorMsg;
		    Element prevElement = null;
		    
		try{					
			boolean neverExpire = (timeToIdle == 0) || (timeToLive == 0);
			
			//The "expirationPeriod" parameter is used to set both Echache's Element parametrs:
			//timeToIdle (between two accesses) and timeToLive (from creation to deletion(eviction)			
			Element element = new Element(key, value, neverExpire, timeToIdle, timeToLive);
			
			//30/5/2012: Sneak directly into the cache to get value, instead of getting the prevElement value		
			prevElement = cacheManager.getCache(mapName.toString()).get(key); // putIfAbsent(element);									
			//prevElement = cacheManager.getCache(mapName.toString()).putIfAbsent(element);				
			
			cacheManager.getCache(mapName.toString()).putIfAbsent(element);
		}
		
		catch (IllegalStateException e){ //Thrown by cacheManager.getCache
			errorMsg = "StoreException - IllegalStateException on putting an entry if absent (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);
			throw new StoreException(errorMsg);
			
		}
		catch (ClassCastException e){
			errorMsg = "StoreException - ClassCastException during  putting an entry  if absent (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);
			throw new StoreException(errorMsg);
			
		}		
		catch (IllegalArgumentException e){
			errorMsg = "StoreException - IllegalArgumentException during putting an entry if absent (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);			
		}
        catch (CacheException e){
        	errorMsg = "StoreException - CacheException during putting an entry if absent (" + key + ", " + value.toString() + ") " + "in " + mapName.toString();
			log.error(errorMsg);			
		}
		
		if (prevElement == null) return null; 
		   else return prevElement.getValue();
				
	}

	/**
	 * Gets the entry value associated with the specified key.
	 *
	 * @param mapName The map name. Must not be {@code null}.
	 * @param key     The entry key. Must not be {@code null}.
	 *
	 * @return The associated entry value, {@code null} if the key is
	 *         invalid or the entry has expired.
	 *
	 * @throws StoreException If the map name is invalid, if the entry
	 *                        value couldn't be retrieved, or of a general
	 *                        store exception was encountered.
	 */
	public synchronized  Object getEntryValue(final MapName mapName, String key,  final boolean quiet)
			throws StoreException {
		
		if (mapName == null)
			throw new NullPointerException("The map name must not be null!");
		if (key == null)
			throw new NullPointerException("The key must not be null!");
		
		Element element = null;
		Cache cache = null;
		String errorMsg;
		
		//element = cacheManager.getCache(mapName).get(key);
		try{
		   cache = cacheManager.getCache(mapName.toString());
		}
		catch (IllegalStateException e){ //Thrown by cacheManager.getCache
			errorMsg = "StoreException - IllegalStateException when accessing cache (getCache)" + mapName.toString() + " with a key " + key;
			log.error(errorMsg);
			throw new StoreException(errorMsg);
			
		}
		catch (ClassCastException e){
			errorMsg = "StoreException - ClassCastException when accessing cache (getCache)" + mapName.toString() + " with a key " + key;		
			log.error(errorMsg);
			throw new StoreException(errorMsg);			
		}		
				
		try {
			if (quiet) element = cache.getQuiet(key); //Don't update statistics and refresh the idle expiration timestamp of the accessed element 
			else
			    element = cache.get(key);
		}
		catch (CacheException e){ //IllegalStateException, ClassCastException,
			errorMsg = "StoreException - CacheException when accessing cache (getCache)" + mapName.toString() + " with a key " + key;		
			log.error(errorMsg);
			throw new StoreException(errorMsg);			
		}
		
		if (element == null) return null;
		else
		    return element.getObjectValue();
	}

	/**
	 * Removes an entry with the specified key.
	 *
	 * @param mapName The map name. Must not be {@code null}.
	 * @param key     The entry key. Must not be {@code null}.
	 *
	 * @return The associated entry value, {@code null} if the key is
	 *         invalid or the entry has expired.
	 *
	 * @throws StoreException If the map name is invalid, if the entry
	 *                        couldn't be removed, of if a general store
	 *                        exception was encountered.
	 */
	public synchronized Object removeEntry(final MapName mapName, String key) throws StoreException {
		
		if (mapName == null)
			throw new NullPointerException("The map name must not be null!");
		if (key == null)
			throw new NullPointerException("The key must not be null!");
		
		Element element = null;
		Cache cache = null;
		String errorMsg;
		
		//element = cacheManager.getCache(mapName).get(key);
		try{
			cache = cacheManager.getCache(mapName.toString());
		}
		catch (IllegalStateException e){ //Thrown by cacheManager.getCache
			errorMsg = "StoreException - IllegalStateException when attempting to remove (" + key + ") " + "from " + mapName.toString() + " during getting the cache";
			log.error(errorMsg);
			throw new StoreException(errorMsg);
		}
		catch (ClassCastException e){
			errorMsg = "StoreException - ClassCastException when attempting to remove (" + key + ") " + "from " + mapName.toString() + " during getting the cache";			
			log.error(errorMsg);
			throw new StoreException(errorMsg);			
		}
		
		try{
		   element = cache.getQuiet(key); //getQuiet reads without updating statistics/notifying the listeners, because that's system access
		}		
		catch(CacheException e){
			errorMsg = "StoreException - CacheException when attempting to remove (" + key + ") " + "from " + mapName.toString() + " during getting the old value";			
			log.error(errorMsg);
			throw new StoreException(errorMsg);			  	
		}
		
		//Todor's NB: Double check .isExpired() requirements and behavior to be implemented 
		if ((element == null) || (element.isExpired())){			
		   return null;
		}
		  else		
		  {
			 try{
		         cache.remove(key);
		         return element.getValue();
			 }
			 catch (IllegalStateException e) {
				 errorMsg = "StoreException - IllegalStateException when attempting to remove (" + key + ") " + "from " + mapName.toString() + " during getting the old value";			
				 log.error(errorMsg);
				 throw new StoreException(errorMsg);	
			}						 			
		  }
		
		/* Todor's NB: Check Replication, CacheEventListener, CacheManagerEvenetListener etc.!!!
		 * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
		 *  This exception should be caught in those circumstances. 
		 */				
				
	}

	/**
	 * <b>Extension by Todor: expirationPeriod in seconds, from the moment the function is called,
	 *  instead of an absolute Date.</b>
	 * <p>
	 * <b>Todor's question: What if the element happens to has expired at the moment when checked for refreshment?</b>
	 * <p>Should it be recreated (replaced), or left, because it was not already valid?
	 * <p>It's not yet clear how the cache will behave if an expired element is refreshed, without being replaced, or
	 * will it be accessible at all (if expired, but not yet evicted from the Ehcache store).</b> 
	 * <p>
	 * Updates the expiration time of the specified entry.
	 *
	 * @param mapName The map name. Must not be {@code null}.
	 * @param key     The entry key. Must not be {@code null}.
	 * @param newExp  The new expiration time. If {@code null} the entry
	 *                shall never expire.
	 *
	 * @return The associated entry value, {@code null} if the key is
	 *         invalid or the entry has expired.
	 *
	 * @throws StoreException If the map name is invalid, if the entry
	 *                        couldn't be refreshed, of if a general store
	 *                        exception was encountered.
	 */
	public synchronized Object refreshEntry(final MapName mapName, String key, final int newExpirationPeriod)
			throws StoreException {

		if (mapName == null)
			throw new NullPointerException("The map name must not be null!");
		if (key == null)
			throw new NullPointerException("The key must not be null!");
		
		Element element;
		Element prevElement;
		
		boolean neverExpire = (newExpirationPeriod == 0);
	
		//newExpirationPeriod sets both Echache Element parametrs: timeToIdle (between two accesses), timeToLive (from creation to deletion)
		//element = new Element(key, value, neverExpire, newExpirationPeriod, newExpirationPeriod); 
		
		//Element element = null;
		Cache cache = null;
		
		String errorMsg;		
		String errCacheManager = "when attempting to refresh expiration time of (" + key + ") " + "from " + mapName.toString() + " during getting the cache";
		String errGetQuiet = "when attempting to refresh expiration time of (" + key + ") " + "from " + mapName.toString() + " during Cache.getQuiet()";
		String errRefresh = " when attempting to refresh (" + key + ") " + "from " + mapName.toString() + " during Element.setEternal(), Element.seTimeToLive() or Element.seTimeToIdle() to " + newExpirationPeriod;
		//element = cacheManager.getCache(mapName).get(key);
		try{
			cache = cacheManager.getCache(mapName.toString());
		}
		catch (IllegalStateException e){ //Thrown by cacheManager.getCache
			errorMsg = "StoreException - IllegalStateException " + errCacheManager;
			log.error(errorMsg);
			throw new StoreException(errorMsg);
		}
		catch (ClassCastException e){
			errorMsg = "StoreException - ClassCastException " + errCacheManager;			
			log.error(errorMsg);
			throw new StoreException(errorMsg);			
		}
		
		try{
		   prevElement = cache.getQuiet(key); //getQuiet reads without updating statistics/notifying the listeners, because that's system access
		   /**
		     * Replace the cached element only if an Element is currently cached for this key
		     * @param element Element to be cached
		     * @return the Element previously cached for this key, or null if no Element was cached
		     * @throws NullPointerException if the Element is null or has a null key
		     */
		    //Element replace(Element element) throws NullPointerException;
			if ((prevElement == null) || (prevElement.isExpired())){			
				   return null;
				}
				  else		
				  {
					 try {
						  prevElement.setEternal(neverExpire);
						  prevElement.setTimeToLive(newExpirationPeriod);
						  prevElement.setTimeToIdle(newExpirationPeriod);	
						  //Ne, 30/5/2012 --> should use put to update it? cache.put(prevElement);
						  cache.put(prevElement);
						  
				          return prevElement.getValue();
					 }
					 catch (IllegalArgumentException e) {
						 errorMsg = "StoreException - IllegalStateException " + errRefresh;			
						 log.error(errorMsg);
						 throw new StoreException(errorMsg);	
					}						 			
				  }					   
		    		    
		    //prevElement = cache.replace(element);		   		    
		}		
		catch(CacheException e){
			errorMsg = "StoreException - CacheException " + errGetQuiet;	
			log.error(errorMsg);
			throw new StoreException(errorMsg);			  	
		}
		catch(NullPointerException e){
			errorMsg = "StoreException - NullPointerException " + errGetQuiet;	
			log.error(errorMsg);
			throw new StoreException(errorMsg);			  	
		}
		
	}
	
	/**
	 * Gets the count of all entries in the specified map, stored in both
	 * RAM and on disk. The number is approximate, because it includes the expired elements.
	 * which are not yet removed (evicted) from the underlying storage model. The storage model
	 * alone may involve eviction from RAM to disk on overflow, and then deletion from the disk,
	 * if a limit is set for the DiskStore (during configuration or programmatically).
	 *    
	 * <b>Notice that counting is an expensive operation if the disk is also used for storage.</b>
	 * 
	 * See: @see <a href="http://ehcache.org/apidocs/net/sf/ehcache/Ehcache.html#getSize()">Ehcache.html#getSize()</a>
	 * for a discussion and more possibilities.
	 * @param mapName The map name. Must not be {@code null}.
	 *
	 * @return The total entry count, 0 if none, -1 if the operation is not
	 *         supported by the underlying storage.
	 *
	 * @throws StoreException If the map name is invalid, if entry counting 
	 *                        failed or if a general store exception was
	 *                        encountered.
	 */	
	public synchronized int getEntryCount(final MapName mapName) throws StoreException {
		if (mapName == null)
			throw new NullPointerException("The map name must not be null");
		
		if (!cacheManager.cacheExists(mapName.toString()))
		{
			String error = "A map called " + mapName.toString() + " doesn't exist in the store!"; 
			log.error(error);
			throw new StoreException(error);			
		}
		
		try{
		return cacheManager.getCache(mapName.toString()).getSize();
		}catch (CacheException e) {			
			String error = "CacheException during getEntryCount: cacheManager.getCache(mapName.toString()).getSize();"; 
			log.error(error);
			throw new StoreException(error);
		}
	}

	/**
	 * Shuts down the key/value store. This method is intended to allow the
	 * underlying data store to perform necessary cleanup (if required).
	 *
	 * <p>For replicated or distributed stores, this method can be used to
	 * shut down the local node.
	 *
	 * @throws StoreException If the store couldn't be shut down or if a 
	 *                        general store exception was encountered.
	 */
	public synchronized  void shutdown() throws StoreException {
       
		if (cacheManager != null) {
    	   cacheManager.shutdown();
    	   log.info("KeyValueStore shutdown: OK!");
       }
       else {
    	   log.error("KeyValueStore shutdown: store wasn't initialized, it can't be shutdown.");
    	   throw new StoreException("KeyValueStore shutdown: store wasn't initialized, it can't be shutdown.");
       }
	}
	

	public synchronized CacheManager getCacheManager() throws StoreException {		
		return cacheManager;
	}

}
