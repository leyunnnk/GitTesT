package com.example.geupjo_bus

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.geupjo_bus.ui.rememberMapViewWithLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
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

@Composable
fun RouteSearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var departure by remember { mutableStateOf(TextFieldValue("")) }
    var destination by remember { mutableStateOf(TextFieldValue("")) }
    var routeResults by remember { mutableStateOf(listOf<String>()) }
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val polylineColor = 0xFF6200EE.toInt()
    // 위치 가져오기
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            currentLocation = getCurrentLocation(context)
            currentLocation?.let {
                val address = getAddressFromLocation(context, it.latitude, it.longitude)
                departure = TextFieldValue(address)
            }
        }
    }

    val mapView = rememberMapViewWithLifecycle(context)
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBackClick, modifier = Modifier.align(Alignment.Start)) {
            Text("뒤로 가기")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 현재 위치 표시
        currentLocation?.let { location ->
            val address = getAddressFromLocation(context, location.latitude, location.longitude)
            Text(text = "현재 위치: $address", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 출발지 입력
        Text(text = "출발지", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = departure,
            onValueChange = { departure = it },
            label = { Text("출발지를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 도착지 입력
        Text(text = "도착지", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

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
                        val (results, polyline) = fetchDirections(departure.text, destination.text)
                        routeResults = results
                        polyline?.let { polylinePoints = decodePolyline(it) }
                        isSearching = false
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isSearching = true
                coroutineScope.launch {
                    val (results, polyline) = fetchDirections(departure.text, destination.text)
                    routeResults = results
                    polyline?.let { polylinePoints = decodePolyline(it) }
                    isSearching = false
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "경로 검색")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (routeResults.isNotEmpty()) {
            Text(text = "검색 결과:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            for (result in routeResults) {
                RouteSearchResultItem(route = result)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 지도 표시
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxHeight(0.8f)
            ) { map ->
                map.getMapAsync { gMap ->
                    googleMap = gMap
                    googleMap?.let { map ->
                        // 현재 위치 표시
                        currentLocation?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                            map.addMarker(MarkerOptions().position(currentLatLng).title("현재 위치"))
                        }

                        // 도착지 마커 표시
                        destinationMarker?.remove()
                        destinationLocation?.let {
                            destinationMarker = map.addMarker(MarkerOptions().position(it).title("도착지"))
                        }

                        // 폴리라인 그리기
                        if (polylinePoints.isNotEmpty()) {
                            map.addPolyline(
                                PolylineOptions()
                                    .addAll(polylinePoints)
                                    .color(polylineColor) // Compose 내부에서 미리 계산한 색상 값 사용
                                    .width(10f)
                            )
                        }
                    }
                }
            }
        } else if (!isSearching) {
            Text(text = "검색 결과가 없습니다.", style = MaterialTheme.typography.bodyMedium)
        }
    }
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

suspend fun fetchDirections(departure: String, destination: String): Pair<List<String>, String?> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$departure&destination=$destination&mode=transit&transit_mode=bus&language=ko&key=AIzaSyA-XxR0OPZoPTA9-TxDyqQVqaRt9EOa-Eg"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string()

            val routeList = mutableListOf<String>()
            var polyline: String? = null

            if (jsonData != null) {
                val jsonObject = JSONObject(jsonData)
                val routes = jsonObject.getJSONArray("routes")

                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    polyline = route.getJSONObject("overview_polyline").getString("points")

                    val legs = route.getJSONArray("legs")
                    val steps = legs.getJSONObject(0).getJSONArray("steps")

                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val instruction = step.getString("html_instructions")
                        val distance = step.getJSONObject("distance").getString("text")

                        if (step.has("transit_details")) {
                            val transitDetails = step.getJSONObject("transit_details")
                            val line = transitDetails.getJSONObject("line")
                            val busNumber = line.getString("short_name")
                            val departureStop = transitDetails.getJSONObject("departure_stop").getString("name")
                            val arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")

                            routeList.add("$instruction - $distance\n버스: $busNumber, 출발 정류장: $departureStop, 도착 정류장: $arrivalStop")
                        } else {
                            routeList.add("$instruction - $distance")
                        }
                    }
                }
            }
            Pair(routeList, polyline)
        } catch (e: Exception) {
            Log.e("Google Directions API", "Error fetching directions: ${e.message}")
            Pair(emptyList(), null)
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
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(
            lat.toDouble() / 1E5,
            lng.toDouble() / 1E5
        )
        poly.add(p)
    }
    return poly
}

@Composable
fun RouteSearchResultItem(route: String) {
    Text(
        text = route,
        style = MaterialTheme.typography.bodyMedium
    )
}
