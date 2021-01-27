package com.enigma.migrationhelper

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase

data class ColumnData(
    val key: String, val type: String, val isNotNull: Boolean = false,
    val defaultValue: String? = null, val isPrimaryKey: Boolean = false
) {

    val toScript
        get() = "`$key` $type" + (if (isNotNull) " NOT NULL" else "") +
                if (defaultValue != null) " DEFAULT $defaultValue" else ""

    override fun equals(other: Any?): Boolean {
        return other is ColumnData && other.key == key
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + isNotNull.hashCode()
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        result = 31 * result + isPrimaryKey.hashCode()
        return result
    }
}

data class IndexData(val indexName: String, val indexKey: String, val unique: Boolean = false) {
    fun getScript(tableName: String) =
        "CREATE INDEX IF NOT EXISTS" + if (unique) " UNIQUE" else "" +
                " `${indexName}` ON  ${tableName}(`${indexKey}`)"
}

fun SupportSQLiteDatabase.alterTable(
    tableName: String,
    dropColumns: Set<ColumnData>,
    addColumns: Set<ColumnData>
) {

    val existingColumns = query("PRAGMA table_info(\"${tableName}\")", null)
        .use { cursor ->
            cursor.moveToFirst()
            val mutableSetOf = mutableSetOf<ColumnData>()
            do {
                val name = cursor.getString(1)
                val type = cursor.getString(2)
                val isNotNull = cursor.getInt(3) == 1
                val defaultValue = cursor.getString(4)
                val isPrimaryKey = cursor.getInt(5) == 1
                val element = ColumnData(name, type, isNotNull, defaultValue, isPrimaryKey)
                mutableSetOf.add(element)
            } while (cursor.moveToNext())
            return@use mutableSetOf.toSet()
        }

    val indices = query("PRAGMA index_list(\"${tableName}\")").use { cursor ->
        cursor.moveToFirst()
        val indexes = mutableSetOf<IndexData>()
        do {
            val indexName = cursor.getString(cursor.getColumnIndex("name"))
            val unique = cursor.getInt(cursor.getColumnIndex("unique")) == 1
            val indexOn = query("PRAGMA index_info(\"${indexName}\")")
                .use {
                    it.moveToFirst()
                    it.getString(it.getColumnIndex("name"))
                }
            indexes.add(IndexData(indexName, indexOn, unique))
        } while (cursor.moveToNext())
        return@use indexes.toList()
    }

    val totalColumns = existingColumns union addColumns subtract dropColumns

    val primaryKeys = totalColumns.filter { it.isPrimaryKey }.toSet()

    val tempDbName = "${tableName}_temp"
    val tableCreateScript = "CREATE TABLE $tempDbName (" +
            totalColumns.joinToString { it.toScript } +
            ", PRIMARY KEY(${primaryKeys.joinToString { it.key }}))"


    execSQL(tableCreateScript)

    val existingNotDropped = existingColumns subtract dropColumns

    val insertionScript = "INSERT INTO $tempDbName (" +
            existingNotDropped.joinToString { "`${it.key}`" } + ") " +
            "SELECT " + existingNotDropped.joinToString { "`${it.key}`" } + " FROM $tableName"

    execSQL(insertionScript)

    execSQL("DROP TABLE $tableName")
    execSQL("ALTER TABLE $tempDbName RENAME TO $tableName")


    if (indices.isNotEmpty()) {
        for (index in indices) {
            val script = index.getScript(tableName)
            execSQL(script)
        }
    }
}

fun Cursor.stringifyCursor(): String {
    moveToFirst()
    val sb = StringBuilder()
    do {
        val columnsQty = columnCount
        val temp = StringBuilder()
        for (idx in 0 until columnsQty) {
            temp.append(getString(idx))
            if (idx < columnsQty - 1) temp.append("; ")
        }
        sb.append(
            "xxxx " + String.format(
                "Row: %d, Values: %s", position,
                temp.toString()
            )
        )
        sb.append("\n")
    } while (moveToNext())
    moveToFirst()
    return sb.toString()
}