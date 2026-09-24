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
    val attempts: Int
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
                attempts INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sms_history")
        onCreate(db)
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
        }
        writableDatabase.update("sms_history", values, "id=?", arrayOf(id.toString()))
    }

    fun recent(limit: Int = 80): List<SmsRecord> {
        val records = mutableListOf<SmsRecord>()
        readableDatabase.query(
            "sms_history",
            null,
            null,
            null,
            null,
            null,
            "id DESC",
            limit.toString()
        ).use { cursor ->
            val id = cursor.getColumnIndexOrThrow("id")
            val sender = cursor.getColumnIndexOrThrow("sender")
            val body = cursor.getColumnIndexOrThrow("body")
            val sent = cursor.getColumnIndexOrThrow("sent_stamp")
            val received = cursor.getColumnIndexOrThrow("received_stamp")
            val sim = cursor.getColumnIndexOrThrow("sim")
            val status = cursor.getColumnIndexOrThrow("status")
            val error = cursor.getColumnIndexOrThrow("error")
            val attempts = cursor.getColumnIndexOrThrow("attempts")
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
                        attempts = cursor.getInt(attempts)
                    )
                )
            }
        }
        return records
    }

    fun retryable(): List<SmsRecord> {
        val records = mutableListOf<SmsRecord>()
        readableDatabase.query(
            "sms_history",
            null,
            "status IN (?,?)",
            arrayOf(STATUS_PENDING, STATUS_FAILED),
            null,
            null,
            "id ASC"
        ).use { cursor ->
            val id = cursor.getColumnIndexOrThrow("id")
            val sender = cursor.getColumnIndexOrThrow("sender")
            val body = cursor.getColumnIndexOrThrow("body")
            val sent = cursor.getColumnIndexOrThrow("sent_stamp")
            val received = cursor.getColumnIndexOrThrow("received_stamp")
            val sim = cursor.getColumnIndexOrThrow("sim")
            val status = cursor.getColumnIndexOrThrow("status")
            val error = cursor.getColumnIndexOrThrow("error")
            val attempts = cursor.getColumnIndexOrThrow("attempts")
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
                        attempts = cursor.getInt(attempts)
                    )
                )
            }
        }
        return records
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
            if (!cursor.moveToFirst()) return null
            return SmsRecord(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                sender = cursor.getString(cursor.getColumnIndexOrThrow("sender")),
                text = cursor.getString(cursor.getColumnIndexOrThrow("body")),
                sentStamp = cursor.getLong(cursor.getColumnIndexOrThrow("sent_stamp")),
                receivedStamp = cursor.getLong(cursor.getColumnIndexOrThrow("received_stamp")),
                sim = cursor.getString(cursor.getColumnIndexOrThrow("sim")),
                status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                error = cursor.getString(cursor.getColumnIndexOrThrow("error")),
                attempts = cursor.getInt(cursor.getColumnIndexOrThrow("attempts"))
            )
        }
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
        private const val DB_VERSION = 1

        @Volatile
        private var instance: SmsHistoryStore? = null

        fun get(context: Context): SmsHistoryStore {
            return instance ?: synchronized(this) {
                instance ?: SmsHistoryStore(context).also { instance = it }
            }
        }
    }
}
