package com.example.geupjo_bus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusArrivalItem
import com.example.geupjo_bus.api.BusStopItem
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import com.example.geupjo_bus.ui.theme.rememberMapViewWithLifecycle
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
            //이 부분이 다른사람과 다를거임
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
                                busArrivalInfo.forEach { arrival ->
                                    val arrivalMinutes = (arrival.arrTime ?: 0) / 60
                                    val remainingStations = arrival.arrPrevStationCnt ?: 0

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
