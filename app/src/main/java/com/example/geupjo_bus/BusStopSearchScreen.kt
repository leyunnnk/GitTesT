package com.example.geupjo_bus

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusStopItem
import com.example.geupjo_bus.api.BusArrivalItem
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.net.URLDecoder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geupjo_bus.ui.theme.rememberMapViewWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusStopSearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    apiKey: String,
    onBusStopClick: (String) -> Unit
) {
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
                                cityCode = 38030,
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
                            fetchBusArrivalInfo(busStop, apiKey, this) { arrivals ->
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
                            fetchBusArrivalInfo(busStop, apiKey, this) { arrivals ->
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
                                    val isAlarmSet = alarmBusArrivals.any { it.routeNo == arrival.routeNo }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

fun toggleAlarm(busArrival: BusArrivalItem, alarmBusArrivals: MutableList<BusArrivalItem>, context: Context) {
    // 알람이 이미 등록된 경우 제거, 그렇지 않으면 추가
    if (alarmBusArrivals.any { it.routeNo == busArrival.routeNo }) {
        alarmBusArrivals.removeAll { it.routeNo == busArrival.routeNo }
    } else {
        alarmBusArrivals.add(busArrival)
    }
    saveAlarms(context, alarmBusArrivals)
}

fun saveAlarms(context: Context, alarms: List<BusArrivalItem>) {
    val sharedPreferences = context.getSharedPreferences("BusAppPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(alarms)
    editor.putString("alarmBusArrivals", json)
    editor.apply()
}

fun loadAlarms(context: Context): List<BusArrivalItem> {
    val sharedPreferences = context.getSharedPreferences("BusAppPrefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("alarmBusArrivals", null) ?: return emptyList()
    val type = object : TypeToken<List<BusArrivalItem>>() {}.type
    return Gson().fromJson(json, type)
}


suspend fun fetchBusArrivalInfo(busStop: BusStopItem, apiKey: String, coroutineScope: CoroutineScope, onResult: (List<BusArrivalItem>) -> Unit) {
    try {
        val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
        val response = BusApiClient.apiService.getBusArrivalInfo(
            apiKey = decodedKey,
            cityCode = 38030,
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
