package com.rescue.flutter_720yun.network

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.rescue.flutter_720yun.adoption.models.V2Response
import java.lang.reflect.ParameterizedType

class V2EnvelopeAdapter: TypeAdapterFactory {
    override fun <T> create(gson: Gson,type: TypeToken<T>): TypeAdapter<T>? {
        if(type.rawType!=V2Response::class.java) return null
        val payload=(type.type as ParameterizedType).actualTypeArguments[0]
        val dataAdapter=gson.getAdapter(TypeToken.get(payload))
        val delegate=gson.getDelegateAdapter(this,type)
        return object: TypeAdapter<T>() {
            override fun write(out: JsonWriter,value: T) { delegate.write(out,value) }
            @Suppress("UNCHECKED_CAST")
            override fun read(input: JsonReader): T {
                try {
                    val element=JsonParser.parseReader(input)
                    if(!element.isJsonObject) throw JsonParseException("Invalid response envelope")
                    val json=element.asJsonObject
                    val code=json["code"]?.asInt ?: throw JsonParseException("Missing code")
                    val message=json["message"]?.takeUnless { it.isJsonNull }?.asString
                    val data=json["data"]?.takeUnless { it.isJsonNull }
                    return V2Response(code,message,if(code==200 && data!=null) dataAdapter.fromJsonTree(data) else null,
                        if(code!=200 && data?.isJsonObject==true) data.asJsonObject else null) as T
                } catch(e: JsonParseException) { throw e }
                catch(e: RuntimeException) { throw JsonParseException("Invalid response envelope",e) }
            }
        }
    }
}
