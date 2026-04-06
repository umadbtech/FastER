package com.faster.festival.utils

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

/**
 * Represents a contact loaded from the device's address book.
 */
data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val normalizedPhone: String
)

/**
 * Queries device contacts via ContentResolver.
 * Handles duplicates, contacts without phone numbers, and phone normalization.
 */
object DeviceContactsHelper {

    private const val TAG = "DeviceContactsHelper"

    /**
     * Search device contacts by partial name match.
     * Returns a deduplicated list sorted by name, limited to [limit] results.
     *
     * Each (name, normalizedPhone) pair is unique — if a contact has multiple
     * numbers they appear as separate entries.
     */
    fun searchContacts(
        context: Context,
        query: String,
        limit: Int = 20
    ): List<DeviceContact> {
        if (query.isBlank() || query.length < 2) return emptyList()

        if (!PermissionUtils.hasContactsPermission(context)) {
            Log.w(TAG, "READ_CONTACTS permission not granted")
            return emptyList()
        }

        val results = mutableListOf<DeviceContact>()
        val seen = mutableSetOf<String>() // "name|normalizedPhone" dedup key

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext() && results.size < limit) {
                    val contactId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx)?.trim() ?: continue
                    val rawPhone = cursor.getString(phoneIdx)?.trim() ?: continue

                    if (name.isBlank() || rawPhone.isBlank()) continue

                    // Normalize the phone number for dedup + validation
                    val normalized = try {
                        PhoneNumberUtils.normalizeToE164(rawPhone)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not normalize phone for $name: $rawPhone")
                        continue
                    }

                    if (!PhoneNumberUtils.isValidE164(normalized)) {
                        Log.w(TAG, "Invalid E.164 after normalization for $name: $rawPhone -> $normalized")
                        continue
                    }

                    val dedupKey = "${name.lowercase()}|$normalized"
                    if (dedupKey in seen) continue
                    seen.add(dedupKey)

                    results.add(
                        DeviceContact(
                            id = contactId,
                            name = name,
                            phoneNumber = rawPhone,
                            normalizedPhone = normalized
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException querying contacts — permission likely revoked", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device contacts", e)
        }

        return results
    }
}
