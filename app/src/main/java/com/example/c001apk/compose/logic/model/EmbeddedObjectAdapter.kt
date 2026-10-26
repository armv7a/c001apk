package com.example.c001apk.compose.logic.model

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/** Some optional API objects are returned as JSON strings, or as an empty string/zero. */
class EmbeddedObjectAdapter : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val delegate = gson.getAdapter(type)
        return object : TypeAdapter<T>() {
            override fun read(reader: JsonReader): T? {
                val value = JsonParser.parseReader(reader)
                return try {
                    val objectValue =
                        if (value.isJsonPrimitive && value.asJsonPrimitive.isString)
                            JsonParser.parseString(value.asString)
                        else value
                    if (objectValue.isJsonObject) delegate.fromJsonTree(objectValue) else null
                } catch (_: JsonParseException) {
                    null
                } catch (_: IllegalStateException) {
                    null
                } catch (_: NumberFormatException) {
                    null
                }
            }

            override fun write(writer: JsonWriter, value: T?) = delegate.write(writer, value)
        }
    }
}
