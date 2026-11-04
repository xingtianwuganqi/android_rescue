package com.rescue.flutter_720yun.findpet

import com.rescue.flutter_720yun.home.models.AddressItem

/** Only an exact province/city pair in both bundled catalogues may supply a city ID. */
object FindPetCities {
    fun resolve(address: String, provinces: List<AddressItem>, cities: List<AddressItem>): String? {
        val parts=address.split('.').map { it.trim() }
        if(parts.size<2) return null
        fun name(value: String)=value.removeSuffix("省").removeSuffix("市")
        val candidates=provinces.filter { name(it.name)==name(parts[0]) }.flatMap { province ->
            province.children.orEmpty().filter { name(it.name)==name(parts[1]) }.mapNotNull { city ->
                cities.singleOrNull { it.code==city.code && name(it.name)==name(city.name) && it.code.matches(Regex("[0-9]{6}")) && it.code.take(2)==province.code.take(2) }?.code
            }
        }.distinct()
        return candidates.singleOrNull()
    }
}
