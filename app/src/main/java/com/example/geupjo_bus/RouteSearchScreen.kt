package com.example.geupjo_bus

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geupjo_bus.ui.rememberMapViewWithLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var departure by remember { mutableStateOf(TextFieldValue("")) }
    var destination by remember { mutableStateOf(TextFieldValue("")) }
    val selectedMode by remember { mutableStateOf("transit") }

    var routeResults by remember { mutableStateOf<List<DirectionResult>>(emptyList()) }
    var travelTime by remember { mutableStateOf("") }
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var expandedRouteIndex by remember { mutableStateOf<Int?>(null) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val departureMarker = remember { mutableStateOf<Marker?>(null) }
    val destinationMarker = remember { mutableStateOf<Marker?>(null) }
    val currentPolyline = remember { mutableStateOf<Polyline?>(null) }
    val recentSearches = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            addAll(loadRecentSearches(context))
        }
    }

    val polylineColor = 0xFF6200EE.toInt()
    val mapView = rememberMapViewWithLifecycle(context)
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("shared_destination", Context.MODE_PRIVATE)
        val savedDestination = sharedPrefs.getString("destination", null)
        if (!savedDestination.isNullOrBlank()) {
            destination = TextFieldValue(savedDestination)
            // 저장된 주소는 한 번 불러오면 제거
            sharedPrefs.edit().remove("destination").apply()
        }



        currentLocation = getCurrentLocation(context)
        currentLocation?.let {
            val address = getAddressFromLocation(context, it.latitude, it.longitude)
            departure = TextFieldValue(address)
        }
        mapView.getMapAsync { gMap ->
            googleMap = gMap
            googleMap?.let { map ->
                currentLocation?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    departureMarker.value = map.addMarker(
                        MarkerOptions().position(currentLatLng).title("출발지")
                    )
                }
            }
        }
    }

    fun performSearch() {
        if (departure.text.isBlank() || destination.text.isBlank()) {
            Toast.makeText(context, "출발지와 도착지를 모두 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        isSearching = true
        keyboardController?.hide()
        coroutineScope.launch {
            val results = fetchDirections(departure.text, destination.text, selectedMode)
            routeResults = results
            travelTime = results.firstOrNull()?.duration ?: ""
            expandedRouteIndex = null
            recentSearches.removeAll { it.first == departure.text && it.second == destination.text }
            recentSearches.add(0, departure.text to destination.text)
            if (recentSearches.size > 5) recentSearches.removeLast()
            saveRecentSearches(context, recentSearches)
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Button(onClick = onBackClick, modifier = Modifier.align(Alignment.Start)) {
            Text("뒤로 가기")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = departure,
            onValueChange = { departure = it },
            label = { Text("출발지를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (departure.text.isNotEmpty()) {
                    IconButton(onClick = { departure = TextFieldValue("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("도착지를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (destination.text.isNotEmpty()) {
                    IconButton(onClick = { destination = TextFieldValue("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { performSearch() })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = {
                val temp = departure
                departure = destination
                destination = temp
            }) {
                Icon(Icons.Default.SwapVert, contentDescription = "출발/도착 스왑")
                Spacer(modifier = Modifier.width(8.dp))
                Text("스왑")
            }

            Button(onClick = { performSearch() }) {
                Text("경로 검색")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(factory = { mapView }, modifier = Modifier
            .fillMaxWidth()
            .height(250.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            routeResults.forEachIndexed { index, directionResult ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (expandedRouteIndex == index) {
                                expandedRouteIndex = null
                            } else {
                                expandedRouteIndex = index
                                updateMapWithDirections(
                                    googleMap = googleMap,
                                    polyline = directionResult.polyline,
                                    polylineColor = polylineColor,
                                    currentPolyline = currentPolyline,
                                    destinationMarker = destinationMarker,
                                    departureMarker = departureMarker,
                                    currentLocation = currentLocation
                                )
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "경로 ${index + 1} - ${directionResult.duration}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (expandedRouteIndex == index) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                directionResult.steps.forEach { step ->
                                    RouteSearchResultItem(
                                        step.replace("(시외)", "🚍(시외)")
                                            .replace("(고속)", "🚄(고속)")
                                            .replace("휴게소", "🛑 휴게소")
                                            .replace("기차", "🚆(기차)")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



        if (recentSearches.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("최근 검색:", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    recentSearches.clear()
                    saveRecentSearches(context, emptyList())
                }) {
                    Text("모두 지우기")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            recentSearches.take(5).forEach { (from, to) ->
                Button(
                    onClick = {
                        departure = TextFieldValue(from)
                        destination = TextFieldValue(to)
                        performSearch()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text("$from → $to")
                }
            }
        }

    }
}


fun updateMapWithDirections(
    googleMap: GoogleMap?,
    polyline: String?,
    polylineColor: Int,
    currentPolyline: MutableState<Polyline?>,
    destinationMarker: MutableState<Marker?>,
    departureMarker: MutableState<Marker?>,
    currentLocation: Location?
) {
    googleMap?.let { map ->
        currentPolyline.value?.remove()
        currentLocation?.let {
            val currentLatLng = LatLng(it.latitude, it.longitude)
            departureMarker.value?.remove()
            departureMarker.value = map.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("출발지")
            )
        }
        if (!polyline.isNullOrEmpty()) {
            val points = decodePolyline(polyline)
            currentPolyline.value = map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(polylineColor)
                    .width(10f)
            )
            val lastPoint = points.lastOrNull()
            lastPoint?.let {
                destinationMarker.value?.remove()
                destinationMarker.value = map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("도착지")
                )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }
        }
    }
}

fun saveRecentSearches(context: Context, searches: List<Pair<String, String>>) {
    val sharedPreferences = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val encoded = searches.joinToString("|") { "${it.first}::${it.second}" }
    editor.putString("recent_searches", encoded)
    editor.apply()
}

fun loadRecentSearches(context: Context): List<Pair<String, String>> {
    val sharedPreferences = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)
    val encoded = sharedPreferences.getString("recent_searches", null) ?: return emptyList()
    return encoded.split("|").mapNotNull {
        val parts = it.split("::")
        if (parts.size == 2) parts[0] to parts[1] else null
    }
}
data class DirectionResult(val steps: List<String>, val duration: String, val polyline: String)
@Composable
fun RouteSearchResultItem(route: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = route,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MultiRouteResults(routeResults: List<DirectionResult>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        routeResults.forEachIndexed { index, directionResult ->
            Text(
                text = "경로 ${index + 1} (예상 시간: ${directionResult.duration})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            directionResult.steps.forEach { step ->
                RouteSearchResultItem(step)
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

suspend fun fetchDirections(departure: String, destination: String, mode: String): List<DirectionResult> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val encodedOrigin = URLEncoder.encode(departure, "UTF-8")
            val encodedDestination = URLEncoder.encode(destination, "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$encodedOrigin&destination=$encodedDestination&mode=$mode&alternatives=true&language=ko&key=AIzaSyA-XxR0OPZoPTA9-TxDyqQVqaRt9EOa-Eg"

            Log.d("Directions API", "URL: $url")

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string()

            Log.d("Directions API", "Response: $jsonData")

            val resultList = mutableListOf<DirectionResult>()

            if (jsonData != null) {
                val jsonObject = JSONObject(jsonData)
                val routes = jsonObject.getJSONArray("routes")

                for (r in 0 until routes.length()) {
                    val route = routes.getJSONObject(r)
                    val polyline = route.getJSONObject("overview_polyline").getString("points")
                    val legs = route.getJSONArray("legs")
                    val duration = legs.getJSONObject(0).getJSONObject("duration").getString("text")
                    val steps = legs.getJSONObject(0).getJSONArray("steps")

                    val numTransfers = (0 until steps.length()).count { steps.getJSONObject(it).has("transit_details") } - 1
                    val transferInfo = if (numTransfers > 0) "🔄 환승 ${numTransfers}회" else "직행"

                    val stepList = mutableListOf<String>()
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val instruction = step.getString("html_instructions").replace(Regex("<.*?>"), "")
                        val distance = step.getJSONObject("distance").getString("text")
                        val durationStep = step.optJSONObject("duration")?.optString("text") ?: ""
                        val travelMode = if (step.has("transit_details")) "버스" else step.getString("travel_mode")
                        val busNumber = if (step.has("transit_details")) {
                            step.getJSONObject("transit_details").getJSONObject("line").optString("short_name", "")
                        } else null
                        val busInfo = if (!busNumber.isNullOrEmpty()) "├🚌 수단: $travelMode ($busNumber)\n" else ""

                        stepList.add(
                            """
                            📍 $instruction
                            ├🚶 거리: $distance
                            $busInfo├⏱️ 소요 시간: $durationStep
                            └$transferInfo
                            """.trimIndent()
                        )
                    }

                    resultList.add(DirectionResult(stepList, duration, polyline))
                }
            }

            resultList
        } catch (e: Exception) {
            Log.e("Google Directions API", "Error fetching directions: ${e.message}")
            emptyList()
        }
    }
}


fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(
            lat.toDouble() / 1E5,
            lng.toDouble() / 1E5
        )
        poly.add(p)
    }
    return poly
}

@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    return suspendCoroutine { continuation ->
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location -> continuation.resume(location) }
            .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    }
}

fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String {
    val geocoder = Geocoder(context)
    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
    return addresses?.firstOrNull()?.getAddressLine(0) ?: "주소를 찾을 수 없습니다."
}
