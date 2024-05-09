package com.hemu1089.rf_reader

object Helper {

    fun isNotEmpty(cs: CharSequence): Boolean {
        return !isEmpty(cs)
    }

    fun isEmpty(cs: CharSequence?): Boolean {
        return cs.isNullOrEmpty()
    }
}