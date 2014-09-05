package com.example.videofly;


import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

public class VideoFlyService  extends Service implements 
Session.SessionListener, Publisher.PublisherListener,
Subscriber.VideoListener {

	private static final String LOGTAG = "VideoFlyService";
	private WindowManager windowManager;

	//RelativeLayouts 
	private RelativeLayout videoFlyHead; //Root Layout
	private RelativeLayout mPublisherViewContainer;  //PublisherLayout
	private RelativeLayout mSubscriberViewContainer; //SubscriberLayout
	private LinearLayout mViewGroupLayout;
	private ImageButton endCallButton;

	//Variables for the OpenTok Methods
	private Session mSession;
	private Publisher mPublisher;
	private Subscriber mSubscriber;
	private ArrayList<Stream> mStreams;
	protected Handler mHandler = new Handler();

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override public void onCreate() {
		super.onCreate();

		Log.d(LOGTAG, "VideoHeadService - onCreate");

		//Initialize variables
		endCallButton = new ImageButton(getApplicationContext());
		videoFlyHead = new RelativeLayout(getApplicationContext());
		mStreams = new ArrayList<Stream>();
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);


		videoFlyHead = (RelativeLayout) LayoutInflater.from(this).
				inflate(R.layout.views, null, true);
		//mPublisherViewContainer = (RelativeLayout) videoHead.findViewById(R.id.publisherview);
		mSubscriberViewContainer = (RelativeLayout) videoFlyHead.findViewById(R.id.subscriberview);

		mViewGroupLayout = (LinearLayout) LayoutInflater.from(this).
				inflate(R.layout.video_controls, null);
		videoFlyHead.addView(mViewGroupLayout);

		endCallButton = (ImageButton) mViewGroupLayout.findViewById(R.id.button_end_call);

		sessionConnect();

		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
				|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);

		videoFlyHead.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;

			//Used to make the Head movable.
			@Override public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					initialX = params.x;
					initialY = params.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					return true;
				case MotionEvent.ACTION_UP:
					return true;
				case MotionEvent.ACTION_MOVE:
					params.x = initialX + (int) (event.getRawX() - initialTouchX);
					params.y = initialY + (int) (event.getRawY() - initialTouchY);
					windowManager.updateViewLayout(videoFlyHead, params);
					return true;
				}
				return false;
			}
		});
		windowManager.addView(videoFlyHead, params);
	}

	private void sessionConnect() {
		Log.d(LOGTAG, "in sessionConnect");
		if (mSession == null) {
			mSession = new Session(VideoFlyService.this,
					OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
			mSession.setSessionListener(this);
			mSession.connect(OpenTokConfig.TOKEN);

			Log.d(LOGTAG, "leaving sessionConnect if loop");
		}
		Log.d(LOGTAG, "leaving sessionConnect");
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (videoFlyHead != null) windowManager.removeView(videoFlyHead);
	}


	//*************OpenTok Call Back Methods**************************
	private void subscribeToStream(Stream stream) {
		Log.d(LOGTAG, "subscribing to stream");
		mSubscriber = new Subscriber(VideoFlyService.this, stream);
		mSubscriber.setVideoListener(this);
		mSession.subscribe(mSubscriber);
		// start loading spinning

		Log.d(LOGTAG, "leaving subscribing to stream");

	}

	private void unsubscribeFromStream(Stream stream) {
		mStreams.remove(stream);
		if (mSubscriber.getStream().getStreamId().equals(stream.getStreamId())) {
			mSubscriberViewContainer.removeView(mSubscriber.getView());
			mSubscriber = null;
			if (!mStreams.isEmpty()) {
				subscribeToStream(mStreams.get(0));
			}
		}
	}
	private void attachSubscriberView(Subscriber subscriber) {
		Log.d(LOGTAG, "in attachSubcriberView");
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				getResources().getDisplayMetrics().widthPixels, getResources()
				.getDisplayMetrics().heightPixels);

		mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
		subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
				BaseVideoRenderer.STYLE_VIDEO_FILL);

		Log.d(LOGTAG, "leaving attachSubcriberView");
	}

	@SuppressWarnings("unused")
	private void attachPublisherView(Publisher publisher) {
		Log.d(LOGTAG, "in attachPublisherView");
		mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
				BaseVideoRenderer.STYLE_VIDEO_FILL);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				320, 240);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
				RelativeLayout.TRUE);
		//		layoutParams.bottomMargin = dpToPx(8);
		//		layoutParams.rightMargin = dpToPx(8);
		mSubscriberViewContainer.addView(mPublisher.getView(), layoutParams);

		Log.d(LOGTAG, "leaving attachPublisherView");
	}
	@Override
	public void onVideoDataReceived(SubscriberKit subscriber) {
		Log.i(LOGTAG, "First frame received");
		attachSubscriberView(mSubscriber);
	}

	@Override
	public void onVideoDisabled(SubscriberKit subscriber) {
		Log.i(LOGTAG,
				"Video quality changed. It is disabled for the subscriber.");
	}

	@Override
	public void onStreamCreated(PublisherKit publisher, Stream stream) {
		if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
			mStreams.add(stream);
			if (mSubscriber == null) {
				subscribeToStream(stream);
			}
		}
	}

	@Override
	public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
		if ((OpenTokConfig.SUBSCRIBE_TO_SELF && mSubscriber != null)) {
			unsubscribeFromStream(stream);
		}
	}

	@Override
	public void onConnected(Session arg0) {
		Log.i(LOGTAG, "Connected to the session.");
	}

	@Override
	public void onDisconnected(Session arg0) {
		Log.i(LOGTAG, "Disconnected from the session.");
		if (mPublisher != null) {
			mPublisherViewContainer.removeView(mPublisher.getView());
		}

		if (mSubscriber != null) {
			mSubscriberViewContainer.removeView(mSubscriber.getView());
		}

		mPublisher = null;
		mSubscriber = null;
		mStreams.clear();
		mSession = null;
	}

	@Override
	public void onError(Session session, OpentokError exception) {
		Log.i(LOGTAG, "Session exception: " + exception.getMessage());
	}

	@Override
	public void onStreamDropped(Session session, Stream stream) {
		if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
			if (mSubscriber != null) {
				unsubscribeFromStream(stream);
			}
		}
	}

	@Override
	public void onStreamReceived(Session session, Stream stream) {

		if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
			mStreams.add(stream);
			if (mSubscriber == null) {
				subscribeToStream(stream);
			}
		}
	}

	@Override
	public void onError(PublisherKit publisher, OpentokError exception) {
		Log.i(LOGTAG, "Publisher exception: " + exception.getMessage());
	}

	/**
	 * Converts dp to real pixels, according to the screen density.
	 * 
	 * @param dp
	 *            A number of density-independent pixels.
	 * @return The equivalent number of real pixels.
	 */
	private int dpToPx(int dp) {
		double screenDensity = this.getResources().getDisplayMetrics().density;
		return (int) (screenDensity * (double) dp);
	}


	public void endCallButtonClicked(View view){
		mStreams.clear();
		mSession.disconnect();
		this.mSubscriber = null;
		stopService(new Intent(getApplicationContext(), VideoFlyService.class));


	}

}
