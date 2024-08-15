package org.evomaster.core.output

object JsonUtils {

    /**
     * Convert a JSON Pointer (RFC6901) into a JSON Path
     */
    fun fromPointerToPath(pointer: String) : String{
        if(pointer.isNullOrBlank()){
            return pointer
        }

        if(!pointer.startsWith("/")){
            throw IllegalArgumentException("Input pointer does not start with /")
        }
        /*
            TODO Ideally would be better to use a library (if any exists), as there
            might be some edges cases we do not know about
         */

        return pointer.substringAfter("/").replace("/",".")
    }
}