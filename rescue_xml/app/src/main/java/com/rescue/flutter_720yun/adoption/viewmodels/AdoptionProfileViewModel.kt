package com.rescue.flutter_720yun.adoption.viewmodels
import androidx.lifecycle.MutableLiveData
import com.rescue.flutter_720yun.adoption.models.*
class AdoptionProfileViewModel : AdoptionViewModel() {
    val data = MutableLiveData<ProfileData?>()
    val saved = MutableLiveData(false)
    var age = ""; var city = ""; var district = ""; var housing = ""; var employment = ""; var experience = ""
    var loaded = false
    fun load() = run {
        val result = repository.profile(); loaded = true
        result.profile?.let { age = it.age?.toString().orEmpty(); city = it.residence_city.orEmpty()
            district = it.residence_district.orEmpty(); housing = it.housing_type.orEmpty()
            employment = it.employment_status.orEmpty(); experience = it.pet_experience.orEmpty() }
        data.value = result
    }
    fun save() = run {
        val result = repository.saveProfile(ProfileWrite(age.toInt(), city, district, housing, employment, experience.trim(), data.value?.version))
        data.value = result; saved.value = true
    }
    override fun clearPrivateState() {
        data.value = null; age = ""; city = ""; district = ""; housing = ""; employment = ""; experience = ""; loaded = false; saved.value = false
    }
}
