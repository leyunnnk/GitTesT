package com.example.geupjo_bus

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusArrivalItem
import com.example.geupjo_bus.api.BusRouteList
import com.example.geupjo_bus.api.BusStop
import com.example.geupjo_bus.ui.rememberMapViewWithLifecycle
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt



@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Geupjo_BusTheme {
                var drawerState by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf("home") }
                val context = LocalContext.current

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            GradientTopBar(
                                title = "GN BUS",
                                onMenuClick = { drawerState = true }
                            )
                        },
                        bottomBar = {
                            BottomNavigationBar(currentScreen) { selected ->
                                currentScreen = selected
                            }
                        },
                        content = { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentScreen) {
                                    "home" -> BusAppContent(
                                        modifier = Modifier.fillMaxSize(),
                                        onSearchClick = { currentScreen = "search" },
                                        onRouteSearchClick = { currentScreen = "route" }
                                    )
                                    "search" -> BusStopSearchScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onBackClick = { currentScreen = "home" },
                                        apiKey = "cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D",
                                        onBusStopClick = { Log.d("MainActivity", "Selected: $it") }
                                    )
                                    "route" -> RouteSearchScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onBackClick = { currentScreen = "home" }
                                    )
                                    "manbok" -> ManbokScreen(
                                        onBackClick = { currentScreen = "home" }
                                    )
                                    "alarm" -> AlarmScreen(
                                        onBackClick = { currentScreen = "home" },
                                        context = context
                                    )
                                }
                            }
                        }
                    )

                    // 드로어 메뉴 (오른쪽 슬라이드)
                    AnimatedVisibility(
                        visible = drawerState,
                        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                    ) {
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopBar(title: String, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        TopAppBar(
            title = {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}


@Composable
fun BottomNavigationBar(currentScreen: String, onTabSelected: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current

    NavigationBar {
        val items = listOf("home", "search", "route", "manbok", "alarm")
        val icons = listOf(
            Icons.Default.Home,
            Icons.Default.Search,
            Icons.Default.DirectionsBus,
            Icons.Default.Favorite,
            Icons.Default.Alarm
        )
        val labels = listOf("홈", "정류장", "경로", "만보기", "알람")

        items.forEachIndexed { index, screen ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(screen)
                },
                icon = {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = labels[index]
                    )
                },
                label = {
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}



@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BusAppContent(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onRouteSearchClick: () -> Unit
    
) {
    val context = LocalContext.current

    var busStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }
    var busStopList by remember { mutableStateOf<List<BusRouteList>>(emptyList()) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var searchedCenter by remember { mutableStateOf<LatLng?>(null) }
    var mapCenter by remember { mutableStateOf<LatLng?>(null) }
    var shouldRecenter by remember { mutableStateOf(false) }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    var selectedBusStop by remember { mutableStateOf<BusStop?>(null) }
    var busArrivalInfo by remember { mutableStateOf<List<BusArrivalItem>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val gyeongsangnamdoCityCodes = listOf(
        38010, 38020, 38030, 38040, 38050,
        38060, 38070, 38080, 38090, 38100,
        38110, 38120, 38130, 38140, 38150,
        38160, 38170, 38180, 38190
    )

    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            getCurrentLocation(context, fusedLocationClient) { lat, lng ->
                latitude = lat
                longitude = lng
                val center = LatLng(lat, lng)
                searchedCenter = center
                mapCenter = center
                coroutineScope.launch {
                    try {
                        val apiKey = URLDecoder.decode("cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D", "UTF-8")
                        val response = BusApiClient.apiService.getNearbyBusStops(apiKey, lat, lng)
                        if (response.isSuccessful) {
                            busStops = response.body()?.body?.items?.itemList?.take(10) ?: emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("API Error", "위치 기반 정류장 조회 실패: ${e.message}")
                    }
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(shouldRecenter) {
        if (shouldRecenter) {
            delay(500L)
            shouldRecenter = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (latitude != null && longitude != null) {
            GoogleMapView(
                latitude!!,
                longitude!!,
                busStops,
                onClick = { busStop ->
                    selectedBusStop = busStop
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val apiKey = URLDecoder.decode("cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D", "UTF-8")
                            var result: List<BusArrivalItem> = emptyList()
                            for (cityCode in gyeongsangnamdoCityCodes) {
                                val res = BusApiClient.apiService.getBusArrivalInfo(apiKey, cityCode, busStop.nodeId!!)
                                if (res.isSuccessful) {
                                    val items = res.body()?.body?.items?.itemList.orEmpty()
                                    if (items.isNotEmpty()) {
                                        result = items.sortedBy { it.arrTime ?: Int.MAX_VALUE }
                                        break
                                    }
                                }
                            }
                            busArrivalInfo = result
                        } catch (e: Exception) {
                            Log.e("API Error", "도착 정보 로드 실패: ${e.message}")
                        } finally {
                            isLoading = false
                            showDialog = true
                        }
                    }
                },
                onMapMoved = { mapCenter = it },
                recenterTrigger = shouldRecenter,
                onRouteSearchClick = onRouteSearchClick
            )

            if (mapCenter != null && searchedCenter != null) {
                val distance = getDistance(
                    searchedCenter!!.latitude,
                    searchedCenter!!.longitude,
                    mapCenter!!.latitude,
                    mapCenter!!.longitude
                )
                if (distance > 200) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val apiKey = URLDecoder.decode("cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D", "UTF-8")
                                    // 🔥 마커 초기화 (기존 유지)
                                    busStops = emptyList()

                                    val response = BusApiClient.apiService.getNearbyBusStops(
                                        apiKey,
                                        mapCenter!!.latitude,
                                        mapCenter!!.longitude
                                    )
                                    if (response.isSuccessful) {
                                        busStops = response.body()?.body?.items?.itemList?.take(10) ?: emptyList()
                                        searchedCenter = mapCenter
                                    }
                                } catch (e: Exception) {
                                    Log.e("API Error", "재검색 실패: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 150.dp, end = 16.dp), // FAB 간격 고려
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "재검색")
                    }
                }

            }
            FloatingActionButton(
                onClick = {
                    shouldRecenter = true
                    searchedCenter = LatLng(latitude!!, longitude!!)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 16.dp), // 위치는 BottomNavigationBar 고려해서 조절
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "현재위치로 이동")
            }


            if (busStops.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("주변에 정류장이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("현재 위치를 확인 중입니다...", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // 도착 정보 AlertDialog
        if (showDialog && selectedBusStop != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text("버스 도착 정보: ${selectedBusStop?.nodeName}", style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        val mapView = rememberMapViewWithLifecycle(context)
                        var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) { view ->
                            view.getMapAsync { gMap ->
                                googleMap = gMap
                                val lat = selectedBusStop?.latitude?: latitude!!
                                val lng = selectedBusStop?.longitude?: longitude!!
                                val loc = LatLng(lat, lng)
                                gMap.clear()
                                gMap.addMarker(MarkerOptions().position(loc).title(selectedBusStop?.nodeName))
                                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f))
                            }
                        }

                        when {
                            isLoading -> {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(8.dp))
                                    Text("버스 도착 정보를 불러오는 중입니다...")
                                }
                            }
                            busArrivalInfo.isEmpty() -> {
                                Text("도착 버스 정보가 없습니다.")
                            }
                            else -> {
                                var alarmBusArrivals by remember { mutableStateOf(loadAlarms(context)) }

                                busArrivalInfo.forEach { arrival ->
                                    val arrivalMinutes = (arrival.arrTime ?: 0) / 60
                                    val remainingStations = arrival.arrPrevStationCnt ?: 0
                                    val isAlarmSet = alarmBusArrivals.any {
                                        it.nodeId == arrival.nodeId && it.routeId == arrival.routeId
                                    }
                                    var showMapDialog by remember { mutableStateOf(false) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    val apiKey = URLDecoder.decode("cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D", "UTF-8")
                                                    var found = false
                                                    for (cityCode in gyeongsangnamdoCityCodes) {
                                                        val response = arrival.routeId?.let {
                                                            BusApiClient.apiService.getBusRouteInfo(apiKey, cityCode, it)
                                                        }
                                                        if (response != null && response.isSuccessful) {
                                                            val items = response.body()?.body?.items?.itemList
                                                            if (!items.isNullOrEmpty()) {
                                                                busStopList = items
                                                                searchedCenter = mapCenter
                                                                showMapDialog = true
                                                                found = true
                                                                break
                                                            }
                                                        }
                                                    }
                                                    if (!found) Log.e("API", "경로 정보 없음")
                                                } catch (e: Exception) {
                                                    Log.e("API", "경로 요청 실패: ${e.message}")
                                                }
                                            }
                                        }
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text("버스 번호: ${arrival.routeNo}")
                                                    Text("예상 도착: ${arrivalMinutes}분 (${remainingStations}개 정류장)")
                                                }
                                                IconButton(onClick = {
                                                    toggleAlarm(arrival, alarmBusArrivals.toMutableList(), context)
                                                    alarmBusArrivals = loadAlarms(context)
                                                }) {
                                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = if (isAlarmSet) Color.Red else Color.Gray)
                                                }
                                            }
                                        }
                                    }

                                    if (showMapDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showMapDialog = false },
                                            title = { Text("버스 경로") },
                                            text = {
                                                GoogleMapRouteView(
                                                    latitude = searchedCenter?.latitude ?: latitude!!,
                                                    longitude = searchedCenter?.longitude ?: longitude!!,
                                                    busRouteList = busStopList,
                                                    onClick = {},
                                                    onMapMoved = {},
                                                    recenterTrigger = false
                                                )
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
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("확인")
                    }
                }
            )
        }
    }
}

@Composable
fun GoogleMapRouteView(
    latitude: Double,
    longitude: Double,
    busRouteList: List<BusRouteList>,
    onClick: (BusStop) -> Unit,
    onMapMoved: (LatLng) -> Unit,
    recenterTrigger: Boolean // recenter 여부 전달
) {
    val cameraPositionState = remember {
        CameraPositionState(
            position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 13f)
        )
    }

    var zoomLevel by remember { mutableStateOf(17f) } // 줌 레벨 추적 상태

    // 지도 이동 시 onMapMoved 호출
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.position.zoom }
            .distinctUntilChanged()
            .collect { zoom ->
                zoomLevel = zoom
            }
    }

    // recenterTrigger가 true이면 현재 위치(latitude, longitude)로 카메라 이동
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 17f))
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        onMapLoaded = {
            Log.d("GoogleMap", "지도 로드 완료")
        }
    ) {
        // 현재 위치 마커 표시 (줌 레벨에 따라 표시 여부 결정)
        if (zoomLevel >= 15f) { // 줌 레벨이 15 이상일 때만 표시
            Marker(
                state = rememberMarkerState(position = LatLng(latitude, longitude)),
                title = "현재 위치",
                snippet = "여기가 현재 위치입니다."
            )
        }

        // 버스 정류장 마커 표시 (줌 레벨에 따라 표시 여부 결정)
        busRouteList.forEach { busRoute ->
            val lat = busRoute.latitude
            val lng = busRoute.longitude
            if (lat != null && lng != null && zoomLevel >= 15f) { // 줌 레벨이 15 이상일 때만 표시
                Marker(
                    state = rememberMarkerState(position = LatLng(lat, lng)),
                    title = busRoute.nodeName,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {

                        true
                    }
                )
            }
        }

        // Polyline 예제: 버스 정류장들을 연결하는 선 그리기
        val points = busRouteList.mapNotNull { busStop ->
            if (busStop.latitude != null && busStop.longitude != null) {
                LatLng(busStop.latitude!!, busStop.longitude!!)
            } else null
        }

        if (points.size >= 2) {
            Polyline(
                points = points,
                color = Color.Red,
                width = 10f
            )
        }
    }
}

@Composable
fun GoogleMapView(
    latitude: Double,
    longitude: Double,
    busStops: List<BusStop>,
    onClick: (BusStop) -> Unit,
    onMapMoved: (LatLng) -> Unit,
    recenterTrigger: Boolean,
    onRouteSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val cameraPositionState = remember {
        CameraPositionState(CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 17f))
    }
    var clickedMarkerPosition by remember { mutableStateOf<LatLng?>(null) }
    var clickedAddress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.position }
            .distinctUntilChanged()
            .collect { onMapMoved(it.target) }
    }

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 17f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                clickedMarkerPosition = latLng
                val geocoder = Geocoder(context)
                val address = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                clickedAddress = address?.firstOrNull()?.getAddressLine(0) ?: "주소를 찾을 수 없습니다."
            }
        ) {
            Marker(
                state = rememberMarkerState(position = LatLng(latitude, longitude)),
                title = "현재 위치",
                snippet = "여기가 현재 위치입니다."
            )

            busStops.forEach { stop ->
                stop.latitude?.let { lat ->
                    stop.longitude?.let { lng ->
                        Marker(
                            state = rememberMarkerState(position = LatLng(lat, lng)),
                            title = stop.nodeName,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = {
                                onClick(stop)
                                true
                            }
                        )
                    }
                }
            }

            clickedMarkerPosition?.let { pos ->
                key(pos) {
                    Marker(
                        state = rememberMarkerState(position = pos),
                        title = "선택한 위치",
                        snippet = clickedAddress,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                }
            }
        }

        // ✅ 오렌지 마커 클릭 시 하단에 주소 + 버튼 표시
        clickedAddress?.let { addr ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Text("선택한 위치 주소: $addr", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val sharedPreferences = context.getSharedPreferences("shared_destination", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("destination", addr).apply()
                    onRouteSearchClick()  // 화면 전환용 콜백
                }) {
                    Text("이 위치로 경로 검색")
                }
            }
        }
    }
}

@Composable
fun NearbyBusStop(
    busStopName: String,
    distance: String,
    currentlat: Double,
    currentlong: Double,
    busStoplati: Double,
    busStoplong: Double,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = busStopName, style = MaterialTheme.typography.titleMedium)
        Text(text = distance, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            text = "${getDistance(currentlat, currentlong, busStoplati, busStoplong).toInt()} m",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun BusArrivalInfo(busNumber: String, arrivalTime: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(text = busNumber, style = MaterialTheme.typography.titleMedium)
        Text(text = arrivalTime, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun getDistance(
    currentlat: Double,
    currentlong: Double,
    busStoplati: Double,
    busStoplong: Double
): Double {
    val R = 6372.8 * 1000
    val dLat = Math.toRadians(busStoplati - currentlat)
    val dLon = Math.toRadians(busStoplong - currentlong)
    val a = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(Math.toRadians(currentlat)) * cos(Math.toRadians(busStoplati))
    val c = 2 * asin(sqrt(a))
    return round(R * c)
}

fun getCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationRetrieved: (Double, Double) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("Location", "위도: $latitude, 경도: $longitude")
                    onLocationRetrieved(latitude, longitude)
                } else {
                    Log.d("Location", "위치를 가져올 수 없습니다.")
                    Toast.makeText(context, "위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManbokScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var stepCount by remember { mutableStateOf(loadStepCount(context)) }
    var showDialog by remember { mutableStateOf(false) }
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    val stepCountListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null && event.sensor.type == Sensor.TYPE_STEP_DETECTOR && event.values.isNotEmpty()) {
                stepCount++
                saveStepCount(context, stepCount)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    LaunchedEffect(Unit) {
        if (stepSensor != null) {
            sensorManager.registerListener(stepCountListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            Log.d("ManbokScreen", "Step sensor not available.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1
            )
        } else {
            val intent = Intent(context, StepCountService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("만보기 화면") },
                actions = {
                    Button(onClick = onBackClick) {
                        Text("뒤로 가기")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "걸음 수: $stepCount",
                    style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "이동 거리: ${round(stepCount * 0.6)} m",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                )
                Text(
                    text = "소모 칼로리: ${round(stepCount * 0.03)} kcal",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showDialog = true }) {
                    Text("초기화")
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    saveStepCount(context, 0)
                    stepCount = 0
                    showDialog = false
                }) {
                    Text("초기화")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
            title = { Text("정말 초기화 하시겠습니까?") },
            text = { Text("기록된 걸음 수가 모두 삭제됩니다.") }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(stepCountListener)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(onBackClick: () -> Unit, context: Context) {
    var alarmBusArrivals by remember { mutableStateOf(loadAlarms(context)) }
    var busArrivalInfo by remember { mutableStateOf<List<BusArrivalItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val gyeongsangnamdoCityCodes = listOf(
        38010, // 창원시
        38020, // 마산시
        38030, // 진주시
        38040, // 통영시
        38050, // 사천시
        38060, // 김해시
        38070, // 밀양시
        38080, // 거제시
        38090, // 양산시
        38100, // 의령군
        38110, // 함안군
        38120, // 창녕군
        38130, // 고성군
        38140, // 남해군
        38150, // 하동군
        38160, // 산청군
        38170, // 함양군
        38180, // 거창군
        38190  // 합천군
    )

    LaunchedEffect(Unit) {
        while (true) {
            scheduleBusArrivalWork(context)
            alarmBusArrivals.forEach { arrival ->
                coroutineScope.launch {
                    try {
                        val apiKey = URLDecoder.decode(
                            "cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D",
                            "UTF-8"
                        )
                        gyeongsangnamdoCityCodes.forEach { cityCode ->
                            val response = BusApiClient.apiService.getBusArrivalInfo(
                                apiKey = apiKey,
                                cityCode = cityCode,
                                nodeId = arrival.nodeId!!
                            )
                            if (response.isSuccessful) {
                                val result = response.body()?.body?.items?.itemList ?: emptyList()
                                if (result.any { it.routeId == arrival.routeId && it.routeNo == arrival.routeNo }) {
                                    busArrivalInfo = result.sortedBy { it.arrTime ?: Int.MAX_VALUE }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("API Error", "도착 정보 로드 실패: ${e.message}")
                    }
                }
            }
            delay(10_000L)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("알람 설정 화면") },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 16.dp)
        ) {
            Text("뒤로 가기")
        }
        if (alarmBusArrivals.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("설정된 알람이 없습니다.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 130.dp)
            ) {
                items(alarmBusArrivals) { arrival ->
                    busArrivalInfo.forEach { arrivals ->
                        if (arrivals.nodeId == arrival.nodeId && arrivals.routeNo == arrival.routeNo && arrivals.routeId == arrival.routeId) {
                            val arrivalMinutes = (arrivals.arrTime ?: 0) / 60
                            val remainingStations = arrivals.arrPrevStationCnt ?: 0
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "${arrivals.nodeName}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "버스 번호: ${arrivals.routeNo}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "예상 도착 시간: ${arrivalMinutes}분 (${remainingStations}개 정류장)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val updatedList = alarmBusArrivals.toMutableList().apply {
                                                removeAll { it.nodeId == arrival.nodeId && it.routeNo == arrival.routeNo && it.routeId == arrival.routeId }
                                            }
                                            alarmBusArrivals = updatedList
                                            saveAlarms(context, updatedList)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("알람 해제")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class BusArrivalWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val context = applicationContext
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val alarmBusArrivals = loadAlarms(context)
            alarmBusArrivals.forEach { arrival ->
                try {
                    val apiKey = URLDecoder.decode("cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D", "UTF-8")
                    val gyeongsangnamdoCityCodes = listOf(
                        38010, 38020, 38030, 38040, 38050,
                        38060, 38070, 38080, 38090, 38100,
                        38110, 38120, 38130, 38140, 38150,
                        38160, 38170, 38180, 38190
                    )

                    for (cityCode in gyeongsangnamdoCityCodes) {
                        val response = withContext(Dispatchers.IO) {
                            BusApiClient.apiService.getBusArrivalInfo(
                                apiKey = apiKey,
                                cityCode = cityCode,
                                nodeId = arrival.nodeId!!
                            )
                        }

                        if (response.isSuccessful) {
                            val busArrivalInfo = response.body()?.body?.items?.itemList
                                ?.sortedBy { it.arrTime ?: Int.MAX_VALUE } ?: emptyList()

                            busArrivalInfo.forEach { arrivals ->
                                if (arrivals.nodeId == arrival.nodeId &&
                                    arrivals.routeNo == arrival.routeNo &&
                                    arrivals.routeId == arrival.routeId) {

                                    val remainingStations = arrivals.arrPrevStationCnt ?: 0

                                    if (remainingStations <= 5) {
                                        showNotification(context, arrivals)
                                        val updatedList = alarmBusArrivals.toMutableList().apply {
                                            removeAll { it.nodeId == arrival.nodeId && it.routeNo == arrival.routeNo && it.routeId == arrival.routeId }
                                        }
                                        saveAlarms(context, updatedList)
                                    }
                                }
                            }


                        }
                    }

                } catch (e: Exception) {
                    Log.e("Worker Error", "Error during bus arrival info processing: ${e.message}")
                }
            }
        }
        return Result.success()
    }
}

fun toggleAlarm(busArrival: BusArrivalItem, alarmBusArrivals: MutableList<BusArrivalItem>, context: Context) {
    if (alarmBusArrivals.any { it.nodeId == busArrival.nodeId && it.routeNo == busArrival.routeNo && it.routeId == busArrival.routeId }) {
        alarmBusArrivals.removeAll { it.nodeId == busArrival.nodeId && it.routeNo == busArrival.routeNo && it.routeId == busArrival.routeId }
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

fun scheduleBusArrivalWork(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<BusArrivalWork>(1, TimeUnit.MINUTES)
        .setInitialDelay(0, TimeUnit.SECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "BusArrivalWork",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}

fun showNotification(context: Context, arrivals: BusArrivalItem) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "bus_arrival_channel"
        val channelName = "버스 도착 알림"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance)
        channel.description = "버스 도착 알림 채널"
        notificationManager.createNotificationChannel(channel)
    }
    val notification = Notification.Builder(context, "bus_arrival_channel")
        .setContentTitle("버스 도착 알림")
        .setContentText("${arrivals.routeNo}번 버스가 ${arrivals.arrPrevStationCnt}개 정류장 남았습니다.")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(Notification.PRIORITY_HIGH)
        .build()
    notificationManager.notify(arrivals.routeNo.hashCode(), notification)
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "BUS_ARRIVAL_CHANNEL"
        val channelName = "Bus Arrival Notifications"
        val channelDescription = "Channel for bus arrival notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

fun saveStepCount(context: Context, stepCount: Int) {
    val sharedPreferences = context.getSharedPreferences("step_data", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putInt("step_count", stepCount)
    editor.apply()
}

fun loadStepCount(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("step_data", Context.MODE_PRIVATE)
    return sharedPreferences.getInt("step_count", 0)
}

@Preview(showBackground = true)
@Composable
fun PreviewBusAppContent() {
    Geupjo_BusTheme {
        BusAppContent(onSearchClick = {}, onRouteSearchClick = {})
    }
}

@Composable
fun MapScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "맵 화면",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = onBackClick) {
            Text("뒤로가기")
        }
    }
}
