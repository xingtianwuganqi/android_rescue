package com.rescue.flutter_720yun.adoption.viewmodels
import androidx.lifecycle.MutableLiveData
import com.rescue.flutter_720yun.adoption.models.*
class AdoptionProfileViewModel : AdoptionViewModel() {
    val data = MutableLiveData<ProfileData?>()
    val saved = MutableLiveData(false)
    var age = ""; var city = ""; var district = ""; var housing = ""; var employment = ""; var experience = ""
    var loaded = false
    fun load(preserveDraft: Boolean = false) = run {
        loaded = false
        val result = repository.profile(); loaded = true
        if (!preserveDraft) {
            val profile = result.profile
            age = profile?.age?.toString().orEmpty(); city = profile?.residence_city.orEmpty()
            district = profile?.residence_district.orEmpty(); housing = profile?.housing_type.orEmpty()
            employment = profile?.employment_status.orEmpty(); experience = profile?.pet_experience.orEmpty()
        }
        data.value = result
    }
    fun save() = run {
        val enteredAge=ProfileInputPolicy.age(age) ?: throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(400,message=ProfileInputPolicy.AGE_ERROR,fields=mapOf("age" to ProfileInputPolicy.AGE_ERROR))
        if (!loaded) throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(400, message = "请先成功加载领养资料后再保存")
        val result = repository.saveProfile(ProfileWrite(enteredAge, city, district, housing, employment, experience.trim(), data.value?.version))
        data.value = result; saved.value = true
    }
    override fun clearPrivateState() {
        data.value = null; age = ""; city = ""; district = ""; housing = ""; employment = ""; experience = ""; loaded = false; saved.value = false
    }
}
