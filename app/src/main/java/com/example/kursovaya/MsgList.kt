package com.example.kursovaya

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.Kursovaya.R

class MsgList : AppCompatActivity() {
    private lateinit var msgListView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_msg_list)

        msgListView = findViewById<ListView>(R.id.listViewMsg)
        val msgList = ArrayList<String>()
        val regsList = intent.getSerializableExtra("messagesList") as? ArrayList<Message>
        if (regsList != null) {
            regsList.forEach {
                val audioInfo =
                    "Текст: ${it.text}\n" +
                    "Дата: ${it.date}\n" +
                    "Путь к файлу: ${it.audioPath}\n"
                msgList.add(audioInfo)
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, msgList)
            msgListView.adapter = adapter
        }
    }
}