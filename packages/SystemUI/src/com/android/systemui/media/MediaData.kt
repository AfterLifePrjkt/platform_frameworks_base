/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.media

import android.app.PendingIntent
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import com.android.systemui.R

/** State of a media view. */
data class MediaData(
    val userId: Int,
    val initialized: Boolean = false,
    val backgroundColor: Int,
    /**
     * App name that will be displayed on the player.
     */
    val app: String?,
    /**
     * App icon shown on player.
     */
    val appIcon: Icon?,
    /**
     * Artist name.
     */
    val artist: CharSequence?,
    /**
     * Song name.
     */
    val song: CharSequence?,
    /**
     * Album artwork.
     */
    val artwork: Icon?,
    /**
     * List of generic action buttons for the media player, based on notification actions
     */
    val actions: List<MediaAction>,
    /**
     * Same as above, but shown on smaller versions of the player, like in QQS or keyguard.
     */
    val actionsToShowInCompact: List<Int>,
    /**
     * Semantic actions buttons, based on the PlaybackState of the media session.
     * If present, these actions will be preferred in the UI over [actions]
     */
    val semanticActions: MediaButton? = null,
    /**
     * Package name of the app that's posting the media.
     */
    val packageName: String,
    /**
     * Unique media session identifier.
     */
    val token: MediaSession.Token?,
    /**
     * Action to perform when the player is tapped.
     * This is unrelated to {@link #actions}.
     */
    val clickIntent: PendingIntent?,
    /**
     * Where the media is playing: phone, headphones, ear buds, remote session.
     */
    val device: MediaDeviceData?,
    /**
     * When active, a player will be displayed on keyguard and quick-quick settings.
     * This is unrelated to the stream being playing or not, a player will not be active if
     * timed out, or in resumption mode.
     */
    var active: Boolean,
    /**
     * Action that should be performed to restart a non active session.
     */
    var resumeAction: Runnable?,
    /**
     * Playback location: one of PLAYBACK_LOCAL, PLAYBACK_CAST_LOCAL, or PLAYBACK_CAST_REMOTE
     */
    var playbackLocation: Int = PLAYBACK_LOCAL,
    /**
     * Indicates that this player is a resumption player (ie. It only shows a play actions which
     * will start the app and start playing).
     */
    var resumption: Boolean = false,
    /**
     * Notification key for cancelling a media player after a timeout (when not using resumption.)
     */
    val notificationKey: String? = null,
    var hasCheckedForResume: Boolean = false,

    /**
     * If apps do not report PlaybackState, set as null to imply 'undetermined'
     */
    val isPlaying: Boolean? = null,

    /**
     * Set from the notification and used as fallback when PlaybackState cannot be determined
     */
    val isClearable: Boolean = true,

    /**
     * Timestamp when this player was last active.
     */
    var lastActive: Long = 0L
) {
    companion object {
        /** Media is playing on the local device */
        const val PLAYBACK_LOCAL = 0
        /** Media is cast but originated on the local device */
        const val PLAYBACK_CAST_LOCAL = 1
        /** Media is from a remote cast notification */
        const val PLAYBACK_CAST_REMOTE = 2
    }

    fun isLocalSession(): Boolean {
        return playbackLocation == PLAYBACK_LOCAL
    }
}

/**
 * Contains [MediaAction] objects which represent specific buttons in the UI
 */
data class MediaButton(
    /**
     * Play/pause button
     */
    var playOrPause: MediaAction? = null,
    /**
     * Next button, or custom action
     */
    var nextOrCustom: MediaAction? = null,
    /**
     * Previous button, or custom action
     */
    var prevOrCustom: MediaAction? = null,
    /**
     * First custom action space
     */
    var custom0: MediaAction? = null,
    /**
     * Second custom action space
     */
    var custom1: MediaAction? = null
) {
    fun getActionById(id: Int): MediaAction? {
        return when (id) {
            R.id.actionPlayPause -> playOrPause
            R.id.actionNext -> nextOrCustom
            R.id.actionPrev -> prevOrCustom
            R.id.action0 -> custom0
            R.id.action1 -> custom1
            else -> null
        }
    }
}

/** State of a media action. */
data class MediaAction(
    val icon: Drawable?,
    val action: Runnable?,
    val contentDescription: CharSequence?,
    val background: Drawable?
)

/** State of the media device. */
data class MediaDeviceData(
    val enabled: Boolean,
    val icon: Drawable?,
    val name: String?
)
