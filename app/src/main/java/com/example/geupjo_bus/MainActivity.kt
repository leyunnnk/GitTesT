package com.example.geupjo_bus

import com.example.geupjo_bus.api.BusArrivalItem
import androidx.compose.ui.Alignment

import java.net.URLDecoder
import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusStop
import com.google.accompanist.permissions.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.util.Log
import kotlinx.coroutines.launch
import com.example.geupjo_bus.ui.MapScreen
import kotlinx.coroutines.Job

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class) // Accompanist 경고 처리
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Geupjo_BusTheme {
                var drawerState by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf("home") } // 현재 화면 상태 관리
                val scope = rememberCoroutineScope()

                Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("진주시 버스 정보") },
                                actions = {
                                    IconButton(onClick = {
                                        drawerState = true
                                    }) {
                                        Text("메뉴")
                                    }
                                },
                                colors = TopAppBarDefaults.largeTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        },
                        content = { innerPadding ->
                            // 화면 전환 로직
                            when (currentScreen) {
                                "home" -> BusAppContent(
                                    Modifier.padding(innerPadding),
                                    onSearchClick = { currentScreen = "search" }, // 검색 화면으로 전환
                                    onRouteSearchClick = { currentScreen = "route" } // 경로 검색 화면으로 전환
                                )
                                "search" -> BusStopSearchScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onBackClick = { currentScreen = "home" },
                                    apiKey = "cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D",
                                    onBusStopClick = { busStopName ->
                                        // 버스 정류장 클릭 시 수행할 동작
                                        Log.d("MainActivity", "Selected bus stop: $busStopName")
                                        // 예: 특정 버스 정류장 도착 정보 화면으로 이동하거나 관련 동작 수행
                                    }
                                )
                                "route" -> RouteSearchScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onBackClick = { currentScreen = "home" } // 홈 화면으로 돌아가기
                                )
                                "map" -> MapScreen( // 새로 추가된 맵 화면
                                    onBackClick = { currentScreen = "home" }
                                )
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = drawerState,
                        enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(500)
                        ),
                        exit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(500)
                        )
                    ) {
                        DrawerContent(
                            onDismiss = { drawerState = false },
                            onMenuItemClick = { screen ->
                                currentScreen = screen // 메뉴 클릭 시 화면 전환
                                drawerState = false
                            }
                        )
                    }
                }
            }
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
    val coroutineScope = rememberCoroutineScope()

    var busStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var selectedBusStop by remember { mutableStateOf<BusStop?>(null) }
    var busArrivalInfo by remember { mutableStateOf<List<BusArrivalItem>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }


    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            getCurrentLocation(context, fusedLocationClient) { lat, lng ->
                latitude = lat
                longitude = lng
                coroutineScope.launch {
                    try {
                        val encodedKey = "cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D"
                        val apiKey = URLDecoder.decode(encodedKey, "UTF-8")

                        val response = BusApiClient.apiService.getNearbyBusStops(
                            apiKey = apiKey,
                            latitude = latitude!!,
                            longitude = longitude!!
                        )

                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            busStops = responseBody?.body?.items?.itemList?.take(4) ?: emptyList()
                        } else {
                            Log.e("API Error", "API 호출 실패: ${response.code()}, ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e("API Error", "정류장 목록 로드 실패: ${e.message}")
                    }
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "GPS 기반 주변 정류장 목록:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (busStops.isNotEmpty()) {
            busStops.forEach { busStop ->
                NearbyBusStop(
                    busStopName = busStop.nodeName ?: "알 수 없음",
                    distance = busStop.nodeNumber ?: "알 수 없음",
                    onClick = {
                        selectedBusStop = busStop
                        coroutineScope.launch {
                            isLoading = true // 로딩 시작
                            try {
                                val apiKey = URLDecoder.decode("cvmPJ15BcYEn%2FRGNukBqLTRlCXkpITZSc6bWE7tWXdBSgY%2FeN%2BvzxH%2FROLnXu%2BThzVwBc09xoXfTyckHj1IJdg%3D%3D", "UTF-8")
                                val response = BusApiClient.apiService.getBusArrivalInfo(
                                    apiKey = apiKey,
                                    cityCode = 38030, // 진주시 코드
                                    nodeId = busStop.nodeId!!
                                )
                                if (response.isSuccessful) {
                                    // 도착 정보를 arrTime(예상 도착 시간) 기준으로 정렬
                                    busArrivalInfo = response.body()?.body?.items?.itemList
                                        ?.sortedBy { it.arrTime ?: Int.MAX_VALUE } // null은 가장 뒤로 이동
                                        ?: emptyList()

                                    if (busArrivalInfo.isEmpty()) {
                                        Log.d("Bus Info", "도착 버스 정보가 없습니다.")
                                    }
                                } else {
                                    Log.e("API Error", "도착 정보 호출 실패: ${response.code()}, ${response.message()}")
                                }
                            } catch (e: Exception) {
                                Log.e("API Error", "도착 정보 로드 실패: ${e.message}")
                            } finally {
                                isLoading = false // 로딩 종료
                                showDialog = true // 다이얼로그 표시
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

        } else {
            Text("주변 정류장 정보를 불러오는 중입니다...")
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
                when {
                    busArrivalInfo.isEmpty() && !isLoading -> {
                        // 도착 정보가 없을 때
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "도착 버스 정보가 없습니다.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    isLoading -> {
                        // 로딩 중일 때
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
                        // 데이터 로드 완료 후
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            busArrivalInfo.forEach { arrival ->
                                val arrivalMinutes = arrival.arrTime?.div(60) ?: 0
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
fun NearbyBusStop(busStopName: String, distance: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable (onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = busStopName, style = MaterialTheme.typography.titleMedium)
        Text(text = distance, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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

// 현재 위치를 가져오는 함수
fun getCurrentLocation(
    context: android.content.Context,
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

@Composable
fun DrawerContent(onDismiss: () -> Unit, onMenuItemClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .padding(16.dp)
            .background(Color.White)
    ) {
        Text(
            text = "닫기",
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DrawerMenuItem(label = "홈", onClick = { onMenuItemClick("home") })
        DrawerMenuItem(label = "정류장 검색", onClick = { onMenuItemClick("search") })
        DrawerMenuItem(label = "경로 검색", onClick = { onMenuItemClick("route") }) // 경로 검색 추가
        DrawerMenuItem(label = "맵", onClick = { onMenuItemClick("map") })
    }
}

@Composable
fun DrawerMenuItem(label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
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

        // 지도 표시나 추가적인 UI 요소들 넣기
    }
}
