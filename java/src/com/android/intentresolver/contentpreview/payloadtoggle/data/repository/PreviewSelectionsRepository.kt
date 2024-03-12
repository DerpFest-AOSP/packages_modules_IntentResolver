/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.intentresolver.contentpreview.payloadtoggle.data.repository

import android.util.Log
import com.android.intentresolver.contentpreview.payloadtoggle.shared.model.PreviewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update

private const val TAG = "PreviewSelectionsRep"

/** Stores set of selected previews. */
@ViewModelScoped
class PreviewSelectionsRepository @Inject constructor() {
    /** Set of selected previews. */
    private val _selections = MutableStateFlow<Set<PreviewModel>?>(null)

    val selections: Flow<Set<PreviewModel>> = _selections.filterNotNull()

    fun setSelection(selection: Set<PreviewModel>) {
        _selections.value = selection
    }

    fun select(item: PreviewModel) {
        _selections.update { selection ->
            selection?.let { it + item }
                ?: run {
                    Log.w(TAG, "Changing selection before it is initialized")
                    null
                }
        }
    }

    fun unselect(item: PreviewModel) {
        _selections.update { selection ->
            selection?.let { it - item }
                ?: run {
                    Log.w(TAG, "Changing selection before it is initialized")
                    null
                }
        }
    }
}
