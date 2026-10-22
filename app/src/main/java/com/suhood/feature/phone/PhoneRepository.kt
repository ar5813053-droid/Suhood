package com.suhood.feature.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContact(val id: String, val name: String, val number: String, val starred: Boolean)

data class RecentCall(
    val id: String, val number: String, val name: String?,
    val type: Int, val dateMs: Long, val durationSec: Long
) {
    val typeLabel: String
        get() = when (type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE -> "Missed"
            CallLog.Calls.REJECTED_TYPE -> "Rejected"
            else -> "Call"
        }
}

class PhoneRepository(private val context: Context) {
    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    suspend fun loadContacts(query: String = ""): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val out = mutableListOf<PhoneContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )
        val selection = if (query.isBlank()) null
        else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val args = if (query.isBlank()) null else arrayOf("%$query%", "%$query%")
        context.contentResolver.query(uri, projection, selection, args, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")?.use { c ->
            val iId = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val iName = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val iNum = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val iStar = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            while (c.moveToNext() && out.size < 500) {
                val num = PhoneNumberUtils.normalize(c.getString(iNum).orEmpty())
                if (num.isBlank()) continue
                out += PhoneContact(c.getString(iId).orEmpty(), c.getString(iName).orEmpty().ifBlank { num }, num, c.getInt(iStar) == 1)
            }
        }
        out.distinctBy { it.number }
    }

    suspend fun loadRecents(filter: String? = null): List<RecentCall> = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext emptyList()
        val out = mutableListOf<RecentCall>()
        val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION)
        context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC")?.use { c ->
            val iId = c.getColumnIndex(CallLog.Calls._ID)
            val iNum = c.getColumnIndex(CallLog.Calls.NUMBER)
            val iName = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val iType = c.getColumnIndex(CallLog.Calls.TYPE)
            val iDate = c.getColumnIndex(CallLog.Calls.DATE)
            val iDur = c.getColumnIndex(CallLog.Calls.DURATION)
            while (c.moveToNext() && out.size < 200) {
                val type = c.getInt(iType)
                if (filter == "missed" && type != CallLog.Calls.MISSED_TYPE) continue
                if (filter == "incoming" && type != CallLog.Calls.INCOMING_TYPE) continue
                if (filter == "outgoing" && type != CallLog.Calls.OUTGOING_TYPE) continue
                out += RecentCall(c.getString(iId).orEmpty(), PhoneNumberUtils.normalize(c.getString(iNum).orEmpty()), c.getString(iName), type, c.getLong(iDate), c.getLong(iDur))
            }
        }
        out
    }

    fun placeCall(rawNumber: String): Boolean {
        val number = PhoneNumberUtils.normalize(rawNumber)
        if (!PhoneNumberUtils.isValidForDial(number)) return false
        if (PhonePrefs(context).isBlocked(number)) return false
        return try {
            val prefs = PhonePrefs(context)
            if (hasCallPermission()) {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val uri = Uri.fromParts("tel", number, null)
                val extras = android.os.Bundle()
                val simId = prefs.preferredSimId
                if (simId >= 0 && android.os.Build.VERSION.SDK_INT >= 23) {
                    extras.putParcelable(
                        TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                        tm?.callCapablePhoneAccounts?.getOrNull(simId)
                    )
                }
                tm?.placeCall(uri, extras)
                    ?: context.startActivity(
                        Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
            true
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
                true
            } catch (_: Exception) { false }
        }
    }

    fun listSims(): List<Pair<Int, String>> {
        return try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return emptyList()
            tm.callCapablePhoneAccounts.mapIndexed { i, h ->
                i to (h.id ?: "SIM ${i + 1}")
            }
        } catch (_: Exception) { emptyList() }
    }

    fun isDefaultDialer(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(android.app.role.RoleManager::class.java)
            if (rm != null && rm.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)) return true
        }
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return tm.defaultDialerPackage == context.packageName
    }
}
