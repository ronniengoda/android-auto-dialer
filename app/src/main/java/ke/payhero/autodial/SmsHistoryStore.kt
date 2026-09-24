package ke.payhero.autodial

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class SmsRecord(
    val id: Long,
    val sender: String,
    val text: String,
    val sentStamp: Long,
    val receivedStamp: Long,
    val sim: String,
    val status: String,
    val error: String?,
    val attempts: Int,
    val forwardedStamp: Long
)

class SmsHistoryStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sms_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                body TEXT NOT NULL,
                sent_stamp INTEGER NOT NULL,
                received_stamp INTEGER NOT NULL,
                sim TEXT NOT NULL,
                status TEXT NOT NULL,
                error TEXT,
                attempts INTEGER NOT NULL DEFAULT 0,
                forwarded_stamp INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE sms_history ADD COLUMN forwarded_stamp INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "UPDATE sms_history SET forwarded_stamp = received_stamp WHERE forwarded_stamp = 0 AND status != 'pending'"
            )
        }
    }

    fun insertPending(
        sender: String,
        text: String,
        sentStamp: Long,
        receivedStamp: Long,
        sim: String
    ): Long {
        val values = ContentValues().apply {
            put("sender", sender)
            put("body", text)
            put("sent_stamp", sentStamp)
            put("received_stamp", receivedStamp)
            put("sim", sim)
            put("status", STATUS_PENDING)
            put("attempts", 0)
        }
        val id = writableDatabase.insert("sms_history", null, values)
        trim()
        return id
    }

    fun mark(id: Long, status: String, error: String?) {
        val values = ContentValues().apply {
            put("status", status)
            put("error", error)
            put("attempts", currentAttempts(id) + 1)
            put("forwarded_stamp", System.currentTimeMillis())
        }
        writableDatabase.update("sms_history", values, "id=?", arrayOf(id.toString()))
    }

    fun count(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM sms_history", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun countWhere(status: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM sms_history WHERE status=?",
            arrayOf(status)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun page(pageIndex: Int, pageSize: Int): List<SmsRecord> {
        val offset = pageIndex.coerceAtLeast(0) * pageSize
        return readableDatabase.query(
            "sms_history",
            null,
            null,
            null,
            null,
            null,
            "id DESC",
            "$pageSize OFFSET $offset"
        ).use { readAll(it) }
    }

    fun recent(limit: Int = 80): List<SmsRecord> {
        return readableDatabase.query(
            "sms_history",
            null,
            null,
            null,
            null,
            null,
            "id DESC",
            limit.toString()
        ).use { readAll(it) }
    }

    fun retryable(): List<SmsRecord> {
        return readableDatabase.query(
            "sms_history",
            null,
            "status IN (?,?)",
            arrayOf(STATUS_PENDING, STATUS_FAILED),
            null,
            null,
            "id ASC"
        ).use { readAll(it) }
    }

    fun get(id: Long): SmsRecord? {
        readableDatabase.query(
            "sms_history",
            null,
            "id=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return readAll(cursor).firstOrNull()
        }
    }

    private fun readAll(cursor: android.database.Cursor): List<SmsRecord> {
        val records = mutableListOf<SmsRecord>()
        val id = cursor.getColumnIndexOrThrow("id")
        val sender = cursor.getColumnIndexOrThrow("sender")
        val body = cursor.getColumnIndexOrThrow("body")
        val sent = cursor.getColumnIndexOrThrow("sent_stamp")
        val received = cursor.getColumnIndexOrThrow("received_stamp")
        val sim = cursor.getColumnIndexOrThrow("sim")
        val status = cursor.getColumnIndexOrThrow("status")
        val error = cursor.getColumnIndexOrThrow("error")
        val attempts = cursor.getColumnIndexOrThrow("attempts")
        val forwarded = cursor.getColumnIndexOrThrow("forwarded_stamp")
        while (cursor.moveToNext()) {
            records.add(
                SmsRecord(
                    id = cursor.getLong(id),
                    sender = cursor.getString(sender),
                    text = cursor.getString(body),
                    sentStamp = cursor.getLong(sent),
                    receivedStamp = cursor.getLong(received),
                    sim = cursor.getString(sim),
                    status = cursor.getString(status),
                    error = cursor.getString(error),
                    attempts = cursor.getInt(attempts),
                    forwardedStamp = cursor.getLong(forwarded)
                )
            )
        }
        return records
    }

    private fun currentAttempts(id: Long): Int {
        readableDatabase.query(
            "sms_history",
            arrayOf("attempts"),
            "id=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun trim() {
        writableDatabase.execSQL(
            "DELETE FROM sms_history WHERE id NOT IN (SELECT id FROM sms_history ORDER BY id DESC LIMIT 200)"
        )
    }

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAILED = "failed"
        private const val DB_NAME = "sms_forward.db"
        private const val DB_VERSION = 2

        @Volatile
        private var instance: SmsHistoryStore? = null

        fun get(context: Context): SmsHistoryStore {
            return instance ?: synchronized(this) {
                instance ?: SmsHistoryStore(context).also { instance = it }
            }
        }
    }
}
