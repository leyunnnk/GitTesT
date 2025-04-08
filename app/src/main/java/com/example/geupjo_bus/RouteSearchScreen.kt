package com.example.geupjo_bus

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val viewModel: RouteSearchViewModel = viewModel()

    var departure by remember { mutableStateOf(TextFieldValue("")) }
    var destination by remember { mutableStateOf(TextFieldValue("")) }
    var selectedMode by remember { mutableStateOf("transit") }
    var routeResults by remember { mutableStateOf(listOf<String>()) }
    var travelTime by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val departureMarker = remember { mutableStateOf<Marker?>(null) }
    val destinationMarker = remember { mutableStateOf<Marker?>(null) }
    val currentPolyline = remember { mutableStateOf<Polyline?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mapView = rememberMapViewWithLifecycle(context)
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    val recentSearches by viewModel.recentSearches.collectAsState()

    LaunchedEffect(Unit) {
        currentLocation = getCurrentLocation(context)
        currentLocation?.let {
            val address = getAddressFromLocation(context, it.latitude, it.longitude)
            departure = TextFieldValue(address)
        }
        mapView.getMapAsync { gMap ->
            googleMap = gMap
            currentLocation?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                departureMarker.value = gMap.addMarker(MarkerOptions().position(currentLatLng).title("출발지"))
            }
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
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("도착지를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    isSearching = true
                    coroutineScope.launch {
                        val (results, polyline, duration) = fetchDirections(departure.text, destination.text, selectedMode)
                        routeResults = results
                        travelTime = duration
                        viewModel.addRecentSearch(departure.text, destination.text)
                        updateMapWithDirections(
                            googleMap = googleMap,
                            polyline = polyline,
                            polylineColor = 0xFF6200EE.toInt(),
                            currentPolyline = currentPolyline,
                            destinationMarker = destinationMarker,
                            departureMarker = departureMarker,
                            currentLocation = currentLocation
                        )
                        isSearching = false
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            isSearching = true
            coroutineScope.launch {
                val (results, polyline, duration) = fetchDirections(departure.text, destination.text, selectedMode)
                routeResults = results
                travelTime = duration
                viewModel.addRecentSearch(departure.text, destination.text)
                updateMapWithDirections(
                    googleMap = googleMap,
                    polyline = polyline,
                    polylineColor = 0xFF6200EE.toInt(),
                    currentPolyline = currentPolyline,
                    destinationMarker = destinationMarker,
                    departureMarker = departureMarker,
                    currentLocation = currentLocation
                )
                isSearching = false
            }
        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("경로 검색")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(16.dp))

        if (routeResults.isNotEmpty()) {
            Text("검색 결과:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (travelTime.isNotEmpty()) Text("총 이동시간: $travelTime", style = MaterialTheme.typography.bodyMedium)
            routeResults.forEach {
                RouteSearchResultItem(it)
                Spacer(modifier = Modifier.height(8.dp))
            }
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(400.dp)) {
                it.getMapAsync { gMap ->
                    googleMap = gMap
                    gMap.clear()
                }
            }
        } else if (!isSearching) {
            Text("검색 결과가 없습니다.", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recentSearches.isNotEmpty()) {
            Text("최근 경로 검색:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            recentSearches.forEach { (from, to) ->
                Button(onClick = {
                    departure = TextFieldValue(from)
                    destination = TextFieldValue(to)
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
        // 기존 폴리라인 제거
        currentPolyline.value?.remove()

        // 출발지 마커 추가 또는 갱신
        currentLocation?.let {
            val currentLatLng = LatLng(it.latitude, it.longitude)
            departureMarker.value?.remove()
            departureMarker.value = map.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("출발지")
            )
        }

        // 새 폴리라인 추가
        if (!polyline.isNullOrEmpty()) {
            val points = decodePolyline(polyline)
            currentPolyline.value = map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(polylineColor)
                    .width(10f)
            )

            // 마지막 지점을 목적지로 설정
            val lastPoint = points.lastOrNull()
            lastPoint?.let {
                // 기존 마커 제거
                destinationMarker.value?.remove()

                // 새 마커 추가
                destinationMarker.value = map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("도착지")
                )

                // 카메라 이동
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 16f))
            }
        }
    }
}

suspend fun fetchDirections(departure: String, destination: String, mode: String): Triple<List<String>, String?, String> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$departure&destination=$destination&mode=$mode&language=ko&key=AIzaSyA-XxR0OPZoPTA9-TxDyqQVqaRt9EOa-Eg"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string()

            val routeList = mutableListOf<String>()
            var polyline: String? = null
            var totalDuration = ""

            if (jsonData != null) {
                val jsonObject = JSONObject(jsonData)
                val routes = jsonObject.getJSONArray("routes")

                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    polyline = route.getJSONObject("overview_polyline").getString("points")

                    val legs = route.getJSONArray("legs")
                    totalDuration = legs.getJSONObject(0).getJSONObject("duration").getString("text")

                    val steps = legs.getJSONObject(0).getJSONArray("steps")

                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val instruction = step.getString("html_instructions")
                        val distance = step.getJSONObject("distance").getString("text")
                        val travelMode = step.getString("travel_mode")

                        val cleanInstruction = instruction.replace(Regex("<.*?>"), "")

                        if (step.has("transit_details")) {
                            val transitDetails = step.getJSONObject("transit_details")
                            val line = transitDetails.getJSONObject("line")
                            val busNumber = line.optString("short_name", "버스")
                            val departureStop = transitDetails.getJSONObject("departure_stop").getString("name")
                            val arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")

                            routeList.add(
                                "$cleanInstruction\n" +
                                        "- 거리: $distance\n" +
                                        "- 버스: $busNumber\n" +
                                        "- 출발 정류장: $departureStop\n" +
                                        "- 도착 정류장: $arrivalStop"
                            )
                        } else {
                            routeList.add("$cleanInstruction\n- 거리: $distance\n- 이동 수단: $travelMode")
                        }
                    }
                }
            }

            Triple(routeList, polyline, totalDuration)
        } catch (e: Exception) {
            Log.e("Google Directions API", "Error fetching directions: ${e.message}")
            Triple(emptyList(), null, "")
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

@Composable
fun RouteSearchResultItem(route: String) {
    Text(
        text = route,
        style = MaterialTheme.typography.bodyMedium
    )
}
