package com.localytics.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.localytics.android.LocalyticsProvider.AmpRulesDbColumns;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to handle amp dialog work for the in-app message.
 */
/* package */public class AmpDialogFragment extends DialogFragment 
{
	public static final String DIALOG_TAG = "amp_dialog";
	
	private Map<String, Object> mAmpMessage;
	
	private JavaScriptAPI mJavaScriptAPI;
	
	private AmpDialogCallback mCallback;
	
	private AtomicBoolean mUploadedViewEvent;
	
	/**
	 * If the dialog is shown, it should not shown with animation again when the phone rotates.
	 * This flag indicates whether the dialog is shown with animation before.
	 */
	private AtomicBoolean mEnterAnimatable;
	
	/**
	 * This element prevents the dismiss animation from being shown multiple times	 
	 */
	private AtomicBoolean mExitAnimatable;
	
	public AmpDialogFragment()
	{
		mEnterAnimatable = new AtomicBoolean(true);
		mExitAnimatable  = new AtomicBoolean(true);
		mUploadedViewEvent = new AtomicBoolean(false);
	}	
	
	@Override
	public void onActivityCreated(Bundle arg0) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onActivityCreated");
        }
		super.onActivityCreated(arg0);
	}

	@Override
	public void onAttach(Activity activity) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onAttach");
        }
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onCreate");
        }
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDetach() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onDetach");
        }
		super.onDetach();
	}

	@Override
	public void onDismiss(DialogInterface dialog) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onDismiss");
        }
		tagAmpActionEventWithAction(AmpConstants.ACTION_DISMISS);
		super.onDismiss(dialog);
	}

	@Override
	public void onStart() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onStart");
        }
		super.onStart();
	}
	
	@Override
	public void onPause() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onPause");
        }
		super.onPause();
	}

	@Override
	public void onResume() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onResume");
        }
		super.onResume();
	}

	@Override
	public void onDestroy() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onDestroy");
        }
		if (null != mCallback)
		{
//			mCallback.onAmpDestroy(mAmpMessage);
		}
		super.onDestroy();
	}

	@Override
	public void onStop() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onStop");
        }
		super.onStop();
	}

	@Override
	public void onViewStateRestored (Bundle savedInstanceState) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onViewStateRestored");
        }
		super.onViewStateRestored(savedInstanceState);
	}
	
	@Override
	public void onSaveInstanceState(Bundle arg0) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onSaveInstanceState");
        }
		super.onSaveInstanceState(arg0);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onCreateView");
        }
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	/**
     * Retrieve an instance of AmpDialogFragment, create a new one if it doesn't exist
     */
    public static AmpDialogFragment newInstance() 
    {
    	AmpDialogFragment fragment = new AmpDialogFragment();
    	fragment.setRetainInstance(true);
        return fragment;
    }	

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onCreateDialog");
        }
		return new AmpDialog(getActivity(), android.R.style.Theme_Dialog);				
    }
	
	@Override
	public void onDestroyView() 
	{
		if (Constants.IS_LOGGABLE)
        {
			Log.w("AmpDialogFragment", "onDestroyView");
        }
		if (getDialog() != null && getRetainInstance())
		{
			getDialog().setOnDismissListener(null);
		}
		super.onDestroyView();
	}		

	public AmpDialogFragment setData(Map<String, Object> ampMessage)
	{
		mAmpMessage = ampMessage;
		return this;
	}
	
	public AmpDialogFragment setJavaScriptAPI(final JavaScriptAPI javaScriptAPI)
	{
		mJavaScriptAPI = javaScriptAPI;
		return this;
	}
	
	public AmpDialogFragment setOnAmpDestroyListener(final AmpDialogCallback callback)
	{
		mCallback = callback;
		return this;
	}
	
	private void tagAmpActionForURL(URL url)
    {
    	// Check if there is an ampAction available in the query string and tag it appropriately
    	final String ampActionValue = getValueByQueryKey(AmpConstants.AMPACTION_STRING, url);
    	if (!TextUtils.isEmpty(ampActionValue))
    	{
    		if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, String.format("Attempting to tag event with custom AMP action.[Action: %s]", ampActionValue)); //$NON-NLS-1$
            }
    		tagAmpActionEventWithAction(ampActionValue);
    	}
    	// Otherwise check if this is a "click" event. We define this as any navigation that
    	// is not part of the original creative
    	else
    	{
    		final String protocol = url.getProtocol();
    		if (!protocol.equals(AmpConstants.PROTOCOL_FILE) && !protocol.equals(AmpConstants.PROTOCOL_HTTP) && !protocol.equals(AmpConstants.PROTOCOL_HTTPS))
    		{
    			tagAmpActionEventWithAction(AmpConstants.ACTION_CLICK);
    		}
    	}
    }

    private void tagAmpActionEventWithAction(String action)
    {
    	if (mUploadedViewEvent.getAndSet(true))
    	{
    		if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, String.format("The AMP action for this message has already been set. Ignoring AMP Action: [%s]", action)); //$NON-NLS-1$                
            }
    		return;
    	}
		
		// Prepare the attributes and custom dimension with campaign_id and abtest pairing    	
    	TreeMap<String, String> attributes = new TreeMap<String, String>(); 
    	attributes.put(AmpConstants.AMP_ACTION_KEY, action);
    	attributes.put(AmpConstants.AMP_CAMPAIGN_ID_KEY, mAmpMessage.get(AmpRulesDbColumns.CAMPAIGN_ID).toString());
    	attributes.put(AmpConstants.AMP_CAMPAIGN_KEY, mAmpMessage.get(AmpRulesDbColumns.RULE_NAME).toString());
		
    	String ab = (String) mAmpMessage.get(AmpRulesDbColumns.AB_TEST);
    	if (!TextUtils.isEmpty(ab))
    	{
    		attributes.put(AmpConstants.AMP_AB_KEY, ab);
    	}
	
		if (!LocalyticsAmpSession.isTestModeEnabled())
		{
			// Tag event with attributes & reporting attributes
			if (null != mCallback)
			{
				mCallback.onTagAmpActionEvent(AmpConstants.AMP_EVENT_NAME_KEY, attributes);			
			}
			
			if (Constants.IS_LOGGABLE)
            {
				final StringBuilder builder = new StringBuilder();
				for (Map.Entry<String, String> entry : attributes.entrySet()) 
				{ 
					builder.append("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
				}
                Log.w(Constants.LOG_TAG, String.format("AMP event tagged successfully.\n   Attributes Dictionary = \n%s", builder.toString())); //$NON-NLS-1$
            }
		}    	
    }		    
    
    private String getValueByQueryKey(final String queryKey, final URL url)
    {		      
    	final String query = url.getQuery();
    	
    	if (TextUtils.isEmpty(queryKey) || TextUtils.isEmpty(query))
    	{
    		return null;
    	}
    		
        final String[] pairs = url.getQuery().split("[&]");
        for (final String pair : pairs)
        {
            final String[] components = pair.split("[=]"); 
            if (components[0].compareTo(queryKey) == 0)
            {
            	if (2 == components.length)
            	{
            		try 
            		{
						return URLDecoder.decode(components[1], "UTF-8");
					} 
            		catch (UnsupportedEncodingException e) 
            		{
						return null;
					}
            	}
            }
        }
        
        return null;
    }
	
	/**
	 * Helper class to generate the amp dialog.
	 */
	/* package */class AmpDialog extends Dialog
	{
		/**
		 * Center dialog
		 */
		private final static String LOCATION_CENTER = "center";
		
		/**
		 * Fullscreen dialog
		 */
		private final static String LOCATION_FULL = "full";
		
		/**
		 * Top banner
		 */
		private final static String LOCATION_TOP = "top";
		
		/**
		 * Bottom banner
		 */
		private final static String LOCATION_BOTTOM = "bottom";
		
		/**
		 * Margin around the dialog in center mode
		 */
		private final static int MARGIN = 10; // in dip
		
		/**
		 * Web view to hold the HTML content
		 */
		private WebView mWebView;
		
		/**
		 * Dismiss button on the top left of the dialog
		 */
		private DismissButton mBtnDismiss;
		
		/**
		 * Display metrics from which we can get the current window size
		 */
		private DisplayMetrics mMetrics;
		
		/**
		 * Display width from amp message
		 */
		private float mWidth;
		
		/**
		 * Display height from amp message
		 */
		private float mHeight;
		
		/**
		 * The root layout of the amp dialog
		 */
		private RelativeLayout mRootLayout;
		
		/**
		 * The container layout for hold the webview and dismiss button
		 */
		private RelativeLayout mDialogLayout;
		
		/**
		 * The in/out animations for the amp dialog 
		 */
		private TranslateAnimation mAnimCenterIn;
		private TranslateAnimation mAnimCenterOut;
		private TranslateAnimation mAnimTopIn;
		private TranslateAnimation mAnimTopOut;
		private TranslateAnimation mAnimBottomIn;
		private TranslateAnimation mAnimBottomOut;
		private TranslateAnimation mAnimFullIn;
		private TranslateAnimation mAnimFullOut;	
		
		/**
		 * Location in which to place the dialog
		 */
		private String mLocation;
	
		/**
		 * The pop-up dialog that shows the campaign information.
		 * 
		 * @param context An activity context with which to pop up the dialog
		 * @param theme The them of the dialog
		 */
		public AmpDialog(Context context, int theme)
		{
			super(context, theme);						
			
			// When user switches from the other app, the amp message here is invalid.
			if (null == mAmpMessage)
			{				
				AmpDialogFragment.this.dismiss();
				return;
			} 
			mLocation = (String) mAmpMessage.get(AmpRulesDbColumns.LOCATION);
			
			setupViews();
			createAnimations();
			adjustLayout();
			
			/**
			 *  Load HTML content into the web view.			 
			 *  Use loadUrl because it saves the memory by eliminating the process of reading HTML into the String,
			 *  which can easily lead to OutOfMemoryException on certain creepy devices.
			 */			
			final String htmlUrl = (String) mAmpMessage.get("html_url");
			if (null != htmlUrl)
			{
				mWebView.loadUrl(htmlUrl);
			}
		}					

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) 
		{
			if (keyCode == KeyEvent.KEYCODE_BACK) 
			{
				if (mExitAnimatable.getAndSet(false))
				{
					dismissWithAnimation();
				}
				return true;
			}
			return super.onKeyDown(keyCode, event);
		}

		@Override
		protected void onStop() 
		{
			if (null != mBtnDismiss) 
			{
				mBtnDismiss.release();
			}
			super.onStop();
		}

		private void setupViews()
		{				
			// Root layout
			mRootLayout = new RelativeLayout(getContext());			
			mRootLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
			
			// Dialog layout (holding dismiss button and webview layout)
			mDialogLayout = new RelativeLayout(getContext());
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.CENTER_IN_PARENT);
			mDialogLayout.setLayoutParams(params);
			mRootLayout.addView(mDialogLayout);
			
			// Create and add the web view dynamically
			mWebView = new AmpWebView(getContext(), null);		
			mDialogLayout.addView(mWebView);
						
			// Create and add the dismiss button dynamically
			mBtnDismiss = new DismissButton(getContext(), null);
			mDialogLayout.addView(mBtnDismiss);						
			
			mBtnDismiss.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{					
					if (mExitAnimatable.getAndSet(false))
					{
						dismissWithAnimation();
					}
				}
			});
			
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			
			setContentView(mRootLayout);
		}
		
		private void adjustLayout()
		{
			// Get screen size
			mMetrics = new DisplayMetrics();
	        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
	        windowManager.getDefaultDisplay().getMetrics(mMetrics);  			
 			
 			// Get amp dialog display size (in dip)
	        mWidth  = (Float) mAmpMessage.get("display_width");
 			mHeight = (Float) mAmpMessage.get("display_height"); 	
 			
 			// Get display dimension based on the device type
 			final float aspectRatio = mHeight / mWidth;
 			final float maxWidth = Math.min(mMetrics.widthPixels, mMetrics.heightPixels);
	        
			/**
			 *  Set display size and location according to the amp message
			 */
	        final Window window = getWindow();		
	        final WindowManager.LayoutParams attributes = window.getAttributes();			
			
			window.setBackgroundDrawable(new ColorDrawable(0)); // Set transparent background		
			window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL); // Enable background clickable
			
			// Dialog in the center screen
			if (mLocation.equals(LOCATION_CENTER))
			{			
				// Set dialog size (for center dialog size, the margin must be taken into account for displaying the dismiss button)
				window.setLayout(mMetrics.widthPixels, mMetrics.heightPixels);
				// Set the margin to place the dismiss button out of dialog's visible boundary
				final int margin = (int) (MARGIN * mMetrics.density + 0.5f);
				ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mWebView.getLayoutParams();
				params.width  = (int) Math.min(maxWidth - (margin << 1), (int) (mWidth * mMetrics.density + 0.5f)) + (margin << 1);
				params.height = (int) (Math.min(maxWidth - (margin << 1), (int) (mWidth * mMetrics.density + 0.5f)) * aspectRatio) + (margin << 1);
				params.setMargins(margin, margin, margin, margin);
				mWebView.requestLayout();								
				mWebView.setLayoutParams(params);
			}
			// Dialog occupying the whole screen
			else if (mLocation.equals(LOCATION_FULL))
			{			
		     	// Set dialog size
		        window.setLayout(mMetrics.widthPixels, mMetrics.heightPixels);
			}
			// Banner on top
			else if (mLocation.equals(LOCATION_TOP))
			{
				attributes.y = -0xFFFFFFF;
				attributes.dimAmount = 0f;								
		     	// Set dialog size
		        window.setLayout((int) maxWidth, (int) (maxWidth * aspectRatio + 0.5f));	        
			}
			// Banner on bottom
			else if (mLocation.equals(LOCATION_BOTTOM))
			{			
				attributes.y = 0xFFFFFFF;
				attributes.dimAmount = 0f;
		     	// Set dialog size
				window.setLayout((int) maxWidth, (int) (maxWidth * aspectRatio + 0.5f));
			}
			
			// Set display animation
			if (mEnterAnimatable.getAndSet(false))
			{
				enterWithAnimation();
			}
			
			// Prevent the top dialog from being cut off after KitKat
			window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}	
		
		/**
		 * Create all amp dialog animations
		 */
		private void createAnimations()
		{
			mAnimCenterIn  = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_PARENT, 0);
			mAnimCenterIn.setDuration(500);			
			mAnimCenterOut = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_PARENT, 1);
			mAnimCenterOut.setDuration(500);
			
			mAnimTopIn  = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, -1, Animation.RELATIVE_TO_PARENT, 0);
			mAnimTopIn.setDuration(500);
			mAnimTopOut = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, -1);
			mAnimTopOut.setDuration(500);
			
			mAnimBottomIn  = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 1, Animation.RELATIVE_TO_PARENT, 0);
			mAnimBottomIn.setDuration(500);
			mAnimBottomOut = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 1);
			mAnimBottomOut.setDuration(500);
			
			mAnimFullIn  = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 1, Animation.RELATIVE_TO_PARENT, 0);
			mAnimFullIn.setDuration(500);			
			mAnimFullOut = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 1);
			mAnimFullOut.setDuration(500);
			
			AnimationListener listenerOut = new AnimationListener() 
			{
				public void onAnimationEnd(Animation animation) { AmpDialogFragment.this.dismiss(); }

				public void onAnimationRepeat(Animation animation) {}

				public void onAnimationStart(Animation animation) {}
			};
			mAnimCenterOut.setAnimationListener(listenerOut);
			mAnimTopOut.setAnimationListener(listenerOut);
			mAnimBottomOut.setAnimationListener(listenerOut);
			mAnimFullOut.setAnimationListener(listenerOut);
		}
		
		/**
		 * Set the dialog pop-up animation
		 */
		public void enterWithAnimation()
		{
			final String location = (String) mAmpMessage.get(AmpRulesDbColumns.LOCATION);
			
			// Dialog in the center screen
			if (location.equals(LOCATION_CENTER)) 
			{			
				mRootLayout.startAnimation(mAnimCenterIn);
			}
			// Dialog occupying the whole screen
			else if (location.equals(LOCATION_FULL))
			{			
				mRootLayout.startAnimation(mAnimFullIn);
			}
			// Banner on top
			else if (location.equals(LOCATION_TOP))
			{
				mRootLayout.startAnimation(mAnimTopIn);
			}
			// Banner on bottom
			else if (location.equals(LOCATION_BOTTOM))
			{			
				mRootLayout.startAnimation(mAnimBottomIn);
			}
		}
		
		/**
		 * Set the dialog dismiss animation. When the animation is over, 
		 * the animation listener registered before will dismiss the dialog.
		 */
		public void dismissWithAnimation()
		{
			final String location = (String) mAmpMessage.get(AmpRulesDbColumns.LOCATION);
			
			// Dialog in the center screen
			if (location.equals(LOCATION_CENTER)) 
			{			
				mRootLayout.startAnimation(mAnimCenterOut);
			}
			// Dialog occupying the whole screen
			else if (location.equals(LOCATION_FULL))
			{			
				mRootLayout.startAnimation(mAnimFullOut);
			}
			// Banner on top
			else if (location.equals(LOCATION_TOP))
			{
				mRootLayout.startAnimation(mAnimTopOut);
			}
			// Banner on bottom
			else if (location.equals(LOCATION_BOTTOM))
			{			
				mRootLayout.startAnimation(mAnimBottomOut);
			}
		}

		/**
		 * Helper class to handle the in app campaign message displaying in android web view.
		 */
		/* package */class AmpWebView extends WebView 
		{			
		    @SuppressLint("SetJavaScriptEnabled")
			public AmpWebView(Context context, AttributeSet attrs) 
		    {
				super(context, attrs);
				
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
				params.gravity = Gravity.CENTER;
				setLayoutParams(params);
				
				setBackgroundColor(0);
				setInitialScale(1);
				setHorizontalScrollBarEnabled(false);
				setVerticalScrollBarEnabled(false);
				
		    	setWebViewClient(new AmpWebViewClient());
		    	
		    	WebSettings settings = getSettings();
		    	settings.setJavaScriptEnabled(true);
		    	addJavascriptInterface(mJavaScriptAPI, "localytics");
		    	settings.setUseWideViewPort(true); // Enable 'viewport' meta tag		    	
		    }				    		    
		    
		    public class AmpWebViewClient extends WebViewClient
	    	{
	    		@Override  
	    	    public void onPageFinished(WebView view, String url)  
	    	    {  
	    			final String location = (String) mAmpMessage.get(AmpRulesDbColumns.LOCATION);
	    			final int margin = location.equals(LOCATION_CENTER) ? (int) (MARGIN * mMetrics.density + 0.5f) << 1 : 0;
	    			final int maxWidth  = Math.min(mMetrics.widthPixels, mMetrics.heightPixels) - margin; 
	    			final int maxHeight = Math.max(mMetrics.widthPixels, mMetrics.heightPixels) - margin;
	    			final float viewportWidth  = Math.min(maxWidth, (int) (mWidth * mMetrics.density + 0.5f)) / mMetrics.density;
	    			final float viewportHeight = Math.min(maxHeight, (int) (mHeight * mMetrics.density + 0.5f)) / mMetrics.density;
	    			
	    	        view.loadUrl(String.format("javascript:(" + 
	    	        	"function() {" +  
	    	        	"  var viewportNode = document.createElement('meta');" +
	    	        	"  viewportNode.name    = 'viewport';" +
	    	        	"  viewportNode.content = 'width=%f, height=%f, user-scalable=no, minimum-scale=.25, maximum-scale=1';" +
	    	        	"  viewportNode.id      = 'metatag';" +
	    	        	"  document.getElementsByTagName('head')[0].appendChild(viewportNode);" +
    	                "})()", viewportWidth, viewportHeight
	    	        ));
	    	        
	    	        view.loadUrl(mJavaScriptAPI.getJsGlueCode());
	    	    } 
	    		
	    		@Override
	    		public boolean shouldOverrideUrlLoading(WebView view, String url)
	    		{
	    			if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, String.format("[AMP Nav Handler]: Evaluating AMP URL:\n\tURL:%s", url)); //$NON-NLS-1$
                    }
	    			
	    			int result = 0;
	    			try 
	    			{
						URL aURL = new URL(url);
						// Check URL for ampActions and tag them appropriately		    			
		    			tagAmpActionForURL(aURL);
		    			
		    			
		    			// If appropriate, handles navigation to the local creative files
		    			if ((result = handleFileProtocolRequest(aURL)) > 0)
		    			{
		    				return result == OPENING_EXTERNAL;
		    			}
		    			
		    			// If appropriate, handles navigation to HTTP resources (both within and outside the AMP view)
		    			if ((result = handleHttpProtocolRequest(aURL)) > 0)
		    			{
		    				return result == OPENING_EXTERNAL;
		    			}
		    			
		    			// If appropriate, handles custom protocols which this app/other apps may be registered to handle
		    			if ((result = handleCustomProtocolRequest(aURL)) > 0)
		    			{
		    				return result == OPENING_EXTERNAL;
		    			}
		    			
		    			if (Constants.IS_LOGGABLE)
	                    {
	                        Log.w(Constants.LOG_TAG, String.format("[AMP Nav Handler]: Protocol handler scheme not recognized. Attempting to load the URL... [Scheme: %s]", aURL.getProtocol())); //$NON-NLS-1$
	                    }
					} 
	    			catch (MalformedURLException e) 
	    			{
	    				if (Constants.IS_LOGGABLE)
	                    {
	                        Log.w(Constants.LOG_TAG, String.format("[AMP Nav Handler]: Invalid url %s", url)); //$NON-NLS-1$
	                    }
					}
	    			finally
	    			{
	    				if (result == OPENING_EXTERNAL)
	    				{
	    					//AmpDialogFragment.this.dismiss();
	    				}
	    			}
	    			
	    			return false; // load within the creative
	    		}
	    		
	    		/**
	    		 * Return values from protocol handlers below
	    		 */
	    		private static final int PROTOCOL_UNMATCHED    = -1;
			    private static final int PROTOCOL_UNRECOGNIZED = -2;
			    private static final int OPENING_INTERNAL      = 1;
			    private static final int OPENING_EXTERNAL      = 2;
			    
			    private int handleFileProtocolRequest(final URL url)
			    {
			    	if (!(url.getProtocol().equals(AmpConstants.PROTOCOL_FILE)))
			    	{
			    		return PROTOCOL_UNMATCHED;
			    	}
			    	
			    	if (Constants.IS_LOGGABLE)
	                {
	                    Log.w(Constants.LOG_TAG, String.format("[AMP Nav Handler]: Displaying content from your local creatives.")); //$NON-NLS-1$
	                }
			    	
			    	return OPENING_INTERNAL;
			    }

			    private int handleHttpProtocolRequest(final URL url)
			    {
			    	final String protocol = url.getProtocol();
			    	if (!protocol.equals(AmpConstants.PROTOCOL_HTTP) && !protocol.equals(AmpConstants.PROTOCOL_HTTPS))
			    	{
			    		return PROTOCOL_UNMATCHED;
			    	}
			    	
			    	if (Constants.IS_LOGGABLE)
	                {
	                    Log.w(Constants.LOG_TAG, "[AMP Nav Handler]: Handling a request for an external HTTP address."); //$NON-NLS-1$
	                }
			    	
			    	// Open the content in chrome if the ampExternalOpen query string hook is present
			    	final String openExternalValue = getValueByQueryKey(AmpConstants.OPEN_EXTERNAL, url);
			    	if (!TextUtils.isEmpty(openExternalValue) && openExternalValue.toLowerCase(Locale.US).equals("true"))
			    	{
			    		if (Constants.IS_LOGGABLE)
		                {
		                    Log.w(Constants.LOG_TAG, String.format("[AMP Nav Handler]: Query string hook [%s] set to true. Opening the URL in chrome", AmpConstants.OPEN_EXTERNAL)); //$NON-NLS-1$
		                }
			    		
			    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
				    	List<ResolveInfo> activities = getContext().getPackageManager().queryIntentActivities(intent, 0);
				    	if (activities.size() > 0)
				    	{
				    		startActivity(intent);
				    		return OPENING_EXTERNAL;
				    	}
			    	}
			    	
			    	// Otherwise open it inside the current view
		    		if (Constants.IS_LOGGABLE)
	                {
	                    Log.w(Constants.LOG_TAG, "[AMP Nav Handler]: Loading HTTP request inside the current AMP view"); //$NON-NLS-1$
	                }
		    		
		    		return OPENING_INTERNAL;
			    }

			    private int handleCustomProtocolRequest(final URL url)
			    {
			    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
			    	List<ResolveInfo> activities = getContext().getPackageManager().queryIntentActivities(intent, 0);
			    	if (activities.size() > 0)
			    	{
			    		if (Constants.IS_LOGGABLE)
		                {
		                    Log.w(Constants.LOG_TAG, "[AMP Nav Handler]: An app on this device is registered to handle this protocol scheme. Opening..."); //$NON-NLS-1$
		                }
			    		startActivity(intent);
			    		return OPENING_EXTERNAL;
			    	}
			    	
			    	return PROTOCOL_UNRECOGNIZED;
			    }			    			   
			}		    
		}
		
		/**
		 * Helper class to generate the dismiss button on the top-left side of the amp dialog.
		 */
		/* package */class DismissButton extends View
		{		
			// Paint for drawing the button
			private Paint mPaint;
			
			// Paint for drawing the shadow layer behind the button
			private Paint mShadowInnerPaint;
			private Paint mShadowOuterPaint;
			
			// Center of the button circle
			private float mCenterX;
			private float mCenterY;
			
			// Radius of the button outer circle
			private float mRadius;
			
			// Offset of the cross line in the circle for the starting and ending points
			private float mOffset;
			
			// Stroke with of the cross lines in the circle
			private float mStrokeWidth;
			
			// Radius of the button inner circle
			private float mInnerRadius;	
			
			// Bitmap of the button
			private Bitmap mBitmap;
			
			// Bit map of the shadow behind the button
			private Bitmap mShadowBitmap;
			
			public DismissButton(Context context, AttributeSet attrs) 
			{
				super(context, attrs);
				
				final float dip = getResources().getDisplayMetrics().density;
				
				mCenterX     = 13 * dip; 
				mCenterY     = 13 * dip;
				mRadius      = 13 * dip;
				mOffset      = 5 * dip;
				mStrokeWidth = 2.5f * dip;
				mInnerRadius = mRadius - mStrokeWidth * 0.5f;
							
				mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				
				mShadowInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				mShadowInnerPaint.setMaskFilter(new BlurMaskFilter(mRadius - dip, Blur.INNER));
				mShadowOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				mShadowOuterPaint.setMaskFilter(new BlurMaskFilter(2 * dip, Blur.OUTER));												
				
				setLayoutParams(new RelativeLayout.LayoutParams((int) (30 * dip + 0.5f), (int) (30 * dip + 0.5f)));
				
				// Create the button bitmap and the canvas in which the button bitmap will be drawn
				mBitmap = Bitmap.createBitmap((int) (26 * dip + 0.5f), (int) (26 * dip + 0.5f), Config.ARGB_8888);				
				Canvas canvas = new Canvas(mBitmap);
				
				// Draw the outer circle
				mPaint.setColor(Color.BLACK);
		        mPaint.setStyle(Style.FILL);
		        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
				
				// Draw the inner circle
				mPaint.setColor(Color.WHITE);
				mPaint.setStyle(Style.STROKE);
				mPaint.setStrokeWidth(mStrokeWidth);
				canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mPaint);
				
				// Draw the cross lines in the circle
				mPaint.setStrokeWidth(4.5f * dip);
				canvas.drawLine(mCenterX - mOffset, mCenterY - mOffset, mCenterX + mOffset, mCenterY + mOffset, mPaint);
				canvas.drawLine(mCenterX - mOffset, mCenterY + mOffset, mCenterX + mOffset, mCenterY - mOffset, mPaint);				
			}
	
			@Override
			protected void onDraw(Canvas canvas) 
			{
				super.onDraw(canvas);
				
				final float dip = getResources().getDisplayMetrics().density;

				canvas.drawCircle(mCenterX + dip, mCenterY + dip, mRadius - dip, mShadowInnerPaint);
				canvas.drawCircle(mCenterX + dip, mCenterY + dip, mRadius - dip, mShadowOuterPaint);
				canvas.drawBitmap(mBitmap, 0, 0, mPaint);										
			}

			public void release()
			{
				if (null != mBitmap)
				{
					mBitmap.recycle();
					mBitmap = null;
				}
			}
		}
	}
	
	/**
	 * This callback interface is implemented by AmpSessionHandler
	 */
	interface AmpDialogCallback
	{
		void onAmpDestroy(final Map<String, Object> ampMessage);
		void onTagAmpActionEvent(final String event, final Map<String, String> attributes);
	}
	
	/**
	 * JavaScript API interface which is implemented by JavaScriptClient
	 */
	interface JavaScriptAPI
	{
		public String getJsGlueCode();
		public void tagEventNative(String event, String attributes, String customDimensions, long customerValueIncrease);
		public void closeNative();
	}
	
}
