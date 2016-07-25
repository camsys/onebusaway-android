/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.android.ui;

import com.sothree.slidinguppanel.ScrollableViewHelper;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.directions.realtime.RealtimeService;
import org.onebusaway.android.directions.tasks.TripRequest;
import org.onebusaway.android.directions.util.OTPConstants;
import org.onebusaway.android.directions.util.TripRequestBuilder;
import org.onebusaway.android.io.ObaAnalytics;
import org.onebusaway.android.util.LocationUtils;
import org.onebusaway.android.util.UIUtils;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.ws.Message;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class TripPlanActivity extends AppCompatActivity implements TripRequest.Callback,
        TripResultsFragment.Listener, TripPlanFragment.Listener, RealtimeService.Callback {

    private static final String TAG = "TripPlanActivity";

    TripRequestBuilder mBuilder;

    TripRequest mTripRequest = null;

    SlidingUpPanelLayout mPanel;

    AlertDialog mFeedbackDialog;

    private static final String PLAN_ERROR_CODE = "org.onebusaway.android.PLAN_ERROR_CODE";

    private static final String PLAN_ERROR_URL = "org.onebusaway.android.PLAN_ERROR_URL";

    private static final String PANEL_STATE_EXPANDED = "org.onebusaway.android.PANEL_STATE_EXPANDED";

    private static final String SHOW_ERROR_DIALOG = "org.onebusaway.android.SHOW_ERROR_DIALOG";

    private static final String REQUEST_LOADING = "org.onebusaway.android.REQUEST_LOADING";

    // flag to indicate intent sent by this class
    private static final String INTENT_SOURCE = "org.onebusaway.android.INTENT_SOURCE";
    private static final String INTENT_SOURCE_ACTIVITY = "activity";
    private static final String INTENT_SOURCE_NOTIFICATION = "notification";

    TripResultsFragment mResultsFragment;

    private ProgressDialog mProgressDialog;

    boolean mRequestLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_trip_plan);

        UIUtils.setupActionBar(this);

        Bundle bundle = (savedInstanceState == null) ? new Bundle() : savedInstanceState;
        mBuilder = new TripRequestBuilder(bundle);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Bundle bundle = mBuilder.getBundle();
        boolean newItineraries = false;

        // see if there is data from intent
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null
                && intent.getStringExtra(INTENT_SOURCE) != null) {

            ArrayList<Itinerary> itineraries = (ArrayList<Itinerary>) intent.getExtras().getSerializable(OTPConstants.ITINERARIES);

            if (itineraries != null) {
                bundle.putSerializable(OTPConstants.ITINERARIES, itineraries);
                newItineraries = true;
            }

            if (intent.getIntExtra(PLAN_ERROR_CODE, -1) != -1) {
                bundle.putSerializable(SHOW_ERROR_DIALOG, true);
                bundle.putInt(PLAN_ERROR_CODE, intent.getIntExtra(PLAN_ERROR_CODE, 0));
                bundle.putString(PLAN_ERROR_URL, intent.getStringExtra(PLAN_ERROR_URL));
            }

            // If intent sent from notification, we have other fields
            if (intent.getStringExtra(INTENT_SOURCE).equals(INTENT_SOURCE_NOTIFICATION)) {
                TripRequestBuilder src = new TripRequestBuilder(intent.getExtras());
                src.copyIntoBundle(bundle);
            }

            setIntent(null);
            bundle.putBoolean(REQUEST_LOADING, false);
            mRequestLoading = false;

        }

        if (mRequestLoading || bundle.getBoolean(REQUEST_LOADING)) {
            showProgressDialog();
        }

        // Check which fragment to create
        boolean haveTripPlan = bundle.getSerializable(OTPConstants.ITINERARIES) != null;

        TripPlanFragment fragment = (TripPlanFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trip_plan_fragment_container);
        if (fragment == null) {
            fragment = new TripPlanFragment();
            fragment.setArguments(bundle);
            fragment.setListener(this);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.trip_plan_fragment_container, fragment).commit();
        }

        mPanel = (SlidingUpPanelLayout) findViewById(R.id.trip_plan_sliding_layout);

        mPanel.setEnabled(haveTripPlan);

        if (haveTripPlan) {
            initResultsFragment();
            if (bundle.getBoolean(PANEL_STATE_EXPANDED) || newItineraries) {
                mPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        }

        // show error dialog if necessary
        if (bundle.getBoolean(SHOW_ERROR_DIALOG)) {
            int planErrorCode = intent.getIntExtra(PLAN_ERROR_CODE, 0);
            String planErrorUrl = intent.getStringExtra(PLAN_ERROR_URL);
            showFeedbackDialog(planErrorCode, planErrorUrl);
        }

        // Set the height of the panel after drawing occurs.
        final ViewGroup layout = (ViewGroup)findViewById(R.id.trip_plan_fragment_container);
        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int viewHeight = mPanel.getHeight();
                int height = layout.getMeasuredHeight();
                mPanel.setPanelHeight(viewHeight - height);
            }
        });

    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {

        if (mBuilder != null) {
            mBuilder.copyIntoBundle(bundle);

            // We also saved the itineraries and the results state in this bundle.

            Bundle source = mBuilder.getBundle();

            ArrayList<Itinerary> itineraries = (ArrayList<Itinerary>) source
                    .getSerializable(OTPConstants.ITINERARIES);
            if (itineraries != null) {
                bundle.putSerializable(OTPConstants.ITINERARIES, itineraries);
            }

            int rank = source.getInt(OTPConstants.SELECTED_ITINERARY);
            bundle.putInt(OTPConstants.SELECTED_ITINERARY, rank);

            boolean showMap = source.getBoolean(OTPConstants.SHOW_MAP);
            bundle.putBoolean(OTPConstants.SHOW_MAP, showMap);

            boolean panelStateExpanded = (mPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED);
            bundle.putBoolean(PANEL_STATE_EXPANDED, panelStateExpanded);

            boolean showError = source.getBoolean(SHOW_ERROR_DIALOG);
            bundle.putBoolean(SHOW_ERROR_DIALOG, showError);

            bundle.putBoolean(REQUEST_LOADING, mRequestLoading);
        }

    }

    @Override
    public void onPause() {

        super.onPause();

        // Close possible progress dialog.
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        if (mFeedbackDialog != null) {
            mFeedbackDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (mPanel != null && mPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onTripRequestReady() {

        // Remove results fragment if it exists
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.trip_results_fragment_container);
        if(fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        mTripRequest = mBuilder.setListener(this).execute(this);

        // clear out selected itinerary from bundle
        Bundle bundle = mBuilder.getBundle();
        bundle.remove(OTPConstants.ITINERARIES);
        bundle.remove(OTPConstants.SELECTED_ITINERARY);

        showProgressDialog();
    }

    private void initResultsFragment() {

        mResultsFragment = (TripResultsFragment) getSupportFragmentManager().findFragmentById(R.id.trip_results_fragment_container);
        if (mResultsFragment != null) {
            // bundle arguments already set
            mResultsFragment.displayNewResults();
            return;
        }

        mResultsFragment = new TripResultsFragment();
        mResultsFragment.setListener(this);
        mResultsFragment.setRealtimeCallback(this);
        mResultsFragment.setArguments(mBuilder.getBundle());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.trip_results_fragment_container, mResultsFragment).commit();

        getSupportFragmentManager().executePendingTransactions();

    }

    @Override
    public void onTripRequestComplete(List<Itinerary> itineraries) {

        // send intent to ourselves...
        Intent intent = new Intent(this, TripPlanActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(OTPConstants.ITINERARIES, (ArrayList<Itinerary>) itineraries)
                .putExtra(INTENT_SOURCE, INTENT_SOURCE_ACTIVITY);

        startActivity(intent);
    }

    @Override
    public void onTripRequestFailure(int errorCode, String url) {

        Intent intent = new Intent(this, TripPlanActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(PLAN_ERROR_CODE, errorCode)
                .putExtra(PLAN_ERROR_URL, url)
                .putExtra(INTENT_SOURCE, INTENT_SOURCE_ACTIVITY);

        startActivity(intent);
    }

    @Override
    public void onTripPlanInvalidated(RealtimeService.Reason reason, List<Itinerary> newPlan, int id) {

        // Send notification to this activity - start with new plan.

        String message = null;

        switch(reason) {
            case EARLY:
                message = getString(R.string.trip_plan_early);
                break;
            case LATE:
                message = getString(R.string.trip_plan_delay);
                break;
            case NOT_PRESENT:
                message = getString(R.string.trip_plan_not_recommended);
                break;
        }

        Bundle bundle = mBuilder.getBundle();
        bundle.putSerializable(OTPConstants.ITINERARIES, (ArrayList<Itinerary>) newPlan);
        bundle.putString(INTENT_SOURCE, INTENT_SOURCE_NOTIFICATION);

        Intent intent = new Intent(this, TripPlanActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtras(bundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_notification)
                        .setContentTitle(getResources().getString(R.string.title_activity_trip_plan))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        notification.defaults = Notification.DEFAULT_ALL;
        notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;

        notificationManager.notify(id, notification);

    }

    private void showFeedbackDialog(int errorCode, final String url) {

        String msg = getString(R.string.tripplanner_error_not_defined);

        if (errorCode > 0 && errorCode != Message.PLAN_OK.getId()) {
            msg = getErrorMessage(errorCode);
        }

        final Bundle bundle = mBuilder.getBundle();

        AlertDialog.Builder feedback = new AlertDialog.Builder(this)
                .setTitle(R.string.tripplanner_error_dialog_title)
                .setMessage(msg);

        feedback.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bundle.putBoolean(SHOW_ERROR_DIALOG, false);
                clearBundleErrors();
            }
        });

        feedback.setNegativeButton(R.string.report_problem_report, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = Application.get().getCurrentRegion().getOtpContactEmail();
                if (!TextUtils.isEmpty(email)) {
                    Location loc = Application.getLastKnownLocation(getApplicationContext(), null);
                    String locString = null;
                    if (loc != null) {
                        locString = LocationUtils.printLocationDetails(loc);
                    }
                    UIUtils.sendEmail(TripPlanActivity.this, email, locString, url);
                    ObaAnalytics.reportEventWithCategory(ObaAnalytics.ObaEventCategory.UI_ACTION.toString(),
                            getString(R.string.analytics_action_problem),
                            getString(R.string.analytics_label_app_feedback_otp));
                }
                else {
                    Toast.makeText(TripPlanActivity.this,
                            getString(R.string.tripplanner_no_contact),
                            Toast.LENGTH_SHORT).show();
                }
                bundle.putBoolean(SHOW_ERROR_DIALOG, false);
                clearBundleErrors();
            }
        });

        mFeedbackDialog = feedback.create();
        mFeedbackDialog.show();
    }

    private void showProgressDialog() {
        mRequestLoading = true;

        if (mProgressDialog == null || !mProgressDialog.isShowing()) {
            mProgressDialog = ProgressDialog.show(this, "",
                    getResources().getText(R.string.task_progress_tripplanner_progress), true);
        }
    }

    private String getErrorMessage(int errorCode) {
        if (errorCode == Message.SYSTEM_ERROR.getId()) {
            return (getString(R.string.tripplanner_error_system));
        } else if (errorCode == Message.OUTSIDE_BOUNDS.getId()) {
            return (getString(R.string.tripplanner_error_outside_bounds));
        } else if (errorCode == Message.PATH_NOT_FOUND.getId()) {
            return (getString(R.string.tripplanner_error_path_not_found));
        } else if (errorCode == Message.NO_TRANSIT_TIMES.getId()) {
            return (getString(R.string.tripplanner_error_no_transit_times));
        } else if (errorCode == Message.REQUEST_TIMEOUT.getId()) {
            return (getString(R.string.tripplanner_error_request_timeout));
        } else if (errorCode == Message.BOGUS_PARAMETER.getId()) {
            return (getString(R.string.tripplanner_error_bogus_parameter));
        } else if (errorCode == Message.GEOCODE_FROM_NOT_FOUND.getId()) {
            return (getString(R.string.tripplanner_error_geocode_from_not_found));
        } else if (errorCode == Message.GEOCODE_TO_NOT_FOUND.getId()) {
            return (getString(R.string.tripplanner_error_geocode_to_not_found));
        } else if (errorCode == Message.GEOCODE_FROM_TO_NOT_FOUND.getId()) {
            return (getString(R.string.tripplanner_error_geocode_from_to_not_found));
        } else if (errorCode == Message.TOO_CLOSE.getId()) {
            return (getString(R.string.tripplanner_error_too_close));
        } else if (errorCode == Message.LOCATION_NOT_ACCESSIBLE.getId()) {
            return (getString(R.string.tripplanner_error_location_not_accessible));
        } else if (errorCode == Message.GEOCODE_FROM_AMBIGUOUS.getId()) {
            return (getString(R.string.tripplanner_error_geocode_from_ambiguous));
        } else if (errorCode == Message.GEOCODE_TO_AMBIGUOUS.getId()) {
            return (getString(R.string.tripplanner_error_geocode_to_ambiguous));
        } else if (errorCode == Message.GEOCODE_FROM_TO_AMBIGUOUS.getId()) {
            return (getString(R.string.tripplanner_error_geocode_from_to_ambiguous));
        } else if (errorCode == Message.UNDERSPECIFIED_TRIANGLE.getId()
                || errorCode == Message.TRIANGLE_NOT_AFFINE.getId()
                || errorCode == Message.TRIANGLE_OPTIMIZE_TYPE_NOT_SET.getId()
                || errorCode == Message.TRIANGLE_VALUES_NOT_SET.getId()) {
            return (getString(R.string.tripplanner_error_triangle));
        } else if (errorCode == TripRequest.NO_SERVER_SELECTED) {
            return getString(R.string.tripplanner_no_server_selected_error);
        }
        else {
            return null;
        }
    }

    private void clearBundleErrors() {
        mBuilder.getBundle().remove(PLAN_ERROR_CODE);
        mBuilder.getBundle().remove(PLAN_ERROR_URL);
    }

    // Handle the sliding panel's interactions with the list view and the map view.
    @Override
    public void onResultViewCreated(View container, final ListView listView, View mapView) {

        if (mPanel != null) {
            mPanel.setScrollableViewHelper(new ScrollableViewHelper() {
                @Override
                public int getScrollableViewScrollPosition(View scrollableView, boolean isSlidingUp) {
                    if (mResultsFragment.isMapShowing()) {
                        return 1; // map can scroll infinitely, so return a positive value
                    } else {
                        return super.getScrollableViewScrollPosition(listView, isSlidingUp);
                    }
                }
            });

            mPanel.setScrollableView(container);
        }
    }
}
