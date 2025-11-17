//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.ageRange;

import android.app.Activity;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.google.android.play.agesignals.AgeSignalsManager;
import com.google.android.play.agesignals.AgeSignalsManagerFactory;
import com.google.android.play.agesignals.AgeSignalsRequest;
import com.google.android.play.agesignals.AgeSignalsResult;
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus;
import com.google.android.gms.tasks.Task;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;

import java.util.HashMap;
import java.util.Map;

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

	/** Age Signals Manager */
	private AgeSignalsManager ageSignalsManager;

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

		// Initialize Age Signals Manager
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity != null) {
			ageSignalsManager = AgeSignalsManagerFactory.create(activity.getApplicationContext());
		}

		return 1;
	}

	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Reinitialize manager if needed
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity != null && ageSignalsManager == null) {
			ageSignalsManager = AgeSignalsManagerFactory.create(activity.getApplicationContext());
		}
	}

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
	 * Request age range from user
	 * On Android, this uses the Age Signals API which doesn't use age gates like iOS
	 * The gates are ignored but accepted for API compatibility
	 */
	@SuppressWarnings("WeakerAccess")
	public int requestAgeRange(LuaState L) {
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null || ageSignalsManager == null) {
			Map<String, Object> eventData = new HashMap<>();
			eventData.put("isError", true);
			eventData.put("isAvailable", false);
			eventData.put("declined", false);
			eventData.put("errorMessage", "Age Signals not available");
			dispatchAgeRangeEvent(eventData);
			return 0;
		}


		activity.runOnUiThread(() -> {
			AgeSignalsRequest request = AgeSignalsRequest.builder().build();
			Task<AgeSignalsResult> task = ageSignalsManager.checkAgeSignals(request);

			task.addOnSuccessListener(result -> {
				Map<String, Object> eventData = new HashMap<>();
				eventData.put("isError", false);
				eventData.put("isAvailable", true);
				eventData.put("declined", false);

				int status = result.userStatus();

				// Handle different user statuses
				if (status == AgeSignalsVerificationStatus.VERIFIED) {
					// User is verified as 18+
					eventData.put("lowerBound", 18);
					eventData.put("upperBound", null); // No upper bound for 18+
					eventData.put("hasParentalControls", false);
					eventData.put("userStatus", "verified");
				} else if (status == AgeSignalsVerificationStatus.SUPERVISED ||
						status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING ||
						status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED) {
					// Supervised user with age range
					Integer ageLower = result.ageLower();
					Integer ageUpper = result.ageUpper();

					if (ageLower != null) {
						eventData.put("lowerBound", ageLower);
					}
					if (ageUpper != null) {
						eventData.put("upperBound", ageUpper);
					}

					eventData.put("hasParentalControls", true);
					if(status == AgeSignalsVerificationStatus.SUPERVISED){
						eventData.put("userStatus", "supervised");
					} else if (status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING) {
						eventData.put("userStatus", "approvalPending");
					}else{
						eventData.put("userStatus", "approvalDenied");
					}

					// Include additional supervised user info
					if (result.installId() != null) {
						eventData.put("installId", result.installId());
					}
					if (result.mostRecentApprovalDate() != null) {
						eventData.put("mostRecentApprovalDate", result.mostRecentApprovalDate().getTime());
					}
				} else if (status == AgeSignalsVerificationStatus.UNKNOWN) {
					// Unknown status - user needs to verify in Play Store
					eventData.put("userStatus", "unknown");
					eventData.put("errorMessage", "User age status unknown. Please verify in Play Store.");
				} else {
					// Empty status
					eventData.put("userStatus", "empty");
				}

				dispatchAgeRangeEvent(eventData);
			}).addOnFailureListener(e -> {
				Map<String, Object> eventData = new HashMap<>();
				eventData.put("isError", true);
				eventData.put("isAvailable", false);
				eventData.put("declined", false);

				// Parse error code if available
				String errorMessage = e.getMessage();
				if (errorMessage != null) {
					if (errorMessage.contains("API_NOT_AVAILABLE")) {
						eventData.put("errorCode", -1);
						eventData.put("errorMessage", "Age Signals API not available. Please update Play Store.");
					} else if (errorMessage.contains("PLAY_STORE_NOT_FOUND")) {
						eventData.put("errorCode", -2);
						eventData.put("errorMessage", "Play Store not found. Please install or enable Play Store.");
					} else if (errorMessage.contains("NETWORK_ERROR")) {
						eventData.put("errorCode", -3);
						eventData.put("errorMessage", "Network error. Please check connection.");
					} else {
						eventData.put("errorMessage", errorMessage);
					}
				} else {
					eventData.put("errorMessage", "Unknown error occurred");
				}

				dispatchAgeRangeEvent(eventData);
			});
		});

		return 0;
	}

	/**
	 * Request permission for significant app update
	 * NOTE: On Android, significant changes are managed through Play Console, not at runtime
	 * This function is provided for API compatibility but logs a message
	 * Parameters: description (string)
	 */
	@SuppressWarnings("WeakerAccess")
	public int requestSignificantUpdatePermission(LuaState L) {
		String description = L.checkString(1);

		// On Android, significant changes must be submitted through Play Console
		// This is a compatibility function that informs developers
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("isError", false);
		eventData.put("description", description);
		eventData.put("platform", "android");
		eventData.put("message", "On Android, significant changes must be submitted through Play Console. " +
				"This API checks the current approval status instead.");

		// Check current approval status
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity != null && ageSignalsManager != null) {
			activity.runOnUiThread(() -> {
				AgeSignalsRequest request = AgeSignalsRequest.builder().build();
				Task<AgeSignalsResult> task = ageSignalsManager.checkAgeSignals(request);

				task.addOnSuccessListener(result -> {
					Map<String, Object> resultData = new HashMap<>(eventData);
					int status = result.userStatus();

					if (status == AgeSignalsVerificationStatus.SUPERVISED) {
						resultData.put("approved", true);
						if (result.mostRecentApprovalDate() != null) {
							resultData.put("mostRecentApprovalDate", result.mostRecentApprovalDate().getTime());
						}
					} else if (status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING) {
						resultData.put("approved", false);
						resultData.put("pending", true);
					} else if (status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED) {
						resultData.put("approved", false);
						resultData.put("denied", true);
					}

					dispatchUpdateEvent(resultData);
				}).addOnFailureListener(e -> {
					Map<String, Object> errorData = new HashMap<>(eventData);
					errorData.put("isError", true);
					errorData.put("errorMessage", e.getMessage());
					dispatchUpdateEvent(errorData);
				});
			});
		} else {
			eventData.put("isError", true);
			eventData.put("errorMessage", "Age Signals Manager not available");
			dispatchUpdateEvent(eventData);
		}

		return 0;
	}

	/**
	 * Request communication permission
	 * NOTE: Android Age Signals API doesn't have a direct equivalent to iOS PermissionKit
	 * for communication requests. This function checks if the user is supervised.
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
		eventData.put("message", "Android Age Signals API doesn't have direct communication approval. " +
				"Check user supervision status instead.");

		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity != null && ageSignalsManager != null) {
			activity.runOnUiThread(() -> {
				AgeSignalsRequest request = AgeSignalsRequest.builder().build();
				Task<AgeSignalsResult> task = ageSignalsManager.checkAgeSignals(request);

				task.addOnSuccessListener(result -> {
					Map<String, Object> resultData = new HashMap<>(eventData);
					int status = result.userStatus();

					// For supervised users, communication should be restricted
					if (status == AgeSignalsVerificationStatus.SUPERVISED ||
							status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING ||
							status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED) {
						resultData.put("isError", false);
						resultData.put("isSupervised", true);
						if(status == AgeSignalsVerificationStatus.SUPERVISED){
							resultData.put("userStatus", "supervised");
						} else if (status == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING) {
							resultData.put("userStatus", "approvalPending");
						}else{
							resultData.put("userStatus", "approvalDenied");
						}
						// App should implement its own communication approval logic
						resultData.put("requiresParentalApproval", true);
					} else {
						resultData.put("isError", false);
						resultData.put("isSupervised", false);
						resultData.put("requiresParentalApproval", false);
					}

					dispatchCommunicationEvent(resultData);
				}).addOnFailureListener(e -> {
					Map<String, Object> errorData = new HashMap<>(eventData);
					errorData.put("isError", true);
					errorData.put("errorMessage", e.getMessage());
					dispatchCommunicationEvent(errorData);
				});
			});
		} else {
			eventData.put("isError", true);
			eventData.put("errorMessage", "Age Signals Manager not available");
			dispatchCommunicationEvent(eventData);
		}

		return 0;
	}

	/**
	 * Start listening for communication responses
	 * NOTE: Android doesn't have a real-time listener for parental responses like iOS
	 * This is a no-op function for API compatibility
	 */
	@SuppressWarnings({"WeakerAccess", "SameReturnValue"})
	public int startListeningForCommunicationResponses(LuaState L) {
		// Android Age Signals API doesn't support real-time response listening
		// Apps need to call checkAgeSignals() to get updated status
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("isError", false);
		eventData.put("platform", "android");
		eventData.put("message", "Android doesn't support real-time response listening. " +
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