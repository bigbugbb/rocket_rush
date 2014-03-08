package com.localytics.android;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalyticsAmpSession extends LocalyticsSession 
{    
	/**
     * Name of the directory in which all Localytics data is stored
     */	
	public static final String LOCALYTICS_DIR = ".localytics";
	
	/**
	 * Name of the directory in which amp data is stored
	 */
	public static final String LOCALYTICS_AMPDIR = "ampData";
	
	/**
	 * Temporary constant for testing only, remove it later
	 */
	public static final boolean USE_EXTERNAL_DIRECTORY = true;		
	
	private static AtomicBoolean mTestModeEnabled = new AtomicBoolean(false);
	
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
	 * 
	 * @param eventName
	 * @param attributes
	 */
	public void triggerAmp(final String eventName)
    {
		final AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		handler.sendMessage(handler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, new Runnable() 
		{
			public void run()
			{
				handler.sendMessage(handler.obtainMessage(AmpSessionHandler.MESSAGE_TRIGGER_AMP, new Object[] { eventName, null }));
			}
		}));
    }
	
	/**
	 * 
	 * @param eventName
	 * @param attributes
	 */
	public void triggerAmp(final String eventName, final Map<String, String> attributes)
    {
		final AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		handler.sendMessage(handler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, new Runnable() 
		{
			public void run()
			{
				handler.sendMessage(handler.obtainMessage(AmpSessionHandler.MESSAGE_TRIGGER_AMP, new Object[] { eventName, new TreeMap<String, String>(attributes) }));
			}
		}));
    }
	
	/**
	 * Attach the current foreground activity to the amp session. The fragment manager associated with each activity
	 * is needed for showing the dialog fragment which hosts the amp dialog. This method gets the fragment manager
	 * from the input activity and attaches it with the amp session handler which controls the amp trigger.
	 * <p>
     * Note: This method should be called before call to {@link #open()} or {@link #open(List)}
	 *  
	 * @param activity The foreground activity, must not be null.
	 */
	public void attach(final Activity activity)
	{
		if (activity == null)
        {
            throw new IllegalArgumentException("attached activity cannot be null"); //$NON-NLS-1$
        }
		
		AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		if (activity instanceof FragmentActivity)
		{
			handler.setFragmentManager(((FragmentActivity) activity).getSupportFragmentManager());
		}
		else
		{
			handler.setFragmentManager(null);
		}
	}
	
	/**
	 * Opposite from the attach, this method detaches any fragment manager from the amp session.
	 * <p>
     * Note: This method should be called after call to {@link #close()} or {@link #close(List)}
	 */
	public void detach()
	{		
		AmpSessionHandler handler = (AmpSessionHandler) getSessionHandler();
		handler.setFragmentManager(null);
	}
	
	public static boolean isTestModeEnabled()
	{
		return mTestModeEnabled.get();
	}
	
	public static void setTestMode(boolean enabled)
	{
		mTestModeEnabled.set(enabled);
	}
	
	/**
	 * Create the localytics directory in which the amp data should be saved.
	 *   
	 * @param context 
	 * @return true if the directory does exist or has been created, false if the directory cannot be created.
	 */
	private boolean createLocalyticsDirectory(final Context context) 
	{
		StringBuilder builder = new StringBuilder();
		
		if (LocalyticsAmpSession.USE_EXTERNAL_DIRECTORY)
		{
			builder.append(Environment.getExternalStorageDirectory().getAbsolutePath());			
		}
		else
		{
			builder.append(context.getFilesDir().getAbsolutePath());
		}	
		builder.append(File.separator);
		builder.append(LOCALYTICS_DIR);
		
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
     * @param apiKey Localytics API key. Cannot be null.
     * @param looper to run the Handler on. Cannot be null.
     */
	@Override
	protected SessionHandler createSessionHandler(final Context context, final String key, final Looper looper) 
    {
    	return new AmpSessionHandler(context, key, sSessionHandlerThread.getLooper());
    }		
}
