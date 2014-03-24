package com.localytics.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Helper object to the {@link AmpSessionHandler} which helps process upload requests and responses.
 */
/* package */class AmpUploadHandler extends UploadHandler 
{	
	/**
     * Projection for querying record of the amp rule
     */
    private static final String[] PROJECTION_AMP_RULE_RECORD = new String[]
       {
            AmpRulesDbColumns._ID,
            AmpRulesDbColumns.VERSION	};
    
    /**
     * Selection for {@link #tagScreen(String)}.
     */
    private static final String SELECTION_UPDATE_AMP_RULE = String.format("%s = ?", AmpRulesDbColumns._ID); //$NON-NLS-1$
	
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
	 * @param eventName The amp display event name.
	 */
	private void bindRuleToEvents(final long ruleId, final List<String> eventNames)
	{	
		// Delete the old bindings
		mProvider.delete(AmpRuleEventDbColumns.TABLE_NAME, String.format("%s = ?", AmpRuleEventDbColumns.RULE_ID_REF), new String[] { Long.toString(ruleId) }); //$NON-NLS-1$
		
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
	 * Get the zip file name associated with the rule id.
	 * 
	 * @param ruleId
	 * @return The zip file name distinguished by the rule id.
	 */
	private String getLocalFileURL(final long ruleId, final boolean isZipped)
	{
		StringBuilder builder = new StringBuilder();
				
		builder.append(getAmpDataDirectory());
		builder.append(File.separator);
		
		if (isZipped) 
		{
			builder.append(String.format("amp_rule_%d.zip", ruleId));
		}
		else
		{
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
		                Log.w(Constants.LOG_TAG, String.format("Could not create the directory %s for saving the HTML file.", file.getAbsolutePath())); //$NON-NLS-1$
		            } 
					return null;
				}
			}
			
			builder.append(File.separator);
			builder.append(AmpConstants.DEFAULT_ZIP_PAGE);
		}		
		
		return builder.toString();
	}
	
	/**
	 * Get the directory path for saving the zip file.
	 * 
	 * @return The absolute directory path in which to save the zip file.
	 */
	private String getAmpDataDirectory()
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
	 * Get the URL from which the zip file can be grabbed.
	 * 
	 * @param ampMessage The map holding all the key/value pairs from the received amp message.
	 * @return The URL address from which the zip file can be grabbed.
	 */
	private String getRemoteFileURL(final Map<String, Object> ampMessage)
	{
		String url = null;
		
		final String devices = getSafeStringFromMap(ampMessage, AmpRulesDbColumns.DEVICES);
		
		if (devices.equals(AmpConstants.DEVICE_TABLET))
		{
			url = getSafeStringFromMap(ampMessage, AmpRulesDbColumns.TABLET_LOCATION);
		}
		else if (devices.equals(AmpConstants.DEVICE_BOTH))
		{
			url = getSafeStringFromMap(ampMessage, AmpRulesDbColumns.PHONE_LOCATION);
		}		
		else
		{
			url = getSafeStringFromMap(ampMessage, AmpRulesDbColumns.PHONE_LOCATION);
		}
		
		return url;
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
    	final int campaignId = getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.CAMPAIGN_ID); 
    	
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
		        	
		        	int remoteVersion = getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.VERSION);
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
		    		saveAmpConditions(ruleId, getSafeListFromMap(ampMessage, AmpConstants.CONDITIONS_KEY));
		    		
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
			final String remoteFileURL = getRemoteFileURL(ampMessage);
			final String localFileURL  = getLocalFileURL(ruleId, remoteFileURL.endsWith(".zip"));
			if (!TextUtils.isEmpty(remoteFileURL) && !TextUtils.isEmpty(localFileURL))
			{				
				downloadFile(remoteFileURL, localFileURL, true); // Enable the overwrite so the file can be updated
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
    	values.put(AmpRulesDbColumns.CAMPAIGN_ID, getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.CAMPAIGN_ID));    	
        values.put(AmpRulesDbColumns.EXPIRATION, getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.EXPIRATION));
        values.put(AmpRulesDbColumns.DISPLAY_SECONDS, getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.DISPLAY_SECONDS));
        values.put(AmpRulesDbColumns.DISPLAY_SESSION, getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.DISPLAY_SESSION));
        values.put(AmpRulesDbColumns.VERSION, getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.VERSION));      
        values.put(AmpRulesDbColumns.PHONE_LOCATION, getSafeStringFromMap(ampMessage, AmpRulesDbColumns.PHONE_LOCATION));
        Map<String, Object> phoneSize = getSafeMapFromMap(ampMessage, AmpConstants.PHONE_SIZE_KEY);   // phone size map
        values.put(AmpRulesDbColumns.PHONE_SIZE_WIDTH, getSafeIntegerFromMap(phoneSize, AmpConstants.WIDTH_KEY));
        values.put(AmpRulesDbColumns.PHONE_SIZE_HEIGHT, getSafeIntegerFromMap(phoneSize, AmpConstants.HEIGHT_KEY));
        Map<String, Object> tabletSize = getSafeMapFromMap(ampMessage, AmpConstants.TABLET_SIZE_KEY); // tablet size map
        values.put(AmpRulesDbColumns.TABLET_LOCATION, getSafeStringFromMap(ampMessage, AmpRulesDbColumns.TABLET_LOCATION));
        values.put(AmpRulesDbColumns.TABLET_SIZE_WIDTH, getSafeIntegerFromMap(tabletSize, AmpConstants.WIDTH_KEY));
        values.put(AmpRulesDbColumns.TABLET_SIZE_HEIGHT, getSafeIntegerFromMap(tabletSize, AmpConstants.HEIGHT_KEY));
        values.put(AmpRulesDbColumns.TIME_TO_DISPLAY, 1);
        values.put(AmpRulesDbColumns.INTERNET_REQUIRED, getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.INTERNET_REQUIRED));
        values.put(AmpRulesDbColumns.AB_TEST, getSafeStringFromMap(ampMessage, AmpRulesDbColumns.AB_TEST));
        values.put(AmpRulesDbColumns.RULE_NAME, getSafeStringFromMap(ampMessage, AmpRulesDbColumns.RULE_NAME));
        values.put(AmpRulesDbColumns.LOCATION, getSafeStringFromMap(ampMessage, AmpRulesDbColumns.LOCATION));
        values.put(AmpRulesDbColumns.DEVICES, getSafeStringFromMap(ampMessage, AmpRulesDbColumns.DEVICES));
        
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
			mProvider.delete(AmpConditionValuesDbColumns.TABLE_NAME, String.format("%s = ?", AmpConditionValuesDbColumns.CONDITION_ID_REF), new String[] { Long.toString(conditionId) }); //$NON-NLS-1$			
		}
		mProvider.delete(AmpConditionsDbColumns.TABLE_NAME, String.format("%s = ?", AmpConditionsDbColumns.RULE_ID_REF), new String[] { Long.toString(ruleId) }); //$NON-NLS-1$
		
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
			// depends on the operator the user chooses.
        	for (int i = 2; i < condition.size(); ++i)
        	{
        		values = new ContentValues(); 
        		values.put(AmpConditionValuesDbColumns.VALUE, getSafeStringFromValue(condition.get(i)));
        		values.put(AmpConditionValuesDbColumns.CONDITION_ID_REF, conditionId);	            		            		    	        
            	mProvider.insert(AmpConditionValuesDbColumns.TABLE_NAME, values);
        	}	 	          		
		}
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
	 * Check whether the downloaded amp message is valid or not.
	 * 
	 * @param ampMessage A map generated from the downloaded amp message.
	 * @return true if the amp message is valid, otherwise false.
	 */
	private boolean validateAMPMessage(Map<String, Object> ampMessage) 
	{
		int campaignId = getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.CAMPAIGN_ID);		
		String ruleName = getSafeStringFromMap(ampMessage, AmpRulesDbColumns.RULE_NAME);
		List<Object> eventNames = getSafeListFromMap(ampMessage, AmpConstants.DISPLAY_EVENTS_KEY); // TODO: constant in diff place?
		int expiration = getSafeIntegerFromMap(ampMessage, AmpRulesDbColumns.EXPIRATION);
		String location = getSafeStringFromMap(ampMessage, AmpRulesDbColumns.LOCATION);
		
		long now = System.currentTimeMillis() / 1000;
		
		// Check for required fields.
		boolean isValid = (campaignId != 0) && !TextUtils.isEmpty(ruleName) && (eventNames != null) && !TextUtils.isEmpty(location) && (expiration > now);
		return isValid;
	}
	
	private String getSafeStringFromValue(Object value)
	{
		String stringValue = null;
		
		if (null == value) 
		{
			return null;
		}
		else if (value instanceof Integer) 
		{
			stringValue = Integer.toString((Integer) value);
		}
		else if (value instanceof String)
		{
			stringValue = (String) value;
		}
		
		return stringValue;
	}
    
	private int getSafeIntegerFromMap(Map<String, Object> map, String key)
	{
		int integerValue = 0;
		Object value = map.get(key);
		
		if (null == value) 
		{
			return 0;
		}
		else if (value instanceof Integer ) 
		{
			integerValue = (Integer) value;
		}
		else if (value instanceof String)
		{
			integerValue = Integer.parseInt((String) value);
		}
		
		return integerValue;
	}

	private String getSafeStringFromMap(Map<String, Object> map, String key)
	{
		String stringValue = null;
		Object value = map.get(key);
		
		if (null == value) 
		{
			return null;
		}
		else if (value instanceof Integer) 
		{
			stringValue = Integer.toString((Integer) value);
		}
		else if (value instanceof String)
		{
			stringValue = (String) value;
		}
		
		return stringValue;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getSafeMapFromMap(Map<String, Object> map, String key)
	{
		Map<String, Object> mapValue = null;
		Object value = map.get(key);
		
		if (null == value) 
		{
			return null;
		}
		else if (value instanceof Map) 
		{
			mapValue = (Map<String, Object>) value;
		}
		
		return mapValue;
	}

	@SuppressWarnings("unchecked")
	private List<Object> getSafeListFromMap(Map<String, Object> map, String key)
	{
		List<Object> listValue = null;
		Object value = map.get(key);
		
		if (null == value)
		{
			return null;
		}
		else if (value instanceof List) 
		{
			listValue = (List<Object>) value;
		}
		
		return listValue;
	}
	
	/**
	 * Download one file from the URL address and saves it to the local phone.	 
	 * 
	 * @param remoteFilePath The remote file path
	 * @param localFilePath The local file path
	 * @param isOverwrite Indicate whether the file should be download if the local file with the same name does exist.
	 * @return the name of the file download if successful, otherwise null.
	 */
	public static String downloadFile(String remoteFilePath, String localFilePath, boolean isOverwrite) 
	{	
		String result = localFilePath;
		
		File file = new File(localFilePath);
		if (file.exists() && !isOverwrite) 
		{
			if (Constants.IS_LOGGABLE)
            {        		
                Log.w(Constants.LOG_TAG, String.format("The file %s does exist and overwrite is turned off.", file.getAbsolutePath())); //$NON-NLS-1$
            } 
			return localFilePath;
		}
		
		File dir = file.getParentFile();
		if (!(dir.mkdirs() || dir.isDirectory()))
		{
			if (Constants.IS_LOGGABLE)
            {        		
                Log.w(Constants.LOG_TAG, String.format("Could not create the directory %s for saving file.", dir.getAbsolutePath())); //$NON-NLS-1$
            } 
			return null;
		}

		try 
		{
			URL url = new URL(remoteFilePath);					
			URLConnection ucon = url.openConnection();			
								
			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			final int BUF_SIZE = 8192;
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is, BUF_SIZE << 1);
			FileOutputStream fos = new FileOutputStream(localFilePath);

			int read = 0;
			byte[] buffer = new byte[BUF_SIZE];
			
			while ((read = bis.read(buffer)) != -1) 
			{
				fos.write(buffer, 0, read);
			}			
			fos.close();
		} 
		catch (IOException e) 
		{
			if (Constants.IS_LOGGABLE)
            {        		
                Log.w(Constants.LOG_TAG, "AMP campaign not found. Creating a new one."); //$NON-NLS-1$
            } 
			result = null;
		}
		
		return result;
	}		
}
