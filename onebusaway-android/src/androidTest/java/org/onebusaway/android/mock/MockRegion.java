package org.onebusaway.android.mock;

import org.onebusaway.android.io.elements.ObaRegion;
import org.onebusaway.android.io.elements.ObaRegionElement;
import org.onebusaway.android.util.RegionUtils;

import android.content.Context;

import java.util.ArrayList;

/**
 * Provides mock region information
 */
public class MockRegion {

    // Production region IDs shouldn't change, so these values should match the regions-v3.json API response
    public static final int TAMPA_REGION_ID = 0;

    public static final int PUGET_SOUND_REGION_ID = 1;

    public static final int ATLANTA_REGION_ID = 3;

    public static ObaRegion getTampa(Context context) {
        ArrayList<ObaRegion> regions = RegionUtils.getRegionsFromResources(context);
        for (ObaRegion r : regions) {
            if (r.getId() == TAMPA_REGION_ID) {
                return r;
            }
        }
        return null; // This should never happen
    }

    public static ObaRegion getPugetSound(Context context) {
        ArrayList<ObaRegion> regions = RegionUtils.getRegionsFromResources(context);
        for (ObaRegion r : regions) {
            if (r.getId() == PUGET_SOUND_REGION_ID) {
                return r;
            }
        }
        return null; // This should never happen
    }

    public static ObaRegion getAtlanta(Context context) {
        ArrayList<ObaRegion> regions = RegionUtils.getRegionsFromResources(context);
        for (ObaRegion r : regions) {
            if (r.getId() == ATLANTA_REGION_ID) {
                return r;
            }
        }
        return null; // This should never happen
    }

    /**
     * Returns a test version of the Tampa region with a path appended, but no separator (i.e.,
     * http://api.tampa.onebusaway.org/api), as the base URL
     *
     * @return a test version of the Tampa region with a path appended, but no separator (i.e.,
     * http://api.tampa.onebusaway.org/api), as the base URL
     */
    public static ObaRegion getRegionWithPathNoSeparator(Context context) {
        ObaRegionElement.Bounds bound = new ObaRegionElement.Bounds(27.976910500000002, -82.445851,
                0.5424609999999994, 0.576357999999999);
        ObaRegionElement.Bounds[] bounds = new ObaRegionElement.Bounds[1];
        bounds[0] = bound;

        return new ObaRegionElement(
                0,
                "Test-RegionWithPathNoSeparator",
                true,
                "http://api.tampa.onebusaway.org/api",
                null,
                bounds,
                new ObaRegionElement.Open311Server[0],
                "en_US",
                "test@test.org",
                true,
                true,
                false,
                null,
                false,
                null);
    }

    /**
     * Returns a test version of the PugetSound region without a trailing path separator (i.e.,
     * http://api.pugetsound.onebusaway.org), as the base URL
     *
     * @return a test version of the PugetSound region without a trailing path separator (i.e.,
     * http://api.pugetsound.onebusaway.org), as the base URL
     */
    public static ObaRegion getRegionNoSeparator(Context context) {
        ObaRegionElement.Bounds bound = new ObaRegionElement.Bounds(47.221315, -122.4051325,
                0.33704, 0.440483);
        ObaRegionElement.Bounds[] bounds = new ObaRegionElement.Bounds[1];
        bounds[0] = bound;

        return new ObaRegionElement(
                0,
                "Test-RegionWithPathNoSeparator",
                true,
                "http://api.pugetsound.onebusaway.org",
                null,
                bounds,
                new ObaRegionElement.Open311Server[0],
                "en_US",
                "test@test.org",
                true,
                true,
                false,
                null,
                false,
                "http://stopinfo.pugetsound.onebusaway.org");
    }

    /**
     * Returns a test version of the Tampa region with a custom port (i.e.,
     * http://api.tampa.onebusaway.org:8088/api/), as the base URL
     *
     * @return a test version of the Tampa region with a custom port (i.e.,
     * http://api.tampa.onebusaway.org:8088/api/), as the base URL
     */
    public static ObaRegion getRegionWithPort(Context context) {
        ObaRegionElement.Bounds bound = new ObaRegionElement.Bounds(27.976910500000002, -82.445851,
                0.5424609999999994, 0.576357999999999);
        ObaRegionElement.Bounds[] bounds = new ObaRegionElement.Bounds[1];
        bounds[0] = bound;

        return new ObaRegionElement(
                0,
                "Test-RegionWithPort",
                true,
                "http://api.tampa.onebusaway.org:8088/api/",
                null,
                bounds,
                new ObaRegionElement.Open311Server[0],
                "en_US",
                "test@test.org",
                true,
                true,
                false,
                null,
                false,
                null);
    }

    /**
     * Returns a test version of the Tampa region without a scheme (i.e.,
     * api.tampa.onebusaway.org:8088/api/), as the base URL
     *
     * @return a test version of the Tampa region without a scheme (i.e.,
     * api.tampa.onebusaway.org:8088/api/), as the base URL
     */
    public static ObaRegion getRegionNoScheme(Context context) {
        ObaRegionElement.Bounds bound = new ObaRegionElement.Bounds(27.976910500000002, -82.445851,
                0.5424609999999994, 0.576357999999999);
        ObaRegionElement.Bounds[] bounds = new ObaRegionElement.Bounds[1];
        bounds[0] = bound;

        return new ObaRegionElement(
                0,
                "Test-RegionNoScheme",
                true,
                "api.tampa.onebusaway.org/api/",
                null,
                bounds,
                new ObaRegionElement.Open311Server[0],
                "en_US",
                "test@test.org",
                true,
                true,
                false,
                null,
                false,
                null);
    }

    /**
     * Returns a test version of the Tampa region without an OBA Discovery or Real-time APIs
     *
     * @return a test version of the Tampa region without an OBA Discovery or Real-time APIs
     */
    public static ObaRegion getRegionWithoutObaApis(Context context) {
        ObaRegionElement.Bounds bound = new ObaRegionElement.Bounds(27.976910500000002, -82.445851,
                0.5424609999999994, 0.576357999999999);
        ObaRegionElement.Bounds[] bounds = new ObaRegionElement.Bounds[1];
        bounds[0] = bound;

        return new ObaRegionElement(
                0,
                "Test-RegionWithoutOBAApis",
                true,
                "http://api.tampa.onebusaway.org/api/",
                null,
                bounds,
                null,
                "en_US",
                "test@test.org",
                false,
                false,
                false,
                null,
                false,
                null);
    }

    /**
     * Returns a test version of the Tampa region that is not active
     *
     * @return a test version of the Tampa region that is not active
     */
    public static ObaRegion getInactiveRegion(Context context) {
        ObaRegionElement.Bounds bound = new ObaRegionElement.Bounds(27.976910500000002, -82.445851,
                0.5424609999999994, 0.576357999999999);
        ObaRegionElement.Bounds[] bounds = new ObaRegionElement.Bounds[1];
        bounds[0] = bound;

        return new ObaRegionElement(
                0,
                "Test-RegionWithoutOBAApis",
                true,
                "http://api.tampa.onebusaway.org/api/",
                null,
                bounds,
                null,
                "en_US",
                "test@test.org",
                false,
                false,
                false,
                null,
                false,
                null);
    }
}
