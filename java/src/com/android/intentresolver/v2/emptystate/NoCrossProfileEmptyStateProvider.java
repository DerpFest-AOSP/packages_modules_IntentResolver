/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.intentresolver.v2.emptystate;

import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.intentresolver.ResolverListAdapter;
import com.android.intentresolver.emptystate.CrossProfileIntentsChecker;
import com.android.intentresolver.emptystate.EmptyState;
import com.android.intentresolver.emptystate.EmptyStateProvider;
import com.android.intentresolver.v2.ProfileHelper;
import com.android.intentresolver.v2.shared.model.Profile;
import com.android.intentresolver.v2.shared.model.User;

import java.util.List;

/**
 * Empty state provider that does not allow cross profile sharing, it will return a blocker
 * in case if the profile of the current tab is not the same as the profile of the calling app.
 */
public class NoCrossProfileEmptyStateProvider implements EmptyStateProvider {

    private final ProfileHelper mProfileHelper;
    private final EmptyState mNoWorkToPersonalEmptyState;
    private final EmptyState mNoPersonalToWorkEmptyState;
    private final CrossProfileIntentsChecker mCrossProfileIntentsChecker;

    public NoCrossProfileEmptyStateProvider(
            ProfileHelper profileHelper,
            EmptyState noWorkToPersonalEmptyState,
            EmptyState noPersonalToWorkEmptyState,
            CrossProfileIntentsChecker crossProfileIntentsChecker) {
        mProfileHelper = profileHelper;
        mNoWorkToPersonalEmptyState = noWorkToPersonalEmptyState;
        mNoPersonalToWorkEmptyState = noPersonalToWorkEmptyState;
        mCrossProfileIntentsChecker = crossProfileIntentsChecker;
    }

    private boolean anyCrossProfileAllowedIntents(ResolverListAdapter selected, UserHandle source) {
        List<Intent> intents = selected.getIntents();
        UserHandle target = selected.getUserHandle();
        return mCrossProfileIntentsChecker.hasCrossProfileIntents(intents,
                source.getIdentifier(), target.getIdentifier());
    }

    @Nullable
    @Override
    public EmptyState getEmptyState(ResolverListAdapter adapter) {
        Profile launchedAsProfile = mProfileHelper.getLaunchedAsProfile();
        User launchedAs = mProfileHelper.getLaunchedAsProfile().getPrimary();
        UserHandle tabOwnerHandle = adapter.getUserHandle();
        boolean launchedAsSameUser = launchedAs.getHandle().equals(tabOwnerHandle);
        Profile.Type tabOwnerType = mProfileHelper.findProfileType(tabOwnerHandle);

        // Not applicable for private profile.
        if (launchedAsProfile.getType() == Profile.Type.PRIVATE
                || tabOwnerType == Profile.Type.PRIVATE) {
            return null;
        }

        // Allow access to the tab when launched by the same user as the tab owner
        // or when there is at least one target which is permitted for cross-profile.
        if (launchedAsSameUser || anyCrossProfileAllowedIntents(adapter, tabOwnerHandle)) {
            return null;
        }

        switch (launchedAsProfile.getType()) {
            case WORK:  return mNoWorkToPersonalEmptyState;
            case PERSONAL: return mNoPersonalToWorkEmptyState;
        }
        return null;
    }

    /**
     * Empty state that gets strings from the device policy manager and tracks events into
     * event logger of the device policy events.
     */
    public static class DevicePolicyBlockerEmptyState implements EmptyState {

        @NonNull
        private final Context mContext;
        private final String mDevicePolicyStringTitleId;
        @StringRes
        private final int mDefaultTitleResource;
        private final String mDevicePolicyStringSubtitleId;
        @StringRes
        private final int mDefaultSubtitleResource;
        private final int mEventId;
        @NonNull
        private final String mEventCategory;

        public DevicePolicyBlockerEmptyState(@NonNull Context context,
                String devicePolicyStringTitleId, @StringRes int defaultTitleResource,
                String devicePolicyStringSubtitleId, @StringRes int defaultSubtitleResource,
                int devicePolicyEventId, @NonNull String devicePolicyEventCategory) {
            mContext = context;
            mDevicePolicyStringTitleId = devicePolicyStringTitleId;
            mDefaultTitleResource = defaultTitleResource;
            mDevicePolicyStringSubtitleId = devicePolicyStringSubtitleId;
            mDefaultSubtitleResource = defaultSubtitleResource;
            mEventId = devicePolicyEventId;
            mEventCategory = devicePolicyEventCategory;
        }

        @Nullable
        @Override
        public String getTitle() {
            return mContext.getSystemService(DevicePolicyManager.class).getResources().getString(
                    mDevicePolicyStringTitleId,
                    () -> mContext.getString(mDefaultTitleResource));
        }

        @Nullable
        @Override
        public String getSubtitle() {
            return mContext.getSystemService(DevicePolicyManager.class).getResources().getString(
                    mDevicePolicyStringSubtitleId,
                    () -> mContext.getString(mDefaultSubtitleResource));
        }

        @Override
        public void onEmptyStateShown() {
            DevicePolicyEventLogger.createEvent(mEventId)
                    .setStrings(mEventCategory)
                    .write();
        }

        @Override
        public boolean shouldSkipDataRebuild() {
            return true;
        }
    }
}
