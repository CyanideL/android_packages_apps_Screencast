package org.namelessrom.screencast.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.Toast;

import org.namelessrom.screencast.Logger;
import org.namelessrom.screencast.R;
import org.namelessrom.screencast.Utils;
import org.namelessrom.screencast.recording.RecordingDevice;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ScreencastService extends Service {

    private static final String TAG = ScreencastService.class.getSimpleName();

    public static final String ACTION_START_SCREENCAST = "org.namelessrom.ACTION_START_SCREENCAST";
    public static final String ACTION_STOP_SCREENCAST  = "org.namelessrom.ACTION_STOP_SCREENCAST";
    public static final String ACTION_SHOW_TOUCHES     = "org.namelessrom.SHOW_TOUCHES";

    private Notification.Builder mBuilder;
    private RecordingDevice      mRecorder;

    private long  mStartTime;
    private Timer mTimer;

    @Override public IBinder onBind(final Intent intent) { return null; }

    @Override public void onDestroy() {
        cleanup();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        super.onDestroy();
    }

    @Override public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent == null) {
            stopSelf();
            return START_STICKY;
        }

        final String action = intent.getAction();

        if (TextUtils.equals(action, ACTION_START_SCREENCAST)) {
            Utils.setRecording(this, true);
            if (!hasAvailableSpace()) {
                Toast.makeText(this, R.string.insufficient_storage, Toast.LENGTH_LONG).show();
                return START_STICKY;
            }

            mStartTime = System.currentTimeMillis();
            try {
                registerScreencaster();
            } catch (Exception e) {
                Logger.e(TAG, "Failed to register screen caster", e);
            }
            mBuilder = createNotificationBuilder();
            Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES, 1);
            addNotificationTouchButton(true);
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    updateNotification(ScreencastService.this);
                }
            }, 100L, 1000L);

            return START_STICKY;
        }

        if (TextUtils.equals(action, ACTION_STOP_SCREENCAST)) {
            Utils.setRecording(this, false);
            Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES, 0);
            if (!hasAvailableSpace()) {
                Toast.makeText(this, R.string.insufficient_storage, Toast.LENGTH_LONG).show();
                return START_STICKY;
            }

            final String filePath = mRecorder != null ? mRecorder.getRecordingFilePath() : null;
            cleanup();
            sendShareNotification(filePath);
        }

        if (TextUtils.equals(action, ACTION_SHOW_TOUCHES)) {
            final String show = intent.getStringExtra(Settings.System.SHOW_TOUCHES);

            mBuilder = createNotificationBuilder();
            addNotificationTouchButton(TextUtils.equals("on", show));
        }

        return START_STICKY;
    }

    private Notification.Builder createNotificationBuilder() {
        final Intent intent = new Intent(ACTION_STOP_SCREENCAST);
        final Notification.Builder builder = new Notification.Builder(this);
        builder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.recording))
                .addAction(R.drawable.ic_stop, getString(R.string.stop),
                        PendingIntent.getBroadcast(this, 0, intent, 0));
        return builder;
    }

    private void updateNotification(final Context context) {
        final long delta = System.currentTimeMillis() - mStartTime;
        final SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("mm:ss");

        mBuilder.setContentText("Video Length : " + localSimpleDateFormat.format(new Date(delta)));

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(0, mBuilder.build());
    }

    private void addNotificationTouchButton(final boolean showTouches) {
        final Intent intent = new Intent(ACTION_SHOW_TOUCHES);
        if (showTouches) {
            Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES, 1);
            intent.putExtra(Settings.System.SHOW_TOUCHES, "off");
            mBuilder.addAction(R.drawable.ic_touch_on,
                    getString(R.string.show_touches),
                    PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            Settings.System.putInt(getContentResolver(), Settings.System.SHOW_TOUCHES, 0);
            intent.putExtra(Settings.System.SHOW_TOUCHES, "on");
            mBuilder.addAction(R.drawable.ic_touch_off,
                    getString(R.string.show_touches),
                    PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }


    private Notification.Builder createShareNotificationBuilder(final String filePath) {
        // parse the Uri
        final Uri localUri = Uri.parse("file://" + filePath);
        Logger.i("ScreencastService", "Video complete: " + localUri);

        // create an temporary intent, which will be used to create the chooser
        final Intent tmpIntent = new Intent("android.intent.action.SEND");
        tmpIntent.setType("video/mp4");
        tmpIntent.putExtra("android.intent.extra.STREAM", localUri);
        tmpIntent.putExtra("android.intent.extra.SUBJECT", filePath);

        // create the intent, which lets us choose how we want to share the screencast
        final Intent shareIntent = Intent.createChooser(tmpIntent, null);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // create an intent, which opens the screencast
        final Intent viewIntent = new Intent("android.intent.action.VIEW");
        viewIntent.setDataAndType(localUri, "video/mp4");
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // get the duration of the screencast in minute:seconds format
        final String duration = new SimpleDateFormat("mm:ss")
                .format(new Date(System.currentTimeMillis() - mStartTime));

        // create our pending intents
        final PendingIntent pendingViewIntent =
                PendingIntent.getActivity(this, 0, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final PendingIntent pendingShareIntent =
                PendingIntent.getActivity(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // build the notification
        return new Notification.Builder(this)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.recording_ready_to_share))
                .setContentText(getString(R.string.video_length, duration))
                .setContentIntent(pendingViewIntent)
                .addAction(android.R.drawable.ic_menu_share, getString(R.string.share),
                        pendingShareIntent);
    }

    private boolean hasAvailableSpace() {
        final StatFs localStatFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return localStatFs.getBlockSizeLong() * localStatFs.getBlockCountLong() / 1048576L >= 100L;
    }

    private void sendShareNotification(final String filePath) {
        final NotificationManager localNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = createShareNotificationBuilder(filePath);
        localNotificationManager.notify(0, mBuilder.build());
    }

    private void cleanup() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private Point getNativeResolution() {
        final Display localDisplay =
                ((DisplayManager) getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0);
        final Point point = new Point();
        try {
            // try to get the real size and return it
            localDisplay.getRealSize(point);
            return point;
        } catch (Exception exception) {
            Logger.e(TAG, "Failed getting real size", exception);
            // fall back to reflection
            try {
                Logger.e(TAG, "Failed getting real size again", exception);
                // get the raw width
                final Method getRawWidth = Display.class.getMethod("getRawWidth", new Class[0]);
                point.x = ((Integer) getRawWidth.invoke(localDisplay, (Object[]) null));

                // get the raw height
                final Method getRawHeight = Display.class.getMethod("getRawHeight", new Class[0]);
                point.y = ((Integer) getRawHeight.invoke(localDisplay, (Object[]) null));

                return point;
            } catch (Exception notAnotherException) {
                Logger.e(TAG, "Failed getting real size again again!", exception);
                // our last resort...
                localDisplay.getSize(point);
            }
        }
        return point;
    }

    private void registerScreencaster() throws RemoteException {
        final Display display =
                ((DisplayManager) getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0);

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        final Point point = getNativeResolution();
        mRecorder = new RecordingDevice(this, point.x, point.y);
        final VirtualDisplay virtualDisplay = mRecorder.registerVirtualDisplay(this,
                "hidden:screen-recording", point.x, point.y, displayMetrics.densityDpi);
        if (virtualDisplay == null) {
            cleanup();
        }
    }

}