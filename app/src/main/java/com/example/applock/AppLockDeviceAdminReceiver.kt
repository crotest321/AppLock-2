package com.example.applock

import android.app.admin.DeviceAdminReceiver

/**
 * This receiver only matters if the user has gone through the advanced,
 * optional "device owner" setup described in README.md. Being a plain
 * device ADMIN (the easy, in-app toggle) is NOT enough to hide another
 * app's icon — that specifically requires device OWNER status, which
 * Android only grants via a one-time ADB command on a freshly-reset or
 * never-configured account. Without that, this app still works fully for
 * locking; "hide" simply stays a soft, in-app hide.
 */
class AppLockDeviceAdminReceiver : DeviceAdminReceiver()
