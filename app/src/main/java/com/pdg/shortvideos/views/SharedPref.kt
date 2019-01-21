package com.pdg.shortvideos.views

import android.content.Context
import android.preference.PreferenceManager

class SharedPref {
    companion object {

        fun getAnswer(forQuestion: String, context: Context): String{
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(forQuestion, "")
        }

        fun setAnswer(forQuestion: String, path: String, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString(forQuestion, path)
            editor.apply()
        }
    }
}