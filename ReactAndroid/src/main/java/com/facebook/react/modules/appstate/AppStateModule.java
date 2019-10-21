/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.modules.appstate;

import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReactSoftException;
import com.facebook.react.bridge.WindowFocusChangeListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@ReactModule(name = AppStateModule.NAME)
public class AppStateModule extends ReactContextBaseJavaModule
    implements LifecycleEventListener, WindowFocusChangeListener {
  public static final String TAG = AppStateModule.class.getSimpleName();

  public static final String NAME = "AppState";

  public static final String APP_STATE_ACTIVE = "active";
  public static final String APP_STATE_BACKGROUND = "background";

  private static final String INITIAL_STATE = "initialAppState";

  private String mAppState;

  public AppStateModule(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addLifecycleEventListener(this);
    reactContext.addWindowFocusChangeListener(this);
    mAppState =
        (reactContext.getLifecycleState() == LifecycleState.RESUMED
            ? APP_STATE_ACTIVE
            : APP_STATE_BACKGROUND);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Map<String, Object> getConstants() {
    HashMap<String, Object> constants = new HashMap<>();
    constants.put(INITIAL_STATE, mAppState);
    return constants;
  }

  @ReactMethod
  public void getCurrentAppState(Callback success, Callback error) {
    success.invoke(createAppStateEventMap());
  }

  @Override
  public void onHostResume() {
    mAppState = APP_STATE_ACTIVE;
    sendAppStateChangeEvent();
  }

  @Override
  public void onHostPause() {
    mAppState = APP_STATE_BACKGROUND;
    sendAppStateChangeEvent();
  }

  @Override
  public void onHostDestroy() {
    // do not set state to destroyed, do not send an event. By the current implementation, the
    // catalyst instance is going to be immediately dropped, and all JS calls with it.
  }

  @Override
  public void onWindowFocusChange(boolean hasFocus) {
    sendEvent("appStateFocusChange", hasFocus);
  }

  private WritableMap createAppStateEventMap() {
    WritableMap appState = Arguments.createMap();
    appState.putString("app_state", mAppState);
    return appState;
  }

  private void sendEvent(String eventName, @Nullable Object data) {
    ReactApplicationContext reactApplicationContext = getReactApplicationContext();

    if (reactApplicationContext.hasActiveCatalystInstance()) {
      reactApplicationContext.getJSModule(RCTDeviceEventEmitter.class).emit(eventName, data);
    } else {
      // We want to collect data about how often this happens, but this will cause a crash
      // in debug, which isn't desirable.
      String msg =
          "sendAppStateChangeEvent: trying to update app state when Catalyst Instance has already disappeared: "
              + eventName;
      if (ReactBuildConfig.DEBUG) {
        Log.e(AppStateModule.TAG, msg);
      } else {
        ReactSoftException.logSoftException(AppStateModule.TAG, new RuntimeException(msg));
      }
    }
  }

  private void sendAppStateChangeEvent() {
    sendEvent("appStateDidChange", createAppStateEventMap());
  }
}