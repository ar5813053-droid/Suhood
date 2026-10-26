package com.suhood.feature.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.content.ContentProviderOperation
import android.content.ContentUris
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


    fun hasWriteContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun addContact(firstName: String, lastName: String, company: String, phone: String, email: String): Boolean {
        if (!hasWriteContactsPermission()) return false
        val display = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { phone }
        val ops = ArrayList<ContentProviderOperation>()
        ops += ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
            .build()
        if (phone.isNotBlank()) {
            ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        }
        if (email.isNotBlank()) {
            ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                .build()
        }
        if (company.isNotBlank()) {
            ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                .build()
        }
        return try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (_: Exception) { false }
    }

    fun deleteContacts(ids: Collection<String>): Int {
        if (!hasWriteContactsPermission() || ids.isEmpty()) return 0
        var n = 0
        for (id in ids) {
            try {
                val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id.toLongOrNull() ?: continue)
                n += context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
        }
        return n
    }


    fun updateContact(contactId: String, firstName: String, lastName: String, phone: String, email: String): Boolean {
        if (!hasWriteContactsPermission()) return false
        val id = contactId.toLongOrNull() ?: return false
        val display = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { phone }
        try {
            val values = android.content.ContentValues().apply {
                put(ContactsContract.Contacts.DISPLAY_NAME, display)
            }
            context.contentResolver.update(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
                values, null, null
            )
            // Update structured name
            val nameWhere = "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
            val nameArgs = arrayOf(contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            val nameVals = android.content.ContentValues().apply {
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
            }
            val updatedName = context.contentResolver.update(ContactsContract.Data.CONTENT_URI, nameVals, nameWhere, nameArgs)
            if (updatedName == 0) {
                // insert name row via raw contact
                val rawId = rawContactId(id) ?: return true
                val ops = ArrayList<ContentProviderOperation>()
                ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
                    .build()
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
            if (phone.isNotBlank()) {
                val phoneWhere = "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
                val phoneArgs = arrayOf(contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                val phoneVals = android.content.ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                }
                val u = context.contentResolver.update(ContactsContract.Data.CONTENT_URI, phoneVals, phoneWhere, phoneArgs)
                if (u == 0) {
                    val rawId = rawContactId(id) ?: return true
                    val ops = ArrayList<ContentProviderOperation>()
                    ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                }
            }
            if (email.isNotBlank()) {
                val emailWhere = "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
                val emailArgs = arrayOf(contactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                val emailVals = android.content.ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                }
                val u = context.contentResolver.update(ContactsContract.Data.CONTENT_URI, emailVals, emailWhere, emailArgs)
                if (u == 0) {
                    val rawId = rawContactId(id) ?: return true
                    val ops = ArrayList<ContentProviderOperation>()
                    ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build()
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                }
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun rawContactId(contactId: Long): Long? {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return null
    }

    fun searchContactsByDigits(digits: String, limit: Int = 12): List<PhoneContact> {
        if (!hasContactsPermission() || digits.isBlank()) return emptyList()
        val norm = digits.filter { it.isDigit() || it == '+' }
        if (norm.isEmpty()) return emptyList()
        val out = mutableListOf<PhoneContact>()
        val seen = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { c ->
                val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext() && out.size < limit) {
                    val num = c.getString(numIdx) ?: continue
                    val digitsOnly = num.filter { it.isDigit() }
                    val q = norm.filter { it.isDigit() }
                    if (q.isEmpty()) continue
                    if (!digitsOnly.contains(q) && !q.contains(digitsOnly.takeLast(q.length.coerceAtMost(digitsOnly.length)))) continue
                    val id = c.getString(idIdx) ?: continue
                    val key = id + "|" + num
                    if (key in seen) continue
                    seen += key
                    out += PhoneContact(id = id, name = c.getString(nameIdx) ?: num, number = num, starred = false)
                }
            }
        } catch (_: Exception) {}
        return out
    }

    fun openSms(number: String, body: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:" + PhoneNumberUtils.normalize(number).ifBlank { number })
                if (!body.isNullOrBlank()) putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("sms:" + number)
                    if (!body.isNullOrBlank()) putExtra("sms_body", body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
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
