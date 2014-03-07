package com.localytics.android;

/**
 * Build constants for the Localytics library.
 * <p>
 * This is not a public API.
 */
/* package */final class AmpConstants
{
	/**
	 * AMP display events
	 */
	public static final String AMP_START_TRIGGER     = "AMP Loaded";
	public static final String AMP_FIRST_RUN_TRIGGER = "AMP First Run";
	public static final String AMP_UPGRADE_TRIGGER   = "AMP upgrade";	
	
	/**
	 * For use in only dictionary data passed between the DB and the view
	 */
	public static final String AMP_KEY = "amp";
	public static final String KEY_HTML_URL = "html_url";
	public static final String KEY_BASE_PATH = "base_path";
	public static final String KEY_DISPLAY_WIDTH = "display_width";
	public static final String KEY_DISPLAY_HEIGHT = "display_height";	
	public static final String CONDITIONS_KEY = "conditions";
	public static final String DISPLAY_EVENTS_KEY = "display_events";
	public static final String PHONE_SIZE_KEY  = "phone_size";
	public static final String TABLET_SIZE_KEY = "tablet_size";
	public static final String WIDTH_KEY  = "width";
	public static final String HEIGHT_KEY = "height";	
	
	/**
	 * Default name for decompressed html
	 */
	public static final String DEFAULT_ZIP_PAGE = "index.html";
	
	/**
	 * Device to show amp message
	 */
	public static final String DEVICE_TABLET = "tablet";
	public static final String DEVICE_PHONE  = "phone";
	public static final String DEVICE_BOTH   = "both";	
	
	/**
	 *  Amp url values
	 */		
	public static final String AMPACTION_STRING = "ampAction";
	public static final String ADID_STRING      = "adid";
	public static final String OPEN_EXTERNAL    = "ampExternalOpen";
	
	/**
	 *  ampEvent tag attribute keys
	 */
	public static final String AMP_ACTION_KEY      = "ampAction";
	public static final String AMP_CAMPAIGN_KEY    = "ampCampaign";
	public static final String AMP_CAMPAIGN_ID_KEY = "ampCampaignId";
	public static final String AMP_DURATION_KEY    = "ampDuration";
	public static final String AMP_AB_KEY          = "ampAB";
	public static final String AMP_EVENT_NAME_KEY  = "ampView";
	
	public static final String ACTION_DISMISS = "X";
	public static final String ACTION_CLICK   = "click";		
	
	/**
	 * Protocol types
	 */
	public static final String PROTOCOL_FILE  = "file";
	public static final String PROTOCOL_HTTP  = "http";
	public static final String PROTOCOL_HTTPS = "https";
}
