package com.localytics.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.localytics.android.LocalyticsProvider.AmpConditionValuesDbColumns;
import com.localytics.android.LocalyticsProvider.AmpConditionsDbColumns;
import com.localytics.android.LocalyticsProvider.AmpDisplayedDbColumns;
import com.localytics.android.LocalyticsProvider.AmpRuleEventDbColumns;
import com.localytics.android.LocalyticsProvider.AmpRulesDbColumns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Helper object to the {@link AmpSessionHandler} which helps process upload requests and responses.
 */
/* package */class AmpUploadHandler extends UploadHandler 
{
    /**
     * The upload wake lock
     */
    private PowerManager.WakeLock mWakeLock;

    /**
     * The tag for upload wakelock
     */
    private static final String UPLOAD_WAKE_LOCK = "UPLOAD_WAKE_LOCK";

	/**
     * Projection for querying record of the amp rule
     */
    private static final String[] PROJECTION_AMP_RULE_RECORD = new String[]
       {
            AmpRulesDbColumns._ID,
            AmpRulesDbColumns.VERSION	};
    

    private static final String SELECTION_UPDATE_AMP_RULE = String.format("%s = ?", AmpRulesDbColumns._ID); //$NON-NLS-1$

    @Override
    public void handleMessage(final Message msg)
    {
        try
        {
            enterWakeLock();
            super.handleMessage(msg);
        }
        finally
        {
            exitWakeLock();
        }
    }

	/**
     * Constructs a new Handler that runs on {@code looper}.
     * <p>
     * Note: This constructor may perform disk access.
     *
     * @param context Application context. Cannot be null.
     * @param sessionHandler Parent {@link SessionHandler} object to notify when uploads are completed. Cannot be null.
     * @param apiKey Localytics API key. Cannot be null.
     * @param installId Localytics install ID.
     * @param looper to run the Handler on. Cannot be null.
     */
	public AmpUploadHandler(final Context context, final Handler sessionHandler, final String apiKey, final String installId, final Looper looper) 
	{
		super(context, sessionHandler, apiKey, installId, looper);		
	}

    private void enterWakeLock()
    {
        // Acquire a wake lock so that the CPU will stay awake while the user's app goes to the back
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, UPLOAD_WAKE_LOCK);
        mWakeLock.acquire();
        if (!mWakeLock.isHeld())
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "Localytics library failed to get wake lock"); //$NON-NLS-1$
            }
        }
    }

    private void exitWakeLock()
    {
        if (!mWakeLock.isHeld())
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "WakeLock will be released but not held when should be."); //$NON-NLS-1$
            }
        }

        // Release the wake lock so that the CPU can go back into low power mode
        mWakeLock.release();

        if (mWakeLock.isHeld())
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "WakeLock was not released when it should have been.");
            }
        }
    }
	
	/**
     * This method is called when the session uploading is successful and the AMP response is received.
     * 
     * @param response AMP response received from the HTTP .
     */    
	protected void onUploadResponded(final String response)
    {    	
    	if (Constants.IS_LOGGABLE)
        {
            Log.w(Constants.LOG_TAG, String.format("get session upload response: \n%s", response)); //$NON-NLS-1$
        }
    	
    	try 
    	{
    		// Parse the responded JSON string into the list of amp messages.
    		Map<String, Object> ampMap = JsonHelper.toMap(new JSONObject(response));    	
    		@SuppressWarnings("unchecked")
			List<Map<String, Object>> ampMessages = JsonHelper.toList((JSONArray) JsonHelper.toJSON(ampMap.get(AmpConstants.AMP_KEY)));
    		
    		// Save each amp message to the local device
    		for (Map<String, Object> ampMessage : ampMessages) 
        	{    
        		saveAMPMessage(ampMessage);
        	}    
		} 
    	catch (final JSONException e) 
    	{
    		if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "JSONException", e); //$NON-NLS-1$
            }
            return;
		}	
    }
	
	/**
	 * Bind the amp rule to the given event by insert a new row into the AmpRuleEventDbColumns.
	 * <p>
	 * Note that the row specified by ruleId must be already in the AmpRulesDbColumns because of the foreign key constraints.
	 * 
	 * @param ruleId The rule id identifies one row in the AmpRulesDbColumns.
	 * @param eventNames The amp display event name.
	 */
	private void bindRuleToEvents(final long ruleId, final List<String> eventNames)
	{	
		// Delete the old bindings
		mProvider.remove(AmpRuleEventDbColumns.TABLE_NAME, String.format("%s = ?", AmpRuleEventDbColumns.RULE_ID_REF), new String[] { Long.toString(ruleId) }); //$NON-NLS-1$
		
		// Create new bindings between the rule and each event.
		for (final String eventName : eventNames)
		{				        
        	final ContentValues values = new ContentValues(); 
            values.put(AmpRuleEventDbColumns.EVENT_NAME, eventName);    	
            values.put(AmpRuleEventDbColumns.RULE_ID_REF, ruleId);                
        	mProvider.insert(AmpRuleEventDbColumns.TABLE_NAME, values);
		}
	}
	
	/**
	 * Parse the amp rule from the received amp message 
	 * Insert the amp rule to the database table if it does not exist, otherwise update the table.
	 * 	
	 * @param ampMessage The map holding all the key/value pairs from the received amp message. 
	 * @return long Representing the number of rows modified, which is in the range from 0 to the number of items in the table. 
	 * @throws Exception 
	 */
	private int saveAMPMessage(final Map<String, Object> ampMessage)
	{		
		// Validate the message
		if (!validateAMPMessage(ampMessage)) 
		{
			return 0;
		}  	    	    	     	
		
		// Get the campaign id from the amp message for querying the database
    	final int campaignId = JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.CAMPAIGN_ID);
    	
    	// Check whether the campaign has been displayed
    	int displayed = 0;
    	Cursor cursorDisplayed = null;
        try
        {        	        	
        	cursorDisplayed = mProvider.query(AmpDisplayedDbColumns.TABLE_NAME, new String[] { AmpDisplayedDbColumns.DISPLAYED }, String.format("%s = ?", AmpDisplayedDbColumns.CAMPAIGN_ID), new String[] { Integer.toString(campaignId) }, null); //$NON-NLS-1$
            if (cursorDisplayed.moveToFirst())
            {
            	displayed = cursorDisplayed.getInt(cursorDisplayed.getColumnIndex(AmpDisplayedDbColumns.DISPLAYED));
            }    
        }         
        catch (Exception e)
        {
        	e.printStackTrace();
        }
        finally
        {
            if (null != cursorDisplayed)
            {
            	cursorDisplayed.close();
            	cursorDisplayed = null;
            }
        }
        
    	// Do nothing if the campaign has been displayed        
        if (displayed != 0)
    	{
    		return 0;
    	} 
        
        // The returned rule id will be greater than 0 only when the rule has been inserted or updated.
		int ruleId = (Integer) mProvider.runBatchTransaction(new Callable<Object>()
		{
		    public Object call() throws Exception
		    {		    							        
		    	int ruleId = 0, localVersion = 0;		    			    	
		    	
		    	// Try to get the amp rule id and version number if the amp rule has previously been inserted into the database
		    	Cursor cursorRule = null;
		        try
		        {        	        	
		        	cursorRule = mProvider.query(AmpRulesDbColumns.TABLE_NAME, PROJECTION_AMP_RULE_RECORD, String.format("%s = ?", AmpRulesDbColumns.CAMPAIGN_ID), new String[] { Integer.toString(campaignId) }, null); //$NON-NLS-1$
		            if (cursorRule.moveToFirst())
		            {
		            	ruleId = cursorRule.getInt(cursorRule.getColumnIndexOrThrow(AmpRulesDbColumns._ID));  
		            	localVersion = cursorRule.getInt(cursorRule.getColumnIndexOrThrow(AmpRulesDbColumns.VERSION));
		            }    
		        }           
		        finally
		        {
		            if (null != cursorRule)
		            {
		            	cursorRule.close();
		            	cursorRule = null;
		            }
		        }
		    	
		        // The rule does exists locally, update it if the version received is later than the local one
		    	if (ruleId > 0) 
		        {
		        	if (Constants.IS_LOGGABLE)
		            {        		
		                Log.w(Constants.LOG_TAG, String.format("Existing AMP rule already exists for this campaign\n\t campaignID = %d\n\t ruleID = %d", campaignId, ruleId)); //$NON-NLS-1$
		            }        	        
		        	
		        	int remoteVersion = JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.VERSION);
                	if (localVersion >= remoteVersion) 
        			{
        				if (Constants.IS_LOGGABLE)
        	            {        		
        					Log.w(Constants.LOG_TAG, String.format("No update needed. Campaign version has not been updated\n\t version: %d", localVersion)); //$NON-NLS-1$
        	            }
        				return Integer.valueOf(0);
        			}
					
		        	// The rule does exist in the database but it hasn't been shown yet and it needs to be updated.
		        	final ContentValues values = parseAmpMessage(ampMessage);
		        	ruleId = mProvider.update(AmpRulesDbColumns.TABLE_NAME, values, SELECTION_UPDATE_AMP_RULE, new String[] { Integer.toString(ruleId) });
		        }  
		        // Here comes a new rule that does not exist locally, insert it into the database 
		        else
		        {        	
		        	if (Constants.IS_LOGGABLE)
		            {        		
		                Log.w(Constants.LOG_TAG, "AMP campaign not found. Creating a new one."); //$NON-NLS-1$
		            }  
		        	
		        	final ContentValues values = parseAmpMessage(ampMessage);
		        	ruleId = (int) mProvider.insert(AmpRulesDbColumns.TABLE_NAME, values);     	        	  
		        }
		       		        
		    	if (ruleId > 0)
		    	{
		    		// Save or update amp conditions associated with this campaign
		    		saveAmpConditions(ruleId, JsonHelper.getSafeListFromMap(ampMessage, AmpConstants.CONDITIONS_KEY));
		    		
		    		// Bind the current amp rule to the events so when some event happens, it can find the rule for displaying the campaign.
					@SuppressWarnings("unchecked")
					List<String> eventNames = JsonHelper.toList((JSONArray) JsonHelper.toJSON(ampMessage.get(AmpConstants.DISPLAY_EVENTS_KEY)));
					bindRuleToEvents(ruleId, eventNames);
		    	}
		        
				return Integer.valueOf(ruleId);
		    }
		});
		
		if (ruleId > 0)
		{
			// Fetch the attached ZIP or HTML file
			final String remoteFileURL = AmpDownloader.getRemoteFileURL(ampMessage);
			final String localFileURL  = AmpDownloader.getLocalFileURL(mContext, mApiKey, ruleId, remoteFileURL.endsWith(".zip"));
			if (!TextUtils.isEmpty(remoteFileURL) && !TextUtils.isEmpty(localFileURL))
			{
                AmpDownloader.downloadFile(remoteFileURL, localFileURL, true); // Enable the overwrite so the file can be updated
			}
		}
        
        return ruleId;
	}
	
	/**
	 * Parse the message into the rule parameters
     * 
	 * @param ampMessage The map holding all the key/value pairs from the received amp message.
	 * @return ContentValues filled with rule parameters for later database operations  
	 */
	private ContentValues parseAmpMessage(final Map<String, Object> ampMessage)
	{		
        final ContentValues values = new ContentValues();

    	values.put(AmpRulesDbColumns.CAMPAIGN_ID, JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.CAMPAIGN_ID));
        values.put(AmpRulesDbColumns.EXPIRATION, JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.EXPIRATION));
        values.put(AmpRulesDbColumns.DISPLAY_SECONDS, JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.DISPLAY_SECONDS));
        values.put(AmpRulesDbColumns.DISPLAY_SESSION, JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.DISPLAY_SESSION));
        values.put(AmpRulesDbColumns.VERSION, JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.VERSION));
        values.put(AmpRulesDbColumns.PHONE_LOCATION, JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.PHONE_LOCATION));
        Map<String, Object> phoneSize = JsonHelper.getSafeMapFromMap(ampMessage, AmpConstants.PHONE_SIZE_KEY);   // phone size map
        values.put(AmpRulesDbColumns.PHONE_SIZE_WIDTH, JsonHelper.getSafeIntegerFromMap(phoneSize, AmpConstants.WIDTH_KEY));
        values.put(AmpRulesDbColumns.PHONE_SIZE_HEIGHT, JsonHelper.getSafeIntegerFromMap(phoneSize, AmpConstants.HEIGHT_KEY));
        Map<String, Object> tabletSize = JsonHelper.getSafeMapFromMap(ampMessage, AmpConstants.TABLET_SIZE_KEY); // tablet size map
        values.put(AmpRulesDbColumns.TABLET_LOCATION, JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.TABLET_LOCATION));
        values.put(AmpRulesDbColumns.TABLET_SIZE_WIDTH, JsonHelper.getSafeIntegerFromMap(tabletSize, AmpConstants.WIDTH_KEY));
        values.put(AmpRulesDbColumns.TABLET_SIZE_HEIGHT, JsonHelper.getSafeIntegerFromMap(tabletSize, AmpConstants.HEIGHT_KEY));
        values.put(AmpRulesDbColumns.TIME_TO_DISPLAY, 1);
        values.put(AmpRulesDbColumns.INTERNET_REQUIRED, JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.INTERNET_REQUIRED));
        values.put(AmpRulesDbColumns.AB_TEST, JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.AB_TEST));
        values.put(AmpRulesDbColumns.RULE_NAME, JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.RULE_NAME));
        values.put(AmpRulesDbColumns.LOCATION, JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.LOCATION));
        values.put(AmpRulesDbColumns.DEVICES, JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.DEVICES));
        
        return values;
	}
	
	/**
	 * 
	 * @param ruleId
	 * @param conditions
	 */
	private void saveAmpConditions(final long ruleId, List<Object> conditions)
	{
		if (null == conditions)
		{
			return;
		}
		
		// Delete existing conditions associated with this rule id
		final long[] conditionIds = getConditionIdFromRuleId(ruleId);
		for (long conditionId : conditionIds)
		{
			mProvider.remove(AmpConditionValuesDbColumns.TABLE_NAME, String.format("%s = ?", AmpConditionValuesDbColumns.CONDITION_ID_REF), new String[] { Long.toString(conditionId) }); //$NON-NLS-1$
		}
		mProvider.remove(AmpConditionsDbColumns.TABLE_NAME, String.format("%s = ?", AmpConditionsDbColumns.RULE_ID_REF), new String[] { Long.toString(ruleId) }); //$NON-NLS-1$
		
		for (Object obj : conditions)
		{
			@SuppressWarnings("unchecked")
			final List<String> condition = (List<String>) obj;
	        			
        	// Insert item into conditions table, each item is actually a piece of attribute defined on the dash board
			// Each campaign can have several attributes, but each attribute only associates with only one campaign.
        	ContentValues values = new ContentValues(); 
	        values.put(AmpConditionsDbColumns.ATTRIBUTE_NAME, condition.get(0));
	        values.put(AmpConditionsDbColumns.OPERATOR, condition.get(1));
	        values.put(AmpConditionsDbColumns.RULE_ID_REF, ruleId);
        	long conditionId = mProvider.insert(AmpConditionsDbColumns.TABLE_NAME, values);
        	
        	// Insert item into condition values table, each condition(attribute) can have one or two values currently
			// depends on the operator the user chosen.
        	for (int i = 2; i < condition.size(); ++i)
        	{
        		values = new ContentValues(); 
        		values.put(AmpConditionValuesDbColumns.VALUE, JsonHelper.getSafeStringFromValue(condition.get(i)));
        		values.put(AmpConditionValuesDbColumns.CONDITION_ID_REF, conditionId);	            		            		    	        
            	mProvider.insert(AmpConditionValuesDbColumns.TABLE_NAME, values);
        	}	 	          		
		}
	}
	
	/**
	 * Get the condition id by querying the AmpConditionsDbColumns with the input rule id.
     * Condition is something like: (1)a1 == b1 and (2)a2 > b2, so one event may associates to many conditions.
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
	 * Check whether the downloaded amp message is valid or not.
	 * 
	 * @param ampMessage A map generated from the downloaded amp message.
	 * @return true if the amp message is valid, otherwise false.
	 */
	private boolean validateAMPMessage(Map<String, Object> ampMessage) 
	{
		int campaignId = JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.CAMPAIGN_ID);
		String ruleName = JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.RULE_NAME);
		List<Object> eventNames = JsonHelper.getSafeListFromMap(ampMessage, AmpConstants.DISPLAY_EVENTS_KEY); // TODO: constant in diff place?
		int expiration = JsonHelper.getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.EXPIRATION);
		String location = JsonHelper.getSafeStringFromMap(ampMessage, AmpRulesDbColumns.LOCATION);
		
		long now = System.currentTimeMillis() / 1000;
		
		// Check for required fields.
		boolean isValid = (campaignId != 0) && !TextUtils.isEmpty(ruleName) && (eventNames != null) && !TextUtils.isEmpty(location) && (expiration > now);
		return isValid;
	}
}
