package com.example.geupjo_bus

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusStopItem
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import com.google.android.gms.location.LocationServices
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
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var searchResults by remember { mutableStateOf(listOf<BusStopItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedBusStopName by remember { mutableStateOf("") }
    var selectedBusStopId by remember { mutableStateOf("") }
    var busRouteInfo by remember { mutableStateOf("경유 노선 정보를 로드 중입니다...") }
    val coroutineScope = rememberCoroutineScope()

    // 위도와 경도 상태 저장
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    // FusedLocationProviderClient를 통해 현재 위치를 가져오는 작업
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 위치 권한 요청
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한 요청 (UI로 위치 권한 요청 표시)
        } else {
            // 위치 정보 가져오기
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    // SharedPreferences나 다른 방법으로 위도/경도 저장 가능
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 뒤로가기 버튼
        Button(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.Start),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {
            Text("뒤로 가기", color = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 제목
        Text(
            text = "정류장 검색",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 검색 입력 필드
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue -> searchQuery = newValue },
            label = { Text("정류장 이름 입력") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "검색 아이콘")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    coroutineScope.launch {
                        searchResults = searchBusStopsFromApi(searchQuery.text, apiKey)
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

        // 검색 결과 표시
        if (searchResults.isNotEmpty()) {
            Text(
                text = "검색 결과:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                searchResults.forEach { result ->
                    latitude?.let {
                        longitude?.let { it1 ->
                            BusStopSearchResultItem(
                                busStopName = result.nodeName ?: "알 수 없음",
                                currentlati = it,
                                currentlong = it1,
                                nodeLati = result.nodeLati ?: "알 수 없음".toDouble(),
                                nodeLong = result.nodeLong ?: "알 수 없음".toDouble(),
                                onClick = {
                                    val currentBusStopId = result.nodeId ?: ""
                                    selectedBusStopName = result.nodeName ?: "알 수 없음"
                                    showDialog = true
                                    coroutineScope.launch {
                                        showDialog = false // Dialog를 닫아서 상태를 초기화
                                        busRouteInfo = "경유 노선 정보를 로드 중입니다..."
                                        busRouteInfo = fetchBusRoutesByStop(currentBusStopId, apiKey)
                                        showDialog = true // Dialog를 다시 열어서 업데이트된 값을 렌더링
                                    }
                                    onBusStopClick(result.nodeName ?: "알 수 없음")
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            // 결과 없음 텍스트
            Text(
                text = "검색 결과가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // AlertDialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = selectedBusStopName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = busRouteInfo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(8.dp)
                )
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("확인")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }
}


@Composable
fun BusStopSearchResultItem(busStopName: String, currentlati: Double, currentlong: Double, nodeLati: Double, nodeLong: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = busStopName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(

            text = "${getDistance(currentlati, currentlong, nodeLati, nodeLong).toInt()}m",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


suspend fun searchBusStopsFromApi(query: String, apiKey: String): List<BusStopItem> {
    return try {
        val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
        val response = BusApiClient.apiService.searchBusStops(
            apiKey = decodedKey,
            cityCode = 38030,
            nodeNm = query
        )

        if (response.isSuccessful) {
            response.body()?.body?.items?.itemList ?: emptyList()
        } else {
            Log.e("API Error", "API 호출 실패 - 코드: ${response.code()}, 메시지: ${response.message()}")
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("API Exception", "API 호출 오류: ${e.message}")
        emptyList()
    }
}

suspend fun fetchBusRoutesByStop(busStopId: String, apiKey: String): String {
    return try {
        Log.d("fetchBusRoutesByStop", "전달된 BusStopId: $busStopId")
        val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
        val response = BusApiClient.apiService.getBusRoutesByStop(
            apiKey = decodedKey,
            cityCode = 38030,
            nodeId = busStopId,  // 'nodeId'를 Retrofit 인터페이스에 맞게 전달
            numOfRows = 10,      // 필요한 요청 수
            pageNo = 1           // 페이지 번호
        )

        // 요청 URL 확인
        Log.d("Retrofit URL", "Final URL: ${response.raw().request.url}")

        if (response.isSuccessful) {
            response.body()?.body?.items?.itemList?.joinToString("\n") { item ->
                val routeNo = item.routeNo ?: "알 수 없음"
                val endNodeName = item.endNodeName ?: "알 수 없음"
                "$routeNo 번 버스 - ($endNodeName 방향)"
            } ?: "해당 정류장의 경유 노선 정보가 없습니다."
        } else {
            Log.e("API Error", "Failed with Code: ${response.code()}, Message: ${response.message()}")
            "경유 노선 정보를 가져오는 데 실패했습니다."
        }
    } catch (e: Exception) {
        Log.e("API Exception", "오류 발생: ${e.message}")
        "경유 노선 정보를 가져오는 중 오류가 발생했습니다."
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewBusStopSearchScreen() {
    Geupjo_BusTheme {
        BusStopSearchScreen(
            onBackClick = {},
            apiKey = "DUMMY_API_KEY",
            onBusStopClick = { busStopName ->
                Log.d("Preview", "Clicked on bus stop: $busStopName")
            }
        )
    }
}
