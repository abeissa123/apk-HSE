package com.example.data.repository

import android.util.Log
import com.example.domain.RealtimeWeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherService {

    companion object {
        private const val TAG = "WeatherService"

        data class SiteCoordinates(val latitude: Double, val longitude: Double, val fallbackTemp: Int, val fallbackCondition: String)

        fun getCoordinatesForSite(siteName: String): SiteCoordinates {
            val normalized = siteName.lowercase(Locale.ROOT)
            return when {
                normalized.contains("figuil") -> SiteCoordinates(9.7569, 13.9634, 33, "Ensoleillé • Chaleur")
                normalized.contains("nomayos") -> SiteCoordinates(3.7744, 11.4506, 27, "Partiellement nuageux")
                normalized.contains("bonabéri") || normalized.contains("bonaberi") -> SiteCoordinates(4.0722, 9.6885, 29, "Humide • Averses")
                else -> SiteCoordinates(9.7569, 13.9634, 30, "Ensoleillé")
            }
        }

        fun mapWmoCodeToCondition(code: Int): String {
            return when (code) {
                0 -> "Ensoleillé"
                1 -> "Principalement dégagé"
                2 -> "Partiellement nuageux"
                3 -> "Couvert"
                45, 48 -> "Brumeux"
                51, 53, 55 -> "Bruine légère"
                56, 57 -> "Bruine verglaçante"
                61 -> "Pluie légère"
                63 -> "Pluie modérée"
                65 -> "Forte pluie"
                80, 81, 82 -> "Averses"
                95 -> "Orages"
                96, 99 -> "Orages avec grêle"
                else -> "Temps clair"
            }
        }
    }

    suspend fun fetchLiveWeatherByCoordinates(
        latitude: Double,
        longitude: Double,
        locationLabel: String = "Position Actuelle (GPS)"
    ): RealtimeWeatherData = withContext(Dispatchers.IO) {
        val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"

        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Cimencam-HSE-Android/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val tempDouble = current.getDouble("temperature_2m")
                val apparentTempDouble = current.optDouble("apparent_temperature", tempDouble)
                val humidity = current.optInt("relative_humidity_2m", 65)
                val weatherCode = current.optInt("weather_code", 0)
                val windSpeed = current.optDouble("wind_speed_10m", 8.0)

                val condition = mapWmoCodeToCondition(weatherCode)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val updatedTimeText = timeFormat.format(Date())

                return@withContext RealtimeWeatherData(
                    temperatureCelsius = kotlin.math.round(tempDouble).toInt(),
                    apparentTemperatureCelsius = kotlin.math.round(apparentTempDouble).toInt(),
                    weatherCondition = condition,
                    weatherCode = weatherCode,
                    humidityPercent = humidity,
                    windSpeedKmh = windSpeed,
                    isLive = true,
                    siteName = locationLabel,
                    lastUpdated = "GPS direct $updatedTimeText"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live GPS weather: ${e.message}")
        }

        // Fallback to closest site
        fetchLiveWeather(locationLabel)
    }

    suspend fun fetchLiveWeather(siteName: String): RealtimeWeatherData = withContext(Dispatchers.IO) {
        val coords = getCoordinatesForSite(siteName)
        val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${coords.latitude}&longitude=${coords.longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
        
        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Cimencam-HSE-Android/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val tempDouble = current.getDouble("temperature_2m")
                val apparentTempDouble = current.optDouble("apparent_temperature", tempDouble)
                val humidity = current.optInt("relative_humidity_2m", 65)
                val weatherCode = current.optInt("weather_code", 0)
                val windSpeed = current.optDouble("wind_speed_10m", 8.0)

                val condition = mapWmoCodeToCondition(weatherCode)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val updatedTimeText = timeFormat.format(Date())

                return@withContext RealtimeWeatherData(
                    temperatureCelsius = kotlin.math.round(tempDouble).toInt(),
                    apparentTemperatureCelsius = kotlin.math.round(apparentTempDouble).toInt(),
                    weatherCondition = condition,
                    weatherCode = weatherCode,
                    humidityPercent = humidity,
                    windSpeedKmh = windSpeed,
                    isLive = true,
                    siteName = siteName,
                    lastUpdated = "Direct $updatedTimeText"
                )
            } else {
                Log.w(TAG, "Open-Meteo HTTP response: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live weather: ${e.message}")
        }

        // Fallback with realistic site defaults
        RealtimeWeatherData(
            temperatureCelsius = coords.fallbackTemp,
            apparentTemperatureCelsius = coords.fallbackTemp + 2,
            weatherCondition = coords.fallbackCondition,
            weatherCode = 0,
            humidityPercent = 60,
            windSpeedKmh = 10.0,
            isLive = false,
            siteName = siteName,
            lastUpdated = "Estimation locale"
        )
    }
}
