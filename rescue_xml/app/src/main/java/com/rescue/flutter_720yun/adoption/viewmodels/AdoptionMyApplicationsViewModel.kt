package com.rescue.flutter_720yun.adoption.viewmodels
import androidx.lifecycle.SavedStateHandle

class AdoptionMyApplicationsViewModel(saved: SavedStateHandle) : AdoptionApplicationsViewModel(saved) {
    init { mine = true }
}
