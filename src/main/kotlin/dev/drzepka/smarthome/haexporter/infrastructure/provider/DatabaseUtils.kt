package dev.drzepka.smarthome.haexporter.infrastructure.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet
import java.sql.Statement

suspend fun Statement.executeAsync(sql: String): Boolean = withContext(Dispatchers.IO) { execute(sql) }

suspend fun Statement.executeQueryAsync(sql: String): ResultSet = withContext(Dispatchers.IO) { executeQuery(sql) }
