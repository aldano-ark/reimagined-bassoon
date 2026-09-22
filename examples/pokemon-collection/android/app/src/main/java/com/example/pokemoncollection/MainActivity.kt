package com.example.pokemoncollection

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pokemoncollection.collection.*
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[CollectionModel::class.java]
        setContent {
            var tab by rememberSaveable { mutableStateOf(Scope.Collection) }
            CollectionTheme { CollectionScreen(model.controller, tab) { tab = it } }
        }
    }
}

/** Owns assembly and in-flight work for the lifetime of this logical Activity, including rotation. */
class CollectionModel(application: Application) : AndroidViewModel(application) {
    val store = CollectionStore(Catalog.read(application.assets), AtomicCollectionStorage(File(application.filesDir, "pokemon-collection-v1.json")))
    val controller = CollectionController(store, viewModelScope)
    init { controller.perform { store.load() } }
}
