/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.android.directions.realtime;

import org.onebusaway.android.directions.model.ItineraryDescription;
import org.onebusaway.android.directions.tasks.TripRequest;
import org.onebusaway.android.directions.util.OTPConstants;
import org.onebusaway.android.directions.util.TripRequestBuilder;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;
import java.util.List;


public class RealtimeServiceImpl implements RealtimeService {

    private static final String TAG = "RealtimeServiceImpl";

    private Context mApplicationContext;

    private AlarmManager mAlarmMgr;

    private RealtimeService.Callback mCallback;

    private IntentFilter mIntentFilter;

    PendingIntent mAlarmIntentTripUpdate;

    Intent mTripUpdateIntent;

    Bundle mBundle;

    ItineraryDescription mItineraryDescription;

    boolean mRegistered;

    private static final long DELAY_THRESHOLD_SEC = 120;

    public RealtimeServiceImpl(Context context, RealtimeService.Callback callback, Bundle bundle) {
        mCallback = callback;
        mApplicationContext = context;
        mAlarmMgr = (AlarmManager) mApplicationContext.getSystemService(Context.ALARM_SERVICE);

        mIntentFilter = new IntentFilter(OTPConstants.INTENT_UPDATE_TRIP_TIME_ACTION);
        mIntentFilter.addAction(OTPConstants.INTENT_NOTIFICATION_ACTION_OPEN_APP);

        mTripUpdateIntent = new Intent(OTPConstants.INTENT_UPDATE_TRIP_TIME_ACTION);
        mAlarmIntentTripUpdate = PendingIntent.getBroadcast(mApplicationContext, 0, mTripUpdateIntent, 0);

        mBundle = bundle;

        mRegistered = false;
    }

    @Override
    public void onItinerarySelected(Itinerary itinerary) {

        disableListenForTripUpdates();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

        if (prefs.getBoolean(OTPConstants.PREFERENCE_KEY_LIVE_UPDATES, true)) {

            boolean realtimeLegsOnItineraries = false;

            for (Leg leg : itinerary.legs) {
                if (leg.realTime) {
                    realtimeLegsOnItineraries = true;
                }
            }

            if (realtimeLegsOnItineraries) {

                Log.d(TAG, "Starting realtime updates for itinerary");

                mItineraryDescription = new ItineraryDescription(itinerary);

                mApplicationContext.registerReceiver(broadcastReceiver, mIntentFilter);
                mAlarmMgr.setInexactRepeating(AlarmManager.RTC, new Date().getTime(),
                        OTPConstants.DEFAULT_UPDATE_INTERVAL_TRIP_TIME, mAlarmIntentTripUpdate);
                mRegistered = true;

            } else {
                Log.d(TAG, "No realtime legs on itinerary");
            }

        }
    }

    private void checkForItineraryChange() {
        TripRequest.Callback callback = new TripRequest.Callback() {
            @Override
            public void onTripRequestComplete(List<Itinerary> itineraries) {
                if (itineraries == null || itineraries.isEmpty()) {
                    onTripRequestFailure(-1, null);
                    return;
                }

                // Check each itinerary. Notify user if our *current* itinerary doesn't exist
                // or has a lower rank.
                for (int i = 0; i < itineraries.size(); i++) {
                    ItineraryDescription other = new ItineraryDescription(itineraries.get(i));

                    if (mItineraryDescription.itineraryMatches(other)) {

                        long delay = mItineraryDescription.getDelay(other);
                        Log.d(TAG, "Delay on itinerary: " + delay);

                        if (Math.abs(delay) > DELAY_THRESHOLD_SEC) {
                            Log.d(TAG, "Notify due to large delay.");
                            mCallback.onTripPlanInvalidated((delay > 0 ? Reason.LATE : Reason.EARLY), itineraries, mItineraryDescription.getId());
                            disableListenForTripUpdates();
                            return;
                        }

                        // Otherwise, we are still good.
                        Log.d(TAG, "Itinerary exists and is not delayed.");
                        checkDisableDueToTimeout();

                        return;
                    }

                }

                Log.d(TAG, "Did not find a matching itinerary in new call.");
                mCallback.onTripPlanInvalidated(Reason.NOT_PRESENT, itineraries, mItineraryDescription.getId());
                disableListenForTripUpdates();

            }

            @Override
            public void onTripRequestFailure(int result, String url) {
                Log.e(TAG, "Failure checking itineraries. Result=" + result + ", url=" + url);
                disableListenForTripUpdates();
                return;
            }
        };

        // Create trip request from the original. Do not update the departure time.
        // Set numItineraries to a large number so it's less likely we won't get same itinerary back.
        TripRequestBuilder builder = new TripRequestBuilder(mBundle)
                .setNumItineraries(10)
                .setListener(callback);

        try {
            builder.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // If the end time for this itinerary has passed, disable trip updates.
    private void checkDisableDueToTimeout() {
        if (mItineraryDescription.isExpired()) {
            Log.d(TAG, "End of trip has passed.");
            disableListenForTripUpdates();
        }
    }

    public void disableListenForTripUpdates() {
        if (mRegistered) {
            mAlarmMgr.cancel(mAlarmIntentTripUpdate);
            mApplicationContext.unregisterReceiver(broadcastReceiver);
        }
        mRegistered = false;
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(OTPConstants.INTENT_UPDATE_TRIP_TIME_ACTION)) {
                checkForItineraryChange();

            }
        }
    };
}
