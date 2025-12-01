//
//  LuaLoader.java
//  AgeRange
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.ageRange"
package plugin.ageRange;

import android.app.Activity;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;

import java.util.HashMap;
import java.util.Map;

import android.database.Cursor;
import android.net.Uri;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
    /** Lua registry ID to the Lua function to be called when events occur. */
    private int fListener;

    /** Event names */
    private static final String AGE_RANGE_EVENT = "ageRangeEvent";
    private static final String UPDATE_EVENT = "significantUpdateEvent";
    private static final String COMMUNICATION_EVENT = "communicationEvent";

    /** Amazon Age Data API URI */
    private static final Uri AMAZON_AGE_DATA_URI = Uri.parse("content://amzn_appstore/getUserAgeData");

    /**
     * Creates a new Lua interface to this plugin.
     */
    @SuppressWarnings("unused")
    public LuaLoader() {
        // Initialize member variables.
        fListener = CoronaLua.REFNIL;

        // Set up this plugin to listen for Corona runtime events
        CoronaEnvironment.addRuntimeListener(this);
    }

    /**
     * Called when this plugin is being loaded via the Lua require() function.
     */
    @Override
    public int invoke(LuaState L) {
        // Register this plugin into Lua with the following functions.
        NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
                new InitWrapper(),
                new RequestAgeRangeWrapper(),
                new RequestSignificantUpdatePermissionWrapper(),
                new RequestCommunicationPermissionWrapper(),
                new StartListeningForCommunicationResponsesWrapper()
        };
        String libName = L.toString( 1 );
        L.register(libName, luaFunctions);

        return 1;
    }

    @Override
    public void onLoaded(CoronaRuntime runtime) {}

    @Override
    public void onStarted(CoronaRuntime runtime) {}

    @Override
    public void onSuspended(CoronaRuntime runtime) {}

    @Override
    public void onResumed(CoronaRuntime runtime) {}

    @Override
    public void onExiting(CoronaRuntime runtime) {
        // Remove the Lua listener references
        CoronaLua.deleteRef(runtime.getLuaState(), fListener);
        fListener = CoronaLua.REFNIL;
    }

    /**
     * Dispatch age range event to Lua
     */
    private void dispatchAgeRangeEvent(final Map<String, Object> eventData) {
        CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
        if (activity == null) return;

        activity.getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();
                CoronaLua.newEvent(L, AGE_RANGE_EVENT);

                for (Map.Entry<String, Object> entry : eventData.entrySet()) {
                    pushValue(L, entry.getValue());
                    L.setField(-2, entry.getKey());
                }

                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Dispatch significant update event to Lua
     */
    private void dispatchUpdateEvent(final Map<String, Object> eventData) {
        CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
        if (activity == null) return;

        activity.getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();
                CoronaLua.newEvent(L, UPDATE_EVENT);

                for (Map.Entry<String, Object> entry : eventData.entrySet()) {
                    pushValue(L, entry.getValue());
                    L.setField(-2, entry.getKey());
                }

                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Dispatch communication event to Lua
     */
    private void dispatchCommunicationEvent(final Map<String, Object> eventData) {
        CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
        if (activity == null) return;

        activity.getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();
                CoronaLua.newEvent(L, COMMUNICATION_EVENT);

                for (Map.Entry<String, Object> entry : eventData.entrySet()) {
                    pushValue(L, entry.getValue());
                    L.setField(-2, entry.getKey());
                }

                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Helper method to push various value types to Lua stack
     */
    private void pushValue(LuaState L, Object value) {
        if (value instanceof String) {
            L.pushString((String) value);
        } else if (value instanceof Integer) {
            L.pushInteger((Integer) value);
        } else if (value instanceof Boolean) {
            L.pushBoolean((Boolean) value);
        } else if (value instanceof Double) {
            L.pushNumber((Double) value);
        } else if (value instanceof Long) {
            L.pushNumber(((Long) value).doubleValue());
        } else {
            L.pushNil();
        }
    }

    /**
     * Initialize age range listener
     */
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    public int init(LuaState L) {
        int listenerIndex = 1;

        if (CoronaLua.isListener(L, listenerIndex, AGE_RANGE_EVENT)) {
            fListener = CoronaLua.newRef(L, listenerIndex);
        }

        return 0;
    }

    /**
     * Request age range from user using Amazon's GetUserAgeData API
     */
    @SuppressWarnings("WeakerAccess")
    public int requestAgeRange(LuaState L) {
        CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
        if (activity == null) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("isError", true);
            eventData.put("isAvailable", false);
            eventData.put("declined", false);
            eventData.put("errorMessage", "Activity not available");
            dispatchAgeRangeEvent(eventData);
            return 0;
        }

        activity.runOnUiThread(() -> {
            Cursor cursor = null;
            try {
                cursor = activity.getContentResolver().query(
                        AMAZON_AGE_DATA_URI,
                        null, null, null, null);

                if (cursor == null || !cursor.moveToFirst()) {
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("isError", true);
                    eventData.put("isAvailable", false);
                    eventData.put("declined", false);
                    eventData.put("errorMessage", "Amazon Age Data API not available");
                    dispatchAgeRangeEvent(eventData);
                    return;
                }

                // Get the JSON response from the cursor
                int dataColumnIndex = cursor.getColumnIndex("data");
                if (dataColumnIndex == -1) {
                    // Try getting the first column if "data" doesn't exist
                    dataColumnIndex = 0;
                }
                String jsonResponse = cursor.getString(dataColumnIndex);

                JSONObject json = new JSONObject(jsonResponse);
                String responseStatus = json.optString("responseStatus", "");
                String userStatus = json.optString("userStatus", "");

                Map<String, Object> eventData = new HashMap<>();

                if ("SUCCESS".equals(responseStatus)) {
                    eventData.put("isError", false);
                    eventData.put("isAvailable", true);
                    eventData.put("declined", false);

                    if ("VERIFIED".equals(userStatus)) {
                        // User is verified as 18+
                        eventData.put("lowerBound", json.optInt("ageLower", 18));
                        // ageUpper is null for 18+ users
                        eventData.put("hasParentalControls", false);
                        eventData.put("userStatus", "verified");
                    } else if ("SUPERVISED".equals(userStatus)) {
                        // Supervised user (under 18)
                        if (!json.isNull("ageLower")) {
                            eventData.put("lowerBound", json.getInt("ageLower"));
                        }
                        if (!json.isNull("ageUpper")) {
                            eventData.put("upperBound", json.getInt("ageUpper"));
                        }
                        eventData.put("hasParentalControls", true);
                        eventData.put("userStatus", "supervised");

                        // Include additional supervised user info
                        if (!json.isNull("userId")) {
                            eventData.put("installId", json.getString("userId"));
                        }
                        if (!json.isNull("mostRecentApprovalDate")) {
                            eventData.put("mostRecentApprovalDate", json.getString("mostRecentApprovalDate"));
                        }
                    } else {
                        // Unknown or empty status
                        eventData.put("userStatus", userStatus.toLowerCase());
                    }
                } else {
                    eventData.put("isError", true);
                    eventData.put("isAvailable", false);
                    eventData.put("declined", false);
                    eventData.put("errorMessage", "Amazon Age Data API returned status: " + responseStatus);
                }

                dispatchAgeRangeEvent(eventData);

            } catch (JSONException e) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("isError", true);
                eventData.put("isAvailable", false);
                eventData.put("declined", false);
                eventData.put("errorMessage", "Failed to parse Amazon Age Data response: " + e.getMessage());
                dispatchAgeRangeEvent(eventData);
            } catch (Exception e) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("isError", true);
                eventData.put("isAvailable", false);
                eventData.put("declined", false);
                eventData.put("errorMessage", "Amazon Age Data API error: " + e.getMessage());
                dispatchAgeRangeEvent(eventData);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });

        return 0;
    }

    /**
     * Request permission for significant app update
     * NOTE: On Amazon, significant changes must be submitted through the Amazon Developer Console
     * This function is provided for API compatibility
     * Parameters: description (string)
     */
    @SuppressWarnings("WeakerAccess")
    public int requestSignificantUpdatePermission(LuaState L) {
        String description = L.checkString(1);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("isError", false);
        eventData.put("description", description);
        eventData.put("platform", "android");
        eventData.put("store", "amazon");
        eventData.put("message", "On Amazon, significant changes must be submitted through the Amazon Developer Console. " +
                "This API is for compatibility only.");
        dispatchUpdateEvent(eventData);

        return 0;
    }

    /**
     * Request communication permission
     * Uses Amazon's GetUserAgeData API to check if the user is supervised
     * Parameters: handle (string), handleKind (string)
     */
    @SuppressWarnings("WeakerAccess")
    public int requestCommunicationPermission(LuaState L) {
        String handle = L.checkString(1);
        String handleKind = L.checkString(2);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("handle", handle);
        eventData.put("handleKind", handleKind);
        eventData.put("platform", "android");
        eventData.put("store", "amazon");
        eventData.put("message", "Amazon Age Data API doesn't have direct communication approval. " +
                "Check user supervision status instead.");

        CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
        if (activity == null) {
            eventData.put("isError", true);
            eventData.put("errorMessage", "Activity not available");
            dispatchCommunicationEvent(eventData);
            return 0;
        }

        activity.runOnUiThread(() -> {
            Cursor cursor = null;
            try {
                cursor = activity.getContentResolver().query(
                        AMAZON_AGE_DATA_URI,
                        null, null, null, null);

                if (cursor == null || !cursor.moveToFirst()) {
                    Map<String, Object> errorData = new HashMap<>(eventData);
                    errorData.put("isError", true);
                    errorData.put("errorMessage", "Amazon Age Data API not available");
                    dispatchCommunicationEvent(errorData);
                    return;
                }

                int dataColumnIndex = cursor.getColumnIndex("data");
                if (dataColumnIndex == -1) {
                    dataColumnIndex = 0;
                }
                String jsonResponse = cursor.getString(dataColumnIndex);

                JSONObject json = new JSONObject(jsonResponse);
                String responseStatus = json.optString("responseStatus", "");
                String userStatus = json.optString("userStatus", "");

                Map<String, Object> resultData = new HashMap<>(eventData);

                if ("SUCCESS".equals(responseStatus)) {
                    if ("SUPERVISED".equals(userStatus)) {
                        resultData.put("isError", false);
                        resultData.put("isSupervised", true);
                        resultData.put("userStatus", "supervised");
                        resultData.put("requiresParentalApproval", true);
                    } else {
                        resultData.put("isError", false);
                        resultData.put("isSupervised", false);
                        resultData.put("requiresParentalApproval", false);
                    }
                } else {
                    resultData.put("isError", true);
                    resultData.put("errorMessage", "Amazon Age Data API returned status: " + responseStatus);
                }

                dispatchCommunicationEvent(resultData);

            } catch (Exception e) {
                Map<String, Object> errorData = new HashMap<>(eventData);
                errorData.put("isError", true);
                errorData.put("errorMessage", "Amazon Age Data API error: " + e.getMessage());
                dispatchCommunicationEvent(errorData);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });

        return 0;
    }

    /**
     * Start listening for communication responses
     * NOTE: Amazon doesn't have a real-time listener for parental responses
     * This is a no-op function for API compatibility
     */
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    public int startListeningForCommunicationResponses(LuaState L) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("isError", false);
        eventData.put("platform", "android");
        eventData.put("store", "amazon");
        eventData.put("message", "Amazon doesn't support real-time response listening. " +
                "Call requestAgeRange() periodically to check for status updates.");
        dispatchCommunicationEvent(eventData);

        return 0;
    }

    /** Implements the library.init() Lua function. */
    private class InitWrapper implements NamedJavaFunction {
        @Override
        public String getName() {
            return "init";
        }

        @Override
        public int invoke(LuaState L) {
            return init(L);
        }
    }

    /** Implements the library.requestAgeRange() Lua function. */
    private class RequestAgeRangeWrapper implements NamedJavaFunction {
        @Override
        public String getName() {
            return "requestAgeRange";
        }

        @Override
        public int invoke(LuaState L) {
            return requestAgeRange(L);
        }
    }

    /** Implements the library.requestSignificantUpdatePermission() Lua function. */
    private class RequestSignificantUpdatePermissionWrapper implements NamedJavaFunction {
        @Override
        public String getName() {
            return "requestSignificantUpdatePermission";
        }

        @Override
        public int invoke(LuaState L) {
            return requestSignificantUpdatePermission(L);
        }
    }

    /** Implements the library.requestCommunicationPermission() Lua function. */
    private class RequestCommunicationPermissionWrapper implements NamedJavaFunction {
        @Override
        public String getName() {
            return "requestCommunicationPermission";
        }

        @Override
        public int invoke(LuaState L) {
            return requestCommunicationPermission(L);
        }
    }

    /** Implements the library.startListeningForCommunicationResponses() Lua function. */
    private class StartListeningForCommunicationResponsesWrapper implements NamedJavaFunction {
        @Override
        public String getName() {
            return "startListeningForCommunicationResponses";
        }

        @Override
        public int invoke(LuaState L) {
            return startListeningForCommunicationResponses(L);
        }
    }
}