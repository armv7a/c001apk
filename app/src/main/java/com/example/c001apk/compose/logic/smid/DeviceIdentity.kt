package com.example.c001apk.compose.logic.smid

import com.example.c001apk.compose.constant.Constants
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.util.TokenDeviceUtils.encode
import com.example.c001apk.compose.util.TokenDeviceUtils.randHexString
import com.example.c001apk.compose.util.Utils

/** Update the DID, device header and protocol defaults in one DataStore transaction. */
fun UserPreferencesCompat.withDigitalUnionId(id: String): UserPreferencesCompat {
    val maker = manufacturer.ifBlank { Utils.randomManufacturer() }
    val deviceBrand = brand.ifBlank { Utils.randomBrand() }
    val deviceModel = model.ifBlank { Utils.randomDeviceModel() }
    val build = buildNumber.ifBlank { randHexString(16) }
    val release = androidVersion.ifBlank { Utils.randomAndroidVersionRelease() }
    val migrateVersion =
        versionName.isBlank() || (versionName == "13.4.1" && versionCode == "2312121")
    val officialVersion = if (migrateVersion) Constants.VERSION_NAME else versionName
    val officialCode = if (migrateVersion) Constants.VERSION_CODE else versionCode
    return copy(
        szlmId = id,
        manufacturer = maker,
        brand = deviceBrand,
        model = deviceModel,
        buildNumber = build,
        androidVersion = release,
        sdkInt = sdkInt.ifBlank { Utils.randomSdkInt() },
        versionName = officialVersion,
        versionCode = officialCode,
        apiVersion = if (migrateVersion) Constants.API_VERSION else apiVersion,
        xAppDevice =
            encode(
                "$id; ; ; ${Utils.randomMacAddress()}; $maker; $deviceBrand; $deviceModel; $build; null",
            ),
        userAgent =
            "Dalvik/2.1.0 (Linux; U; Android $release; $deviceModel $build) (#Build; $deviceBrand; $deviceModel; $build; $release) +CoolMarket/$officialVersion-$officialCode-${Constants.MODE}",
    )
}
