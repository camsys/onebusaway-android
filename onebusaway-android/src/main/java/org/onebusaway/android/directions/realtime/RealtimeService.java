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

package org.onebusaway.android.directions.realtime;

import org.opentripplanner.api.model.Itinerary;

import java.util.List;

public interface RealtimeService {

    enum Reason { EARLY, LATE, NOT_PRESENT }

    interface Callback {
        /**
         * Notify calling class that trip plan is no longer valid
         *
         * @param reason - reason why plan is invalid: early, late, did not appear in new request.
         * @param newPlan - list of itineraries from most recent request to OTP for this plan
         * @param id - unique ID for the trip plan that has become invalid. Helpful for sending notifications.
         */
        void onTripPlanInvalidated(Reason reason, List<Itinerary> newPlan, int id);
    }

    void onItinerarySelected(Itinerary itinerary);

    void disableListenForTripUpdates();

}
