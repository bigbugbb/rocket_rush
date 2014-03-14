package com.localytics.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import com.localytics.android.LocalyticsProvider.AmpConditionValuesDbColumns;
import com.localytics.android.LocalyticsProvider.AmpConditionsDbColumns;
import com.localytics.android.LocalyticsProvider.AmpDisplayedDbColumns;
import com.localytics.android.LocalyticsProvider.AmpRuleEventDbColumns;
import com.localytics.android.LocalyticsProvider.AmpRulesDbColumns;
import com.localytics.android.LocalyticsProvider.AttributesDbColumns;

/**
 * Helper class to handle amp session-related work on the {@link LocalyticsAmpSession#sSessionHandlerThread}.
 */
/* package */class AmpSessionHandler extends SessionHandler implements AmpDialogFragment.AmpDialogCallback
{		
	/**
	 * The fragment manager bound for showing the amp dialog
	 */
	private FragmentManager mFragmentManager;
	
	/**
     * Selection for {@link #getAmpMessageMap()}.
     */
    private static final String SELECTION_AMP_RULES = String.format("%s > ?", AmpRulesDbColumns.EXPIRATION); //$NON-NLS-1$
	
    /**
     * Selection for {@link #getAmpMessageMap()}.
     */
    private static final String SELECTION_AMP_RULEEVENTS = String.format("%s = ?", AmpRuleEventDbColumns.EVENT_NAME); //$NON-NLS-1$
    
	/**
     * Sort order for the amp rules.
     * <p>
     * This is a workaround for Android bug 3707 <http://code.google.com/p/android/issues/detail?id=3707>.
     */
    private static final String AMP_RULES_SORT_ORDER = String.format("CAST(%s AS TEXT)", AmpRulesDbColumns._ID); //$NON-NLS-1$

    /**
     * Sort order for the amp rule events.
     * <p>
     * This is a workaround for Android bug 3707 <http://code.google.com/p/android/issues/detail?id=3707>.
     */
    private static final String AMP_RULEEVENTS_SORT_ORDER = String.format("CAST(%s as TEXT)", AmpRuleEventDbColumns.RULE_ID_REF); //$NON-NLS-1$
    
    /**
     * Projection for {@link #open(boolean, Map)}.
     */
    private static final String[] JOINER_ARG_AMP_RULES_COLUMNS = new String[] { AmpRulesDbColumns._ID };
    
    /**
     * Projection for {@link #open(boolean, Map)}.
     */
    private static final String[] PROJECTION_AMP_RULEEVENTS = new String[] { AmpRuleEventDbColumns.RULE_ID_REF };
    
    /**
     * Constructs a new Handler that runs on the given looper for AmpSession.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining
     *            references to {@code Activity} instances. Cannot be null.
     * @param key The key unique for each application generated at www.localytics.com. Cannot be null or empty.
     * @param looper to run the Handler on. Cannot be null.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if {@code key} is null or empty
     */
	public AmpSessionHandler(final Context context, final String key, final Looper looper) 
	{
		super(context, key, looper);				
		
		// Update the thread name displayed in DDMS
		sUploadHandlerThread.setName(AmpUploadHandler.class.getSimpleName());
	}
	
	/**
	 * 	 
	 * 
	 * @param fragmentManager The fragment manager object
	 */
	public void setFragmentManager(final FragmentManager fragmentManager)
	{
		mFragmentManager = fragmentManager;
	}
	
    /**
     * Gets a new Handler that runs on {@code looper}.
     * <p>
     * Note: This constructor may perform disk access.
     *
     * @param context Application context. Cannot be null.
     * @param sessionHandler Parent {@link SessionHandler} object to notify when uploads are completed. Cannot be null.
     * @param apiKey Localytics API key. Cannot be null.
     * @param installId Localytics install ID.
     * @param looper to run the Handler on. Cannot be null.
     */
	@Override
    protected UploadHandler createUploadHandler(final Context context, final Handler sessionHandler, final String apiKey, final String installId, final Looper looper)
    {
    	return new AmpUploadHandler(context, this, apiKey, getInstallationId(mProvider, apiKey), sUploadHandlerThread.getLooper());
    }
	
	@Override
    public void handleMessage(final Message msg)
    {
        try
        {
        	// Do this check otherwise the super class may throw exception
        	if (msg.what != MESSAGE_TRIGGER_AMP) 
        	{
        		super.handleMessage(msg);
        	}

            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
            }

            switch (msg.what)
            {               
                case MESSAGE_TRIGGER_AMP:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Handler received MESSAGE_TRIGGER_AMP"); //$NON-NLS-1$
                    }

                    final Object[] params = (Object[]) msg.obj;

                    final String event = (String) params[0];
                    @SuppressWarnings("unchecked")
					final Map<String, String> attributes = (Map<String, String>) params[1];
                    AmpSessionHandler.this.triggerAmp(event, attributes);

                    break;
                }
                case MESSAGE_TAG_EVENT:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Amp Session Handler received MESSAGE_TAG_EVENT"); //$NON-NLS-1$
                    }

                    final Object[] params = (Object[]) msg.obj;

                    final String event = (String) params[0];
                    @SuppressWarnings("unchecked")
                    final Map<String, String> attributes = (Map<String, String>) params[1];                  

                    /**
                     * This message should be already handled by SessionHandler, extra work here is
                     * to trigger the amp.
                     */
                    AmpSessionHandler.this.triggerAmp(event, attributes);

                    break;
                }
            }
        }
        catch (final Exception e)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", e); //$NON-NLS-1$
            }

            if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED)
            {
                throw new RuntimeException(e);
            }
        }
    }
	
	/**
     * Trigger the campaign for the given amp event.
     * 
     * <p>
     * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
     * public interface is to send {@link #MESSAGE_TRIGGER_AMP} to the Handler.
     *
     * @param eventName The name of the amp event which occurred. Cannot be null.
     * @see #MESSAGE_TRIGGER_AMP
     */
    /* package */void triggerAmp(final String eventName)
    {
    	triggerAmp(eventName, null);
    }
    
    /**
     * Trigger the campaign for the given amp event.
     * <p>
     * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
     * public interface is to send {@link #MESSAGE_TRIGGER_AMP} to the Handler.
     * 
     * @param eventName The name of the amp event which occurred. Cannot be null.
     * @param attributes The collection of attributes for this particular event. May be null.
     * @see #MESSAGE_TRIGGER_AMP
     */
    /* package */void triggerAmp(final String eventName, final Map<String, String> attributes)
    {
    	// Get all amp messages associated with the input event
		Vector<Map<String, Object>> ampMessages = getAmpMessageMaps(eventName);
		if (ampMessages.size() == 0) {
			if (eventName.startsWith(mContext.getPackageName())) 
			{			
				final String eventString = eventName.substring(mContext.getPackageName().length() + 1, eventName.length());
				ampMessages = getAmpMessageMaps(eventString);
			}
		}
		
		// Retrieve the suitable amp message for displaying the amp dialog
		final Map<String, Object> ampMessage = retrieveDisplayingCandidate(ampMessages, attributes);
		
		if (null == ampMessage)
		{
			return;
		}
		
		new Handler(Looper.getMainLooper()).post(new Runnable() 
		{
			public void run() 
			{
				// We attach/detach/check fragment manager in the same thread.
				if (null == mFragmentManager) 
				{
					return;
				}
				
				try 
				{
					if (mFragmentManager.findFragmentByTag(AmpDialogFragment.DIALOG_TAG) != null) 
					{					
						return;
					}
					final AmpDialogFragment fragment = AmpDialogFragment.newInstance();
					fragment.setData(ampMessage)
							.setOnAmpDestroyListener(AmpSessionHandler.this)
							.setJavaScriptAPI(new JavaScriptClient(mContext, AmpSessionHandler.this, mProvider, fragment))
							.show(mFragmentManager, AmpDialogFragment.DIALOG_TAG);
					
					/* 
					 * Synchronous call is necessary here otherwise the check from the findFragmentByTag will fail						 
					 * which leads to the creation of multiple dialog fragments having the same tag.				
					 */	
					mFragmentManager.executePendingTransactions();
				}
				catch (final Exception e)
				{
					if (Constants.IS_LOGGABLE)
		            {
		                Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", e); //$NON-NLS-1$
		            }

		            if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED)
		            {
		                throw new RuntimeException(e);
		            }
				}			
			}
		});	
    }     
    
    /**
     * As a callback, this method will be called when the amp is being destroyed.
     * 
     * @param ampMessage The amp message from which to trigger the amp.
     */
	public void onAmpDestroy(final Map<String, Object> ampMessage)
	{
		try {
	    	// Get the rule id and campaign id to querying the database
	    	final int campaignId = (Integer) ampMessage.get(AmpRulesDbColumns.CAMPAIGN_ID);
	    	final int ruleId = getRuleIdFromCampaignId(campaignId);
	    	
	    	mProvider.runBatchTransaction(new Runnable()
	        {
	            public void run()
	            {
	            	// Set the displayed state of this campaign to prevent it from being displayed twice.
	            	final ContentValues values = new ContentValues();
	                values.put(AmpDisplayedDbColumns.DISPLAYED, 1);
	                values.put(AmpDisplayedDbColumns.CAMPAIGN_ID, campaignId);
	                mProvider.insert(AmpDisplayedDbColumns.TABLE_NAME, values); //$NON-NLS-1$
	                
	                /**
	                 * Clear the database after the campaign has been displayed.
	                 */
	                // First delete the associated amp conditions and amp condition values        
	         		final long[] conditionIds = getConditionIdFromRuleId(ruleId);
	         		for (long conditionId : conditionIds)
	         		{
	         			mProvider.delete(AmpConditionValuesDbColumns.TABLE_NAME, String.format("%s = ?", AmpConditionValuesDbColumns.CONDITION_ID_REF), new String[] { Long.toString(conditionId) }); //$NON-NLS-1$			
	         		}
	         		mProvider.delete(AmpConditionsDbColumns.TABLE_NAME, String.format("%s = ?", AmpConditionsDbColumns.RULE_ID_REF), new String[] { Integer.toString(ruleId) }); //$NON-NLS-1$
	         		
	         		// Then delete the binding between the event and this rule
	         		mProvider.delete(AmpRuleEventDbColumns.TABLE_NAME, String.format("%s = ?", AmpRuleEventDbColumns.RULE_ID_REF), new String[] { Integer.toString(ruleId) }); //$NON-NLS-1$
	         		
	         		// Last delete the amp rule itself
	         		mProvider.delete(AmpRulesDbColumns.TABLE_NAME, String.format("%s = ?", AmpRulesDbColumns._ID), new String[] { Integer.toString(ruleId) }); //$NON-NLS-1$
	            }
	        });
	    	
	    	// Delete the decompressed file if it does exist
	    	final String basepath = (String) ampMessage.get(AmpConstants.KEY_BASE_PATH);
	    	if (null != basepath) 
	    	{
		    	File dir = new File(basepath);
		    	if (dir.isDirectory()) 
		    	{
		            for (String childen : dir.list()) 
		            {
		                new File(dir, childen).delete();
		            }
		        }
		    	
		    	if (!dir.delete()) 
		    	{
		    		if (Constants.IS_LOGGABLE)
	                {
	                    Log.w(Constants.LOG_TAG, String.format("Delete %s failed.", basepath)); //$NON-NLS-1$
	                }
		    	}
		    	
		    	File zip = new File(basepath + ".zip");
		    	if (!zip.delete())
		    	{
		    		if (Constants.IS_LOGGABLE)
	                {
	                    Log.w(Constants.LOG_TAG, String.format("Delete %s failed.", basepath + ".zip")); //$NON-NLS-1$
	                }
		    	}
	    	}
		}
    	catch (final Exception e)
		{
			if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", e); //$NON-NLS-1$
            }

            if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED)
            {
                throw new RuntimeException(e);
            }
		}
	}
	
	/**
     * As a callback, this method will be called when the amp action event should be tagged.
     * 
     * @param ampMessage The amp message from which to trigger the amp.
     */
	public void onTagAmpActionEvent(final String event, final Map<String, String> attributes) 
	{
        final String eventString = String.format(Constants.EVENT_FORMAT, mContext.getPackageName(), event);
        
        /*
         * Convert the attributes into the internal representation of packagename:key
         */
        final TreeMap<String, String> remappedAttributes = new TreeMap<String, String>();

        if (null != attributes)
        {
            final String packageName = mContext.getPackageName();
            for (final Entry<String, String> entry : attributes.entrySet())
            {
                remappedAttributes.put(String.format(AttributesDbColumns.ATTRIBUTE_FORMAT, packageName, entry.getKey()), (String) entry.getValue());
            }
        }

        sendMessage(obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Object[] { eventString, new TreeMap<String, String>(remappedAttributes), null }));
	}
	
    /**
     * This method check whether the amp conditions are satisfied by the input attributes.
     * 
     * @param ampMessage
     * @param attributes
     * @return
     */
    private boolean isAmpMessageSatisfiedConditions(final Map<String, Object> ampMessage, final Map<String, String> attributes)
    {
    	boolean satisfied = true;    	
    	    	 
    	// Get the rule id and campaign id to querying the database
    	final int campaignId = (Integer) ampMessage.get(AmpRulesDbColumns.CAMPAIGN_ID);
    	final int ruleId = getRuleIdFromCampaignId(campaignId);
    	
        // Get all the amp conditions associated with the rule id.    	
    	final Vector<AmpCondition> ampConditions = getAmpConditions(ruleId);
    	
    	// If all amp conditions are satisfied by the attributes, then the amp message is satisfied.
    	for (final AmpCondition condition : ampConditions)
    	{
    		if (!condition.isSatisfiedByAttributes(attributes))
    		{
    			satisfied = false;
    			break;
    		}
    	}
    	
    	return satisfied;
    }   
    
    /**
     * Retrieve the whole amp conditions from the database by the amp message 
     * @param ampMessage
     * @return
     */
    private Vector<AmpCondition> getAmpConditions(final int ruleId)
    {
    	Vector<AmpCondition> ampConditions = null;    	
    	
    	// Get amp condition from rule id.
    	Cursor cursor = null;
        try
        {        	        	
        	cursor = mProvider.query(AmpConditionsDbColumns.TABLE_NAME, null, String.format("%s = ?", AmpConditionsDbColumns.RULE_ID_REF), new String[] { Integer.toString(ruleId) }, null); //$NON-NLS-1$
            while (cursor.moveToNext())
            {
            	final int conditionId = cursor.getInt(cursor.getColumnIndexOrThrow(AmpConditionsDbColumns._ID));
            	final String name     = cursor.getString(cursor.getColumnIndexOrThrow(AmpConditionsDbColumns.ATTRIBUTE_NAME));
            	final String operator = cursor.getString(cursor.getColumnIndexOrThrow(AmpConditionsDbColumns.OPERATOR));
            	final Vector<String> values = getAmpConditionValues(conditionId);
            	
        		if (null == ampConditions)
        		{
        			ampConditions = new Vector<AmpCondition>();            			
        		}
        		
        		ampConditions.add(new AmpCondition(name, operator, values));
            }
        }
        finally
        {
            if (null != cursor)
            {
            	cursor.close();
            	cursor = null;
            }            
        }
        
        return ampConditions;
    }
    
    /**
     * Retrieve the condition values from the database by the condition id.
     * 
     * @param conditionId The id of the AmpConditionsDbColumns.
     * @return The values associated with the condition id as a vector of string. 
     */
    private final Vector<String> getAmpConditionValues(final int conditionId)
    {
    	Vector<String> values = null;
    	
    	Cursor cursor = null;
        try
        {        	        	
        	cursor = mProvider.query(AmpConditionValuesDbColumns.TABLE_NAME, null, String.format("%s = ?", AmpConditionValuesDbColumns.CONDITION_ID_REF), new String[] { Integer.toString(conditionId) }, null); //$NON-NLS-1$
        	while (cursor.moveToNext())
            {
            	final String value = cursor.getString(cursor.getColumnIndexOrThrow(AmpConditionValuesDbColumns.VALUE));
            	
            	if (null == values)
            	{
            		values = new Vector<String>();
            	}
            	
            	values.add(value);
            }
        }
        finally
        {
            if (null != cursor)
            {
            	cursor.close();
            	cursor = null;
            }            
        }
        
        return values;
    }
    
    /**
	 * Get the rule id by querying the AmpRulesDbColumns with the input campaign id.
	 * 
	 * @param campaignId The campaign id identifies the the campaign defined remotely.
	 * @return The rule id greater than 0 if it does exist, otherwise 0 if the amp rule hasn't been saved.
	 */
	private int getRuleIdFromCampaignId(final int campaignId)
	{
		int ruleId = 0;		
		Cursor cursor = null;
        try
        {        	        	
            cursor = mProvider.query(AmpRulesDbColumns.TABLE_NAME, new String[] { AmpRulesDbColumns._ID }, String.format("%s = ?", AmpRulesDbColumns.CAMPAIGN_ID), new String[] { Integer.toString(campaignId) }, null); //$NON-NLS-1$
            if (cursor.moveToFirst())
            {
            	ruleId = cursor.getInt(cursor.getColumnIndexOrThrow(AmpRulesDbColumns._ID));
            }
        }
        finally
        {
            if (null != cursor)
            {
                cursor.close();
                cursor = null;
            }            
        }
		return ruleId;  
	}
	
	/**
	 * Get the condition id by querying the AmpConditionsDbColumns with the input rule id.
	 * 
	 * @param ruleId The rule id identifies the the amp rule parsed from the amp message.
	 * @return A list of condition id greater than 0 if it does exist, otherwise 0 if the amp condition hasn't been saved.
	 */
	private long[] getConditionIdFromRuleId(final long ruleId)
	{
		long conditionIds[] = null;
		
		Cursor cursor = null;
        try
        {        	        	
            cursor = mProvider.query(AmpConditionsDbColumns.TABLE_NAME, new String[] { AmpConditionsDbColumns._ID }, String.format("%s = ?", AmpConditionsDbColumns.RULE_ID_REF), new String[] { Long.toString(ruleId) }, null); //$NON-NLS-1$
            conditionIds = new long[cursor.getCount()];
            
            int i = 0;
            while (cursor.moveToNext())
            {
            	conditionIds[i++] = cursor.getInt(cursor.getColumnIndexOrThrow(AmpConditionsDbColumns._ID));
            }
        }
        finally
        {
            if (null != cursor)
            {
                cursor.close();
                cursor = null;
            }            
        }
		return conditionIds;  
	}
    
    /**
     * Retrieve the amp message information as a hash map by query the database table with the event name.
     * 
     * @param eventName The event name associated with the amp rule.
     * @return A vector of amp message hash maps associated with the event name.
     */
    private Vector<Map<String, Object>> getAmpMessageMaps(final String eventName) 
    {    	
    	final Vector<Map<String, Object>> ampMessageMaps = new Vector<Map<String, Object>>();
    	
    	Cursor rulesCursor = null;
        Cursor ruleEventsCursor = null;
        try
        {
        	String now = Long.toString(System.currentTimeMillis() / 1000);
        	
        	rulesCursor = mProvider.query(AmpRulesDbColumns.TABLE_NAME, null, SELECTION_AMP_RULES, new String[] { now }, AMP_RULES_SORT_ORDER);
        	
        	ruleEventsCursor = mProvider.query(AmpRuleEventDbColumns.TABLE_NAME, null, SELECTION_AMP_RULEEVENTS, new String[] { eventName }, AMP_RULEEVENTS_SORT_ORDER);
        	
        	for (int i = 0; i < ruleEventsCursor.getCount(); ++i)
        	{
	        	ruleEventsCursor.moveToPosition(i);
	        	
	        	int ruleIdRef = ruleEventsCursor.getInt(ruleEventsCursor.getColumnIndexOrThrow(AmpRuleEventDbColumns.RULE_ID_REF));
	        	for (int j = 0; j < rulesCursor.getCount(); ++j)
	        	{
	        		rulesCursor.moveToPosition(j);
	        		
	        		int ruleId = rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns._ID));
	        		if (ruleId == ruleIdRef)
	        		{
	        			final Map<String, Object> ampMap = new HashMap<String, Object>();
                    	
                    	ampMap.put(AmpRulesDbColumns._ID, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns._ID)));
                    	ampMap.put(AmpRulesDbColumns.CAMPAIGN_ID, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.CAMPAIGN_ID)));    	
                    	ampMap.put(AmpRulesDbColumns.EXPIRATION, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.EXPIRATION)));
                    	ampMap.put(AmpRulesDbColumns.DISPLAY_SECONDS, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.DISPLAY_SECONDS)));
                    	ampMap.put(AmpRulesDbColumns.DISPLAY_SESSION, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.DISPLAY_SESSION)));
                    	ampMap.put(AmpRulesDbColumns.VERSION, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.VERSION)));      
                    	ampMap.put(AmpRulesDbColumns.PHONE_LOCATION, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.PHONE_LOCATION)));
                    	ampMap.put(AmpRulesDbColumns.PHONE_SIZE_WIDTH, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.PHONE_SIZE_WIDTH)));
                    	ampMap.put(AmpRulesDbColumns.PHONE_SIZE_HEIGHT, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.PHONE_SIZE_HEIGHT)));
                    	ampMap.put(AmpRulesDbColumns.TABLET_LOCATION, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.TABLET_LOCATION)));
                    	ampMap.put(AmpRulesDbColumns.TABLET_SIZE_WIDTH, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.TABLET_SIZE_WIDTH)));
                    	ampMap.put(AmpRulesDbColumns.TABLET_SIZE_HEIGHT, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.TABLET_SIZE_HEIGHT)));
                    	ampMap.put(AmpRulesDbColumns.TIME_TO_DISPLAY, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.TIME_TO_DISPLAY)));
                    	ampMap.put(AmpRulesDbColumns.INTERNET_REQUIRED, rulesCursor.getInt(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.INTERNET_REQUIRED)));
                    	ampMap.put(AmpRulesDbColumns.AB_TEST, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.AB_TEST)));
                    	ampMap.put(AmpRulesDbColumns.RULE_NAME, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.RULE_NAME)));
                    	ampMap.put(AmpRulesDbColumns.LOCATION, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.LOCATION)));
                        ampMap.put(AmpRulesDbColumns.DEVICES, rulesCursor.getString(rulesCursor.getColumnIndexOrThrow(AmpRulesDbColumns.DEVICES)));
                                                    
                        ampMessageMaps.add(ampMap);
	        		}
	        	}
	        	rulesCursor.moveToFirst();
        	}        	
        }
        finally
        {
            if (null != rulesCursor)
            {
            	rulesCursor.close();
            	rulesCursor = null;
            }

            if (null != ruleEventsCursor)
            {
            	ruleEventsCursor.close();
            	ruleEventsCursor = null;
            }
        }    
        
        return ampMessageMaps;
    }
    
    /**
     * 
     * @param ampMessages
     * @param attributes
     * @return
     */
    private Map<String, Object> retrieveDisplayingCandidate(final Vector<Map<String, Object>> ampMessages, final Map<String, String> attributes)
    {
    	Map<String, Object> candidate = null;
    	
    	for (Map<String, Object> ampMessage : ampMessages)
    	{
	    	// Check the internet connectivity if internet is required
	    	boolean internetRequired = (Integer) ampMessage.get(AmpRulesDbColumns.INTERNET_REQUIRED) == 1;
	    	if (internetRequired && !isConnectingToInternet())
	    	{
	    		continue;
	    	}

	    	// Check to see whether the message satisfies the client side attributes
	    	if (null != attributes && !isAmpMessageSatisfiedConditions(ampMessage, attributes))
	    	{
				continue;
	    	}
	    	
	    	candidate = ampMessage;
    	}    	
    	
    	if (null != candidate)
    	{    	       
	    	/**
	    	 * Get the local URL of the HTML file
	    	 */
			String localHtmlURL = null;
			final int ruleId = (Integer) candidate.get(AmpRulesDbColumns._ID);    		
						
			final String zipFileDirPath   = getZipFileDirPath();
			final String unzipFileDirPath = getUnzipFileDirPath(ruleId);
			if (getRemoteFileURL(candidate).endsWith(".zip")) // TODO: use constant
			{
				// Decompress the zip file
				if (decompressZipFile(zipFileDirPath, unzipFileDirPath, String.format("amp_rule_%d.zip", ruleId))) 
				{
					// Use WebView.loadUrl rather than first read HTML into the String
					localHtmlURL = AmpConstants.PROTOCOL_FILE + "://" + unzipFileDirPath + File.separator + AmpConstants.DEFAULT_ZIP_PAGE; 
				}
			}
			else
			{
				localHtmlURL = AmpConstants.PROTOCOL_FILE + "://" + unzipFileDirPath + File.separator + AmpConstants.DEFAULT_ZIP_PAGE;
			}
			
			if (TextUtils.isEmpty(localHtmlURL))
			{
				return null;
			}
									
			/**
			 *  Get display width and display height in dp based on the devices
			 */
			int displayWidth  = 0;
			int displayHeight = 0;
			final String devices = (String) candidate.get(AmpRulesDbColumns.DEVICES);
			
			if (devices.equals(AmpConstants.DEVICE_TABLET))
			{
				displayWidth  = (Integer) candidate.get(AmpRulesDbColumns.TABLET_SIZE_WIDTH);
				displayHeight = (Integer) candidate.get(AmpRulesDbColumns.TABLET_SIZE_HEIGHT);
			}
			else if (devices.equals(AmpConstants.DEVICE_BOTH))
			{
				displayWidth  = (Integer) candidate.get(AmpRulesDbColumns.PHONE_SIZE_WIDTH);
				displayHeight = (Integer) candidate.get(AmpRulesDbColumns.PHONE_SIZE_HEIGHT);
			}		
			else
			{
				displayWidth  = (Integer) candidate.get(AmpRulesDbColumns.PHONE_SIZE_WIDTH);
				displayHeight = (Integer) candidate.get(AmpRulesDbColumns.PHONE_SIZE_HEIGHT);
			}
			
			/**
			 * Save the HTML URL, display with and display height
			 */
			candidate.put(AmpConstants.KEY_HTML_URL, localHtmlURL); // TODO: use constant
			candidate.put(AmpConstants.KEY_BASE_PATH, unzipFileDirPath);
			candidate.put(AmpConstants.KEY_DISPLAY_WIDTH, (float) displayWidth);
			candidate.put(AmpConstants.KEY_DISPLAY_HEIGHT, (float) displayHeight);
    	}
    	
    	return candidate;
    }
    
    /**
	 * Get the URL from which the zip file can be grabbed.
	 * 
	 * @param ampMessage The map holding all the key/value pairs from the received amp message.
	 * @return The URL address from which the zip file can be grabbed.
	 */
	private String getRemoteFileURL(final Map<String, Object> ampMap)
	{
		String url = null;
		
		final String devices = (String) ampMap.get(AmpRulesDbColumns.DEVICES);
		
		if (devices.compareTo(AmpConstants.DEVICE_TABLET) == 0)
		{
			url = (String) ampMap.get(AmpRulesDbColumns.TABLET_LOCATION);
		}
		else if (devices.compareTo(AmpConstants.DEVICE_BOTH) == 0)
		{
			url = (String) ampMap.get(AmpRulesDbColumns.PHONE_LOCATION);
		}		
		else
		{
			url = (String) ampMap.get(AmpRulesDbColumns.PHONE_LOCATION);
		}
		
		return url;
	}

    /**
     * Decompress the downloaded zip file from the given zip directory and save the unzipped files 
     * into the unzip file directory.
     * 
     * @param zipFileDir The directory within which the zip files are located.
     * @param unzipFileDir The directory where the decompressed files should be saved.
     * @param filename The filename of the zip file be decompressed.
     * @return true if the decompression is successful, otherwise false
     */
    private boolean decompressZipFile(final String zipFileDir, final String unzipFileDir, final String filename) 
    {
    	ZipInputStream zis = null;
        try 
        {
            zis = new ZipInputStream(new FileInputStream(zipFileDir + File.separator + filename));            
                    
            ZipEntry ze = null;
            
            byte[] buffer = new byte[8192];
            
            while ((ze = zis.getNextEntry()) != null) 
            {
            	// For each entry to be extracted
            	String entryName = unzipFileDir + File.separator + ze.getName();            	
            	if (ze.isDirectory()) 
            	{ 
            		File newFile = new File(entryName);
            		if (!newFile.mkdir())
            		{
            			if (Constants.IS_LOGGABLE)
            			{
            				Log.w(Constants.LOG_TAG, String.format("Could not create directory %s", entryName)); //$NON-NLS-1$
            			}
            		}
            	} 
            	else 
            	{
            		FileOutputStream fos = new FileOutputStream(entryName);
            		
            		int byteRead = 0;
            		while ((byteRead = zis.read(buffer, 0, buffer.length)) > 0) 
            		{
            			fos.write(buffer, 0, byteRead);
            		}
            		
            		fos.close();
            		zis.closeEntry();
            	}
            }                                                   
        } 
        catch (final IOException e) 
        {
			if (Constants.IS_LOGGABLE)
			{
				Log.w(Constants.LOG_TAG, "Caught IOException", e); //$NON-NLS-1$
			}
			return false;
		}
        finally
        {
        	try 
        	{        				
        		if (null != zis)
        		{
        			zis.close();
        			zis = null;
        		}
			} 
        	catch (final IOException e) 
        	{
				if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Caught IOException", e); //$NON-NLS-1$
                }
				return false;
			}
        }
         
        return true;
    }
	
    /**
	 * Get the directory path of the zip file.
	 * 	 
	 * @return The absolute directory path within which to get the zip file.
	 */
	private String getZipFileDirPath()
	{
		StringBuilder builder = new StringBuilder();
		
		if (LocalyticsAmpSession.USE_EXTERNAL_DIRECTORY)
		{
			builder.append(Environment.getExternalStorageDirectory().getAbsolutePath());			
		}
		else
		{
			builder.append(mContext.getFilesDir().getAbsolutePath());
		}		
		builder.append(File.separator);
		builder.append(LocalyticsAmpSession.LOCALYTICS_DIR);
		builder.append(File.separator);
		builder.append(LocalyticsAmpSession.LOCALYTICS_AMPDIR);
		
		return builder.toString();
	}
	
	/**
	 * Create and return the directory path for saving the decompressed or downloaded files.
	 * 
	 * @param ruleId The newly created directory name is distinguished by the unique rule id.
	 * @return The absolute directory path within which to save the decompressed or downloaded files.
	 */
	private String getUnzipFileDirPath(final int ruleId)
	{
		StringBuilder builder = new StringBuilder();
		
		if (LocalyticsAmpSession.USE_EXTERNAL_DIRECTORY)
		{
			builder.append(Environment.getExternalStorageDirectory().getAbsolutePath());			
		}
		else
		{
			builder.append(mContext.getFilesDir().getAbsolutePath());
		}		
		builder.append(File.separator);
		builder.append(LocalyticsAmpSession.LOCALYTICS_DIR);
		builder.append(File.separator);
		builder.append(LocalyticsAmpSession.LOCALYTICS_AMPDIR);
		builder.append(File.separator);
		builder.append(String.format("amp_rule_%d", ruleId));
		
		final String path = builder.toString();
		
		File file = new File(path);
		// If the file does not exist or the file does exist but it's not a directory
		// Create a new directory
		if (!file.exists() || !file.isDirectory())
		{
			if (!file.mkdirs()) 
			{
				if (Constants.IS_LOGGABLE)
	            {        		
	                Log.w(Constants.LOG_TAG, String.format("Could not create the directory %s for saving the decompressed file.", file.getAbsolutePath())); //$NON-NLS-1$
	            } 
				return null;
			}
		}		
		
		return path;
	}
    
	/**
	 * The method is used to check whether the internet connection on the device is available.
	 * 
	 * @return true if the device is currently connecting to the internet, otherwise false
	 */
    private boolean isConnectingToInternet()
    {
    	ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    	if (null != connectivity) 
    	{
    		NetworkInfo[] info = connectivity.getAllNetworkInfo();
    		if (null != info) 
    		{
    			for (int i = 0; i < info.length; ++i)
    			{
    				if (info[i].getState() == NetworkInfo.State.CONNECTED)
    				{
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }	       
}
