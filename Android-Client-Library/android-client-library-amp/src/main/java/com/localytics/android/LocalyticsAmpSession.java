package com.localytics.android;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class LocalyticsAmpSession extends LocalyticsSession 
{
    /**
     * Constructs a new {@link LocalyticsAmpSession} object.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining references to
     *            {@code Activity} instances. Cannot be null.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if LOCALYTICS_APP_KEY in AndroidManifest.xml is null or empty
     */
    public LocalyticsAmpSession(final Context context)
    {
        this(context, null);
    }

    /**
     * Constructs a new {@link LocalyticsAmpSession} object.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining references to
     *            {@code Activity} instances. Cannot be null.
     * @param key The key unique for each application generated at www.localytics.com. Cannot be null or empty.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if {@code key} is null or empty
     */
	public LocalyticsAmpSession(final Context context, final String key) 
	{
		super(context, key);

		// Update the thread name displayed in DDMS
		sSessionHandlerThread.setName(AmpSessionHandler.class.getSimpleName());
		
		// Create localytics directory on the app's folder
		createLocalyticsDirectory(context);
	}

	/**
     * Initiates an upload of any Localytics data for this session's API key. This should be done early in the process life in
     * order to guarantee as much time as possible for slow connections to complete. It is necessary to do this even if the user
     * has opted out because this is how the opt out is transported to the webservice.
     */
	@Override
    public void upload()
    {
		final AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		handler.sendMessage(handler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, new Runnable() 
		{
			public void run()
			{
				handler.sendMessage(handler.obtainMessage(AmpSessionHandler.MESSAGE_TRIGGER_AMP, new Object[] { AmpConstants.AMP_START_TRIGGER, null }));
			}
		}));
    }
	
	/**
	 * Trigger the in-app-messaging dialog with the given event name.
     * This event should be uploaded first by tagEvent and the campaign associated with this event should be created.
     *
	 * @param eventName
	 */
	public void triggerAmp(final String eventName)
    {
		triggerAmp(eventName, null);
    }
	
	/**
     * Trigger the in-app-messaging dialog with the given event name.
     * This event should be uploaded first by tagEvent and the campaign associated with this event should be created.
     *
	 * @param eventName The event name, must not be null.
	 * @param attributes The attributes associated with the event. These attributes should be uploaded first by tagEvent.
	 */
	public void triggerAmp(final String eventName, final Map<String, String> attributes)
    {
        /*
         * Convert the event and attributes into the internal representation of packagename:eventName and packagename:key
         */
        final String eventString = String.format(Constants.EVENT_FORMAT, mContext.getPackageName(), eventName);

        final TreeMap<String, String> remappedAttributes = new TreeMap<String, String>();

        if (null != attributes)
        {
            final String packageName = mContext.getPackageName();
            for (final Map.Entry<String, String> entry : attributes.entrySet())
            {
                remappedAttributes.put(String.format(LocalyticsProvider.AttributesDbColumns.ATTRIBUTE_FORMAT, packageName, entry.getKey()), entry.getValue());
            }
        }

		final AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		handler.sendMessage(handler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, new Runnable() 
		{
			public void run()
			{
				handler.sendMessage(handler.obtainMessage(AmpSessionHandler.MESSAGE_TRIGGER_AMP, new Object[] { eventString, (attributes == null) ? null : remappedAttributes }));
			}
		}));
    }
	
	/**
	 * Attach the current foreground activity to the amp session. The fragment manager associated with each activity
	 * is needed for showing the dialog fragment which hosts the amp dialog. This method gets the fragment manager
	 * from the input activity and attaches it with the amp session handler which controls the amp trigger.
	 *
	 * @param activity The foreground activity, must not be null.
	 */
	public void attach(final FragmentActivity activity)
	{
		if (activity == null)
        {
            throw new IllegalArgumentException("attached activity cannot be null"); //$NON-NLS-1$
        }
		
		AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
        handler.setFragmentManager(activity.getSupportFragmentManager());
	}
	
	/**
	 * Opposite from the attach, this method detaches any fragment manager from the amp session.
	 */
	public void detach()
	{		
		AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		handler.setFragmentManager(null);
	}

    /**
     * Determine whether the test mode is enabled.
     *
     * @return true if the test mode is enabled, otherwise false.
     */
	public static boolean isTestModeEnabled()
	{
		return AmpConstants.isTestModeEnabled();
	}

    /**
     * Enable/Disable the test mode with the given flag.
     * If the user is not in the test mode, all in-app-message dialog will not show once and the information
     * to show the dialog will be deleted after the dialog is closed. In test mode, the information will never
     * be deleted so the same in-app-message dialog will always show under the right condition.
     *
     * @param enabled Flag to indicate whether to enable the test mode or not.
     */
	public static void setTestModeEnabled(final boolean enabled)
	{
		AmpConstants.setTestModeEnabled(enabled);
	}
	
	/**
	 * Create the localytics directory in which the amp data should be saved.
	 *   
	 * @param context The application context
	 * @return true if the directory does exist or has been created, false if the directory cannot be created.
	 */
	private boolean createLocalyticsDirectory(final Context context) 
	{
		StringBuilder builder = new StringBuilder();
		
		if (AmpConstants.USE_EXTERNAL_DIRECTORY)
		{
			builder.append(Environment.getExternalStorageDirectory().getAbsolutePath());			
		}
		else
		{
			builder.append(context.getFilesDir().getAbsolutePath());
		}	
		builder.append(File.separator);
		builder.append(AmpConstants.LOCALYTICS_DIR);
		
		File dir = new File(builder.toString());
		
		if (!(dir.mkdirs() || dir.isDirectory())) 
		{
			return false;
		}
		
		return true;
	}
	
	

	/**
     * Gets a new Handler that runs on {@code looper}.
     *
     * @param context Application context. Cannot be null.
     * @param key Localytics API key. Cannot be null.
     * @param looper to run the Handler on. Cannot be null.
     */
	@Override
	protected SessionHandler createSessionHandler(final Context context, final String key, final Looper looper) 
    {
    	return new AmpSessionHandler(context, key, sSessionHandlerThread.getLooper());
    }		
}
