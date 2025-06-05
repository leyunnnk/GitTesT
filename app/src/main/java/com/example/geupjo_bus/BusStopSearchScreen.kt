package com.example.geupjo_bus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusArrivalItem
import com.example.geupjo_bus.api.BusRouteList
import com.example.geupjo_bus.api.BusStopItem
import com.example.geupjo_bus.ui.rememberMapViewWithLifecycle
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusStopSearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    apiKey: String,
    onBusStopClick: (String) -> Unit
) {
    val supportedCities = mapOf(
        "진주시" to 38030,
        "창원시" to 38010,
        "통영시" to 38050,
        "사천시" to 38060,
        "김해시" to 38070,
        "밀양시" to 38080,
        "거제시" to 38090,
        "양산시" to 38100,
        "의령군" to 38310,
        "함안군" to 38320,
        "창녕군" to 38330,
        "고성군" to 38340,
        "남해군" to 38350,
        "하동군" to 38360,
        "산청군" to 38370,
        "함양군" to 38380,
        "거창군" to 38390,
        "합천군" to 38400

    )
    var searchQuery by remember { mutableStateOf(TextFieldValue("") )}
    var busStops by remember { mutableStateOf<List<BusStopItem>>(emptyList()) }
    val favoriteBusStops = remember { mutableStateListOf<BusStopItem>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedBusStop by remember { mutableStateOf<BusStopItem?>(null) }
    var busArrivalInfo by remember { mutableStateOf<List<BusArrivalItem>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var searchedCenter by remember { mutableStateOf<LatLng?>(null) }
    var busStopList by remember { mutableStateOf<List<BusRouteList>>(emptyList()) }

    var selectedCity by remember { mutableStateOf("진주시") }
    var cityCode by remember { mutableIntStateOf(supportedCities[selectedCity] ?: 38030) }
    var expanded by remember { mutableStateOf(false) }
    // 위치 서비스 클라이언트
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }



    // Load favorites and get location on start
    LaunchedEffect(Unit) {
        favoriteBusStops.addAll(loadFavorites(context))
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Button(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.Start),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {
            Text("뒤로 가기", color = MaterialTheme.colorScheme.onPrimary)
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCity,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("도시 선택") }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                supportedCities.keys.forEach { city ->
                    DropdownMenuItem(
                        text = { Text(text = city) },
                        onClick = {
                            selectedCity = city
                            cityCode = supportedCities[city] ?: 38030
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "정류장 검색",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue -> searchQuery = newValue },
            label = { Text("정류장 이름 입력") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    coroutineScope.launch {
                        try {
                            val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
                            val response = BusApiClient.apiService.searchBusStops(
                                apiKey = decodedKey,
                                cityCode = cityCode,
                                nodeNm = searchQuery.text
                            )

                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                busStops = responseBody?.body?.items?.itemList?.take(40) ?: emptyList()
                            } else {
                                Log.e("API Error", "API 호출 실패 - 코드: ${response.code()}, 메시지: ${response.message()}")
                            }
                        } catch (e: Exception) {
                            Log.e("API Exception", "API 호출 오류: ${e.message}")
                        }
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (favoriteBusStops.isNotEmpty()) {
            Text(
                text = "즐겨찾기 정류장",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            favoriteBusStops.forEach { busStop ->
                BusStopSearchResultItem(
                    busStopName = busStop.nodeName ?: "알 수 없음",
                    onClick = {
                        selectedBusStop = busStop
                        coroutineScope.launch {
                            fetchBusArrivalInfo(busStop, apiKey, coroutineScope, cityCode) { arrivals -> // coroutineScope 전달
                                busArrivalInfo = arrivals
                                showDialog = true
                            }
                        }
                    },
                    isFavorite = true,
                    onFavoriteClick = { toggleFavorite(busStop, favoriteBusStops, context) }
                )
            }
        }

        if (busStops.isNotEmpty()) {
            Text(
                text = "검색 결과",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            busStops.forEach { busStop ->
                BusStopSearchResultItem(
                    busStopName = busStop.nodeName ?: "알 수 없음",
                    onClick = {
                        selectedBusStop = busStop
                        coroutineScope.launch {
                            fetchBusArrivalInfo(busStop, apiKey, coroutineScope, cityCode) { arrivals -> // coroutineScope 전달
                                busArrivalInfo = arrivals
                                showDialog = true
                            }
                        }
                    },
                    isFavorite = favoriteBusStops.any { it.nodeId == busStop.nodeId },
                    onFavoriteClick = { toggleFavorite(busStop, favoriteBusStops, context) }
                )
            }
        }
    }

    if (showDialog && selectedBusStop != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "버스 도착 정보: ${selectedBusStop?.nodeName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 지도 표시
                    val mapView = rememberMapViewWithLifecycle(context)
                    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) { map ->
                        map.getMapAsync { gMap ->
                            googleMap = gMap
                            googleMap?.clear()

                            // Latitude, Longitude 값 처리
                            val busStopLat =
                                selectedBusStop?.nodeLati?.toString()?.toDoubleOrNull() ?: latitude
                                ?: 0.0
                            val busStopLong =
                                selectedBusStop?.nodeLong?.toString()?.toDoubleOrNull()
                                    ?: longitude ?: 0.0

                            // LatLng 객체 생성
                            val busStopLocation = LatLng(busStopLat, busStopLong)

                            // 마커 추가
                            googleMap?.addMarker(
                                MarkerOptions()
                                    .position(busStopLocation)
                                    .title(selectedBusStop?.nodeName) // 마커에 타이틀 추가
                            )

                            // 지도 카메라를 마커가 있는 위치로 이동
                            googleMap?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    busStopLocation,
                                    17f
                                )
                            )
                        }
                    }

                    // 도착 버스 정보 표시
                    when {
                        busArrivalInfo.isEmpty() && !isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "도착 버스 정보가 없습니다.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("버스 도착 정보를 불러오는 중입니다...")
                            }
                        }

                        else -> {
                            // 도착 버스 정보 카드 목록
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()) // 스크롤 가능하도록 설정
                            ) {
                                var alarmBusArrivals by remember { mutableStateOf(loadAlarms(context)) }

                                busArrivalInfo.forEach { arrival ->
                                    val arrivalMinutes = (arrival.arrTime ?: 0) / 60
                                    val remainingStations = arrival.arrPrevStationCnt ?: 0

                                    // 알람이 설정된 상태인지 확인
                                    val isAlarmSet = alarmBusArrivals.any { it.nodeId == arrival.nodeId && it.routeNo == arrival.routeNo && it.routeId == arrival.routeId}
                                    var showMapDialog by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        onClick = {
                                            // 구글 맵을 포함한 AlertDialog 표시
                                            coroutineScope.launch {
                                                try {
                                                    val encodedKey = "cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D"
                                                    val apiKey = URLDecoder.decode(encodedKey, "UTF-8")

                                                    val response = arrival.routeId?.let {
                                                        BusApiClient.apiService.getBusRouteInfo(
                                                            apiKey = apiKey,
                                                            cityCode = cityCode,
                                                            routeId = arrival.routeId!!
                                                        )
                                                    }

                                                    if (response != null && response.isSuccessful) {
                                                        val responseBody = response.body()

                                                        // 디버깅 로그 추가
                                                        Log.d("API Debug", "Response: $responseBody")

                                                        if (responseBody?.body?.items?.itemList != null) {
                                                            busStopList = responseBody.body?.items?.itemList?: emptyList()
                                                            searchedCenter = LatLng(selectedBusStop!!.nodeLati!!,
                                                                selectedBusStop!!.nodeLong!!
                                                            )
                                                            showMapDialog = true
                                                        } else {
                                                            Log.e("API Error", "Response body is null or empty")
                                                        }
                                                    } else {
                                                        Log.e("API Error", "API 호출 실패: ${response?.code()}, ${response?.message()}, ${arrival.routeId!!}")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("API Error", "정류장 목록 로드 실패: ${e.message}")
                                                }
                                            }

                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "버스 번호: ${arrival.routeNo}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "예상 도착 시간: ${arrivalMinutes}분 (${remainingStations}개 정류장)",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // 알람 버튼을 누르면 알람 목록에 추가/제거하고 상태를 업데이트
                                                IconButton(onClick = {
                                                    toggleAlarm(arrival, alarmBusArrivals.toMutableList(), context).also {
                                                        // 상태 업데이트 후 UI 즉시 반영
                                                        alarmBusArrivals = loadAlarms(context)
                                                    }
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Alarm,
                                                        contentDescription = "알람 설정",
                                                        tint = if (isAlarmSet) Color.Red else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (showMapDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showMapDialog = false },
                                            title = { Text(text = "버스 경로") },
                                            text = {
                                                searchedCenter?.let {
                                                    GoogleMapRouteView(
                                                        latitude = it.latitude,
                                                        longitude = searchedCenter!!.longitude,
                                                        busRouteList = busStopList,
                                                        onClick = { /* 마커 클릭 이벤트 */ },
                                                        onMapMoved = { /* 지도 이동 이벤트 */ },
                                                        recenterTrigger = false
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(onClick = { showMapDialog = false }) {
                                                    Text("닫기")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("확인", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun BusStopSearchResultItem(
    busStopName: String,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = busStopName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun toggleFavorite(busStop: BusStopItem, favoriteBusStops: MutableList<BusStopItem>, context: Context) {
    if (favoriteBusStops.any { it.nodeId == busStop.nodeId }) {
        favoriteBusStops.removeAll { it.nodeId == busStop.nodeId }
    } else {
        favoriteBusStops.add(busStop)
    }
    saveFavorites(context, favoriteBusStops)
}

fun saveFavorites(context: Context, favorites: List<BusStopItem>) {
    val sharedPreferences = context.getSharedPreferences("BusAppPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(favorites)
    editor.putString("favoriteBusStops", json)
    editor.apply()
}

fun loadFavorites(context: Context): List<BusStopItem> {
    val sharedPreferences = context.getSharedPreferences("BusAppPrefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("favoriteBusStops", null) ?: return emptyList()
    val type = object : TypeToken<List<BusStopItem>>() {}.type
    return Gson().fromJson(json, type)
}






suspend fun fetchBusArrivalInfo(busStop: BusStopItem, apiKey: String, coroutineScope: CoroutineScope, cityCode: Int, onResult: (List<BusArrivalItem>) -> Unit) {
    try {
        val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
        val response = BusApiClient.apiService.getBusArrivalInfo(
            apiKey = decodedKey,
            cityCode = cityCode,
            nodeId = busStop.nodeId!!
        )

        if (response.isSuccessful) {
            onResult(response.body()?.body?.items?.itemList ?: emptyList())
        } else {
            Log.e("API Error", "도착 정보 호출 실패: ${response.code()}, ${response.message()}")
            onResult(emptyList())
        }
    } catch (e: Exception) {
        Log.e("API Exception", "도착 정보 로드 실패: ${e.message}")
        onResult(emptyList())
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBusStopSearchScreen() {
    Geupjo_BusTheme {
        BusStopSearchScreen(
            onBackClick = {},
            apiKey = "DUMMY_API_KEY",
            onBusStopClick = {}
        )
    }
}
