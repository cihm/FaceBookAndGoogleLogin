package com.example.test_fb_api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.*;
import com.facebook.model.*;
import com.facebook.AccessToken;

import com.facebook.widget.*;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.plus.PlusClient;




public class TestApi extends ActionBarActivity implements OnClickListener , ConnectionCallbacks, OnConnectionFailedListener {

	private static final String PERMISSION = "publish_actions";
	private Button postStatusUpdateButton;
	private Button postPhotoButton;
	private Button pickFriendsButton;
	private Button pickPlaceButton;
	private LoginButton loginButton;
	private ProfilePictureView profilePictureView;
	private TextView greeting;
	private PendingAction pendingAction = PendingAction.NONE;
	private ViewGroup controlsContainer;
	private GraphUser user;
	private GraphPlace place;
	private List<GraphUser> tags;
	private boolean canPresentShareDialog;
	private boolean canPresentShareDialogWithPhotos;
    
	
	//google
	private PlusClient mPlusClient;
	private SignInButton mSignInButton;
	private View mSignOutButton;
	private static final int REQUEST_CODE_RESOLVE_ERR = 9000;

    private ProgressDialog mConnectionProgressDialog;
    private ConnectionResult mConnectionResult;
    
    private static final int DIALOG_GET_GOOGLE_PLAY_SERVICES = 1;

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES = 2;
    
    private static final String TAG = "PlayHelloActivity";
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    public static final String EXTRA_ACCOUNTNAME = "extra_accountname";

    private TextView mOut;
	
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    
    
    private String mEmail;

    private Type requestType;

    public static String TYPE_KEY = "type_key";
    public static enum Type {FOREGROUND, BACKGROUND, BACKGROUND_WITH_SYNC}
    
    
	private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }
	
	private static final Location SEATTLE_LOCATION = new Location("") {
	        {
	            setLatitude(47.6097);
	            setLongitude(-122.3331);
	        }
	};//for pick place
	
    private UiLifecycleHelper uiHelper;
    
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    
    private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
        @Override
        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
            Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
        }

        @Override
        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            Log.d("HelloFacebook", "Success!");
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_api);
		
		
		mPlusClient = new PlusClient.Builder(this, this, this).setActions(
		        "http://schemas.google.com/AddActivity", "http://schemas.google.com/BuyActivity")
		        .setScopes(Scopes.PLUS_LOGIN) // Space separated list of scopes
		        .build();
		
		
		mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
	    mSignInButton.setOnClickListener(this);
		
	    // Progress bar to be displayed if the connection failure is not resolved.
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Signing in...");
        
        mSignOutButton = findViewById(R.id.sign_out_button);
        mSignOutButton.setOnClickListener(this);
        
        mOut = (TextView) findViewById(R.id.message);

        Bundle extras = getIntent().getExtras();
        //requestType = Type.valueOf(extras.getString(TYPE_KEY));
        //setTitle(getTitle() + " - " + requestType.name());
        //if (extras.containsKey(EXTRA_ACCOUNTNAME)) {
        //    mEmail = extras.getString(EXTRA_ACCOUNTNAME);
         //   new GetNameInForeground(TestApi.this, mEmail, SCOPE).execute();
        //}
        
        //==================================================================================================
        
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        
        
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.example.test_fb_api", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.e("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
        } catch (NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
        if (savedInstanceState != null) {
            String name = savedInstanceState.getString("test");
            pendingAction = PendingAction.valueOf(name);
        }

        loginButton = (LoginButton) findViewById(R.id.login_button);
        
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
            	//this method is get user data
            	
            	TestApi.this.user = user;
            	
                updateUI();
                // It's possible that we were waiting for this.user to be populated in order to post a
                // status update.
                handlePendingAction();
            }
        });

        profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
        greeting = (TextView) findViewById(R.id.greeting);

        postStatusUpdateButton = (Button) findViewById(R.id.postStatusUpdateButton);
        postStatusUpdateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostStatusUpdate();
            }
        });

        postPhotoButton = (Button) findViewById(R.id.postPhotoButton);
        postPhotoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostPhoto();
            }
        });

        pickFriendsButton = (Button) findViewById(R.id.pickFriendsButton);
        pickFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickFriends();
            }
        });

        pickPlaceButton = (Button) findViewById(R.id.pickPlaceButton);
        pickPlaceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickPlace();
            }
        });

        controlsContainer = (ViewGroup) findViewById(R.id.main_ui_container);

        final FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            // If we're being re-created and have a fragment, we need to a) hide the main UI controls and
            // b) hook up its listeners again.
            controlsContainer.setVisibility(View.GONE);
            if (fragment instanceof FriendPickerFragment) {
                setFriendPickerListeners((FriendPickerFragment) fragment);
            } else if (fragment instanceof PlacePickerFragment) {
                setPlacePickerListeners((PlacePickerFragment) fragment);
            }
        }

        // Listen for changes in the back stack so we know if a fragment got popped off because the user
        // clicked the back button.
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fm.getBackStackEntryCount() == 0) {
                    // We need to re-show our UI.
                    controlsContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        // Can we present the share dialog for regular links?
        canPresentShareDialog = FacebookDialog.canPresentShareDialog(this,
                FacebookDialog.ShareDialogFeature.SHARE_DIALOG);
        // Can we present the share dialog for photos?
        canPresentShareDialogWithPhotos = FacebookDialog.canPresentShareDialog(this,
                FacebookDialog.ShareDialogFeature.PHOTOS);
    
		/*
		  Session session = Session.getActiveSession();
		 // start Facebook Login
	    Session.openActiveSession(this, true, new Session.StatusCallback() {

	      // callback when session changes state
	      @Override
	      public void call(Session session, SessionState state, Exception exception) {
	        if (session.isOpened()) {

	          // make request to the /me API
	          Request.newMeRequest(session, new Request.GraphUserCallback() {

	            // callback after Graph API response with user object
	            @Override
	            public void onCompleted(GraphUser user, Response response) {
	              if (user != null) {
	                TextView welcome = (TextView) findViewById(R.id.textView1);
	                welcome.setText("Hello " + user.getName() + "!");
	              }
	            }
	          }).executeAsync();
	        }
	      }
	    });
	    
	    */
	    
		
	}

	
	@Override
	protected void onStop() {
	        super.onStop();
	        mPlusClient.disconnect();
	 }

	  
	@Override
	protected void onStart() {
	        super.onStart();
	        mPlusClient.connect();
	}
	
	@Override
	    public void onClick(View view) {
	      
		switch(view.getId()) {
	        
	            case R.id.sign_in_button:
	            	
	                int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	                Log.e("available: ", ""+available+"==="+ConnectionResult.SUCCESS );
	                
	                if (available != ConnectionResult.SUCCESS) {
	                    showDialog(DIALOG_GET_GOOGLE_PLAY_SERVICES);
	                    return;
	                }

	                if (mConnectionResult == null) {
	                   // mConnectionProgressDialog.show();
	                } else {
	                    try {
	                        mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
	                    } catch (SendIntentException e) {
	                        // Try connecting again.
	                        mConnectionResult = null;
	                        mPlusClient.connect();
	                    }
	                }
	                break;
	            case R.id.sign_out_button:
	                if (mPlusClient.isConnected()) {
	                    mPlusClient.clearDefaultAccount();
	                    mPlusClient.disconnect();
	                    mPlusClient.connect();
	                }
	                break;
//	            case R.id.revoke_access_button:
//	                if (mPlusClient.isConnected()) {
//	                    mPlusClient.revokeAccessAndDisconnect(this);
//	                    updateButtons(false /* isSignedIn */);
//	                }
//	                break;
	        }
	    }
	  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test_api, menu);
		return true;
	}
	
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);

        updateUI();
    }

    
    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be launched into.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

        outState.putString("test", pendingAction.name());
    }

    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//google
		if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
			if (resultCode == RESULT_OK) {
				mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				getUsername();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "You must pick an account",
						Toast.LENGTH_SHORT).show();
			}
		} else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR || requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
				&& resultCode == RESULT_OK) {
			handleAuthorizeResult(resultCode, data);
			return;
		}	
		//get user token need
		
	  super.onActivityResult(requestCode, resultCode, data);
	  
	  uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
	  
	  if (requestCode == REQUEST_CODE_RESOLVE_ERR && resultCode == RESULT_OK) {
	        mConnectionResult = null;
	        mPlusClient.connect();
	   }
	}
	
	
	private void handleAuthorizeResult(int resultCode, Intent data) {
		if (data == null) {
			show("Unknown error, click the button again");
			return;
		}
		if (resultCode == RESULT_OK) {
			Log.i(TAG, "Retrying");
			 new GetNameInForeground(TestApi.this, mEmail, SCOPE).execute();
			return;
		}
		if (resultCode == RESULT_CANCELED) {
			show("User rejected authorization.");
			return;
		}
		show("Unknown error, click the button again");
	}

	
	  /** Called by button in the layout */
    public void greetTheUser(View view) {
        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (statusCode == ConnectionResult.SUCCESS) {
            getUsername();
        } else if (GooglePlayServicesUtil.isUserRecoverableError(statusCode)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                    statusCode, this, 0 /* request code not used */);
            dialog.show();
        } else {
            Toast.makeText(this, "unrecoverable_error", Toast.LENGTH_SHORT).show();
        }
    }

    /** Attempt to get the user name. If the email address isn't known yet,
     * then call pickUserAccount() method so the user can pick an account.
     */
    private void getUsername() {
        if (mEmail == null) {
            pickUserAccount();
        } else {
            if (isDeviceOnline()) {
            	 new GetNameInForeground(TestApi.this, mEmail, SCOPE).execute();
            } else {
                Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    

    /** Starts an activity in Google Play Services so the user can pick an account */
    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    /** Checks whether the device currently has a network connection */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * This method is a hook for background threads and async tasks that need to update the UI.
     * It does this by launching a runnable under the UI thread.
     */
    public void show(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOut.setText(message);
            }
        });
    }

    
    
    
    
    public void handleException(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                    		TestApi.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    /**
     * Note: This approach is for demo purposes only. Clients would normally not get tokens in the
     * background from a Foreground activity.
     
    private AbstractGetNameTask getTask(
    		TestApi activity, String email, String scope) {
        switch(requestType) {
            case FOREGROUND:
                return new GetNameInForeground(activity, email, scope);
            case BACKGROUND:
                return new GetNameInBackground(activity, email, scope);
            case BACKGROUND_WITH_SYNC:
                return new GetNameInBackgroundWithSync(activity, email, scope);
            default:
                return new GetNameInBackground(activity, email, scope);
        }
    }
    */
    
    
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	        if (pendingAction != PendingAction.NONE &&
	                (exception instanceof FacebookOperationCanceledException ||
	                exception instanceof FacebookAuthorizationException)) {
	                new AlertDialog.Builder(this)
	                    .setTitle(R.string.cancelled)
	                    .setMessage(R.string.permission_not_granted)
	                    .setPositiveButton(R.string.ok, null)
	                    .show();
	            pendingAction = PendingAction.NONE;
	        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
	            handlePendingAction();
	        }
	        updateUI();
	}
	  
	
	
    private void onClickPickFriends() {
        final FriendPickerFragment fragment = new FriendPickerFragment();

        setFriendPickerListeners(fragment);

        showPickerFragment(fragment);
    }

    private void setFriendPickerListeners(final FriendPickerFragment fragment) {
        fragment.setOnDoneButtonClickedListener(new FriendPickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
                onFriendPickerDone(fragment);
            }
        });
    }

    private void onFriendPickerDone(FriendPickerFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();

        String results = "";

        List<GraphUser> selection = fragment.getSelection();
        tags = selection;
        if (selection != null && selection.size() > 0) {
            ArrayList<String> names = new ArrayList<String>();
            for (GraphUser user : selection) {
                names.add(user.getName());
            }
            results = TextUtils.join(", ", names);
        } else {
            results = getString(R.string.no_friends_selected);
        }

        showAlert(getString(R.string.you_picked), results);
    }

    
    private void showPickerFragment(PickerFragment<?> fragment) {
        fragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> pickerFragment, FacebookException error) {
                String text = "expection"+ error.getMessage();
                Toast toast = Toast.makeText(TestApi.this, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        controlsContainer.setVisibility(View.GONE);

        // We want the fragment fully created so we can use it immediately.
        fm.executePendingTransactions();

        fragment.loadData(false);
    }

    
    private void onPlacePickerDone(PlacePickerFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();

        String result = "";

        GraphPlace selection = fragment.getSelection();
        if (selection != null) {
            result = selection.getName();
        } else {
            result = getString(R.string.no_place_selected);
        }

        place = selection;

        showAlert(getString(R.string.you_picked), result);
    }

    private void onClickPickPlace() {
        final PlacePickerFragment fragment = new PlacePickerFragment();
        fragment.setLocation(SEATTLE_LOCATION);
        fragment.setTitleText(getString(R.string.pick_seattle_place));

        setPlacePickerListeners(fragment);

        showPickerFragment(fragment);
    }

    private void setPlacePickerListeners(final PlacePickerFragment fragment) {
        fragment.setOnDoneButtonClickedListener(new PlacePickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
                onPlacePickerDone(fragment);
            }
        });
        fragment.setOnSelectionChangedListener(new PlacePickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(PickerFragment<?> pickerFragment) {
                if (fragment.getSelection() != null) {
                    onPlacePickerDone(fragment);
                }
            }
        });
    }
    
    private void updateUI() {
        Session session = Session.getActiveSession();
       
        Log.e("Session", session.toString());
        boolean enableButtons = (session != null && session.isOpened());
        //App ID: 768407556549842
        postStatusUpdateButton.setEnabled(enableButtons || canPresentShareDialog);
        postPhotoButton.setEnabled(enableButtons || canPresentShareDialogWithPhotos);
        pickFriendsButton.setEnabled(enableButtons);
        pickPlaceButton.setEnabled(enableButtons);
       
        if (enableButtons && user != null) {
            profilePictureView.setProfileId(user.getId());//1496725350571636
            greeting.setText(getString(R.string.hello_user, user.getFirstName()));
            Log.e("user", user.toString());
        } else {
        	Log.e("user", "null");
        	profilePictureView.setProfileId(null);
            greeting.setText(null);
        }
    }

    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() {
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case POST_PHOTO:
                postPhoto();
                break;
            case POST_STATUS_UPDATE:
                postStatusUpdate();
                break;
        }
    }
    
    private void onClickPostPhoto() {
        performPublish(PendingAction.POST_PHOTO, canPresentShareDialogWithPhotos);
    }

    private FacebookDialog.PhotoShareDialogBuilder createShareDialogBuilderForPhoto(Bitmap... photos) {
        return new FacebookDialog.PhotoShareDialogBuilder(this)
                .addPhotos(Arrays.asList(photos));
    }
    
    
    private void performPublish(PendingAction action, boolean allowNoSession) {
        Session session = Session.getActiveSession();
        if (session != null) {
            pendingAction = action;
            if (hasPublishPermission()) {
                // We can do the action right away.
                handlePendingAction();
                return;
            } else if (session.isOpened()) {
                // We need to get new permissions, then complete the action when we get called back.
                session.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, PERMISSION));
                return;
            }
        }

        if (allowNoSession) {
            pendingAction = action;
            handlePendingAction();
        }
    }
    
    private void postPhoto() {
        Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher);
        if (canPresentShareDialogWithPhotos) {
            FacebookDialog shareDialog = createShareDialogBuilderForPhoto(image).build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } else if (hasPublishPermission()) {
            Request request = Request.newUploadPhotoRequest(Session.getActiveSession(), image, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    showPublishResult(getString(R.string.photo_post), response.getGraphObject(), response.getError());
                }
            });
            request.executeAsync();
        } else {
            pendingAction = PendingAction.POST_PHOTO;
        }
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }
    
    private void showPublishResult(String message, GraphObject result, FacebookRequestError error) {
        String title = null;
        String alertMessage = null;
        if (error == null) {
            title = getString(R.string.success);
            String id = result.cast(GraphObjectWithId.class).getId();
            alertMessage = getString(R.string.successfully_posted_post, message, id);
        } else {
            title = getString(R.string.error);
            alertMessage = error.getErrorMessage();
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(alertMessage)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private interface GraphObjectWithId extends GraphObject {
        String getId();
    }

    
    private void onClickPostStatusUpdate() {
        performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
    }

    private FacebookDialog.ShareDialogBuilder createShareDialogBuilderForLink() {
        return new FacebookDialog.ShareDialogBuilder(this)
                .setName("Hello Facebook")
                .setDescription("The 'Hello Facebook' sample application showcases simple Facebook integration")
                .setLink("http://developers.facebook.com/android");
    }

    private void postStatusUpdate() {
        if (canPresentShareDialog) {
            FacebookDialog shareDialog = createShareDialogBuilderForLink().build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } else if (user != null && hasPublishPermission()) {
            final String message = getString(R.string.status_update, user.getFirstName(), (new Date().toString()));
            Request request = Request
                    .newStatusUpdateRequest(Session.getActiveSession(), message, place, tags, new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            // change message here
                        	showPublishResult(message, response.getGraphObject(), response.getError());
                        }
                    });
            request.executeAsync();
        } else {
            pendingAction = PendingAction.POST_STATUS_UPDATE;
        }
    }


    
    /*
     * (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener#onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     	google+ api test 
     */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		  if (mConnectionProgressDialog.isShowing()) {
              // The user clicked the sign-in button already. Start to resolve
              // connection errors. Wait until onConnected() to dismiss the
              // connection dialog.
              if (result.hasResolution()) {
                      try {
                              result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                      } catch (SendIntentException e) {
                              mPlusClient.connect();
                      }
              }
      }

      // Save the intent so that we can start an activity when the user clicks
      // the sign-in button.
      mConnectionResult = result;
      
    
      updateButtons(false /* isSignedIn */);
		
	}


	@Override
	public void onConnected(Bundle connectionHint) {
		// TODO Auto-generated method stub
		 mConnectionProgressDialog.dismiss();
		 String accountName = mPlusClient.getAccountName();
	     Log.e("connectionHint", mPlusClient.getAccountName()+"==="+mPlusClient.getCurrentPerson().toString());
	     
	     updateButtons(true /* isSignedIn */);
	     
	}


	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	        mPlusClient.connect();
	        updateButtons(false /* isSignedIn */);
	}

    private void updateButtons(boolean isSignedIn) {
        if (isSignedIn) {
            mSignInButton.setVisibility(View.INVISIBLE);
            mSignOutButton.setEnabled(true);
        
        } else {
            if (mConnectionResult == null) {
                // Disable the sign-in button until onConnectionFailed is called with result.
                mSignInButton.setVisibility(View.INVISIBLE);
              
            } else {
                // Enable the sign-in button since a connection result is available.
                mSignInButton.setVisibility(View.VISIBLE);
          
            }

            mSignOutButton.setEnabled(false);
         
        }
    }

}
