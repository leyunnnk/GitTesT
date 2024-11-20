package com.example.geupjo_bus

import com.example.geupjo_bus.api.BusArrivalItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geupjo_bus.api.BusApiClient
import com.example.geupjo_bus.api.BusStopItem
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import kotlinx.coroutines.delay
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
    var busArrivalInfo by remember { mutableStateOf("버스 도착 정보를 로드 중입니다...") }
    val coroutineScope = rememberCoroutineScope()

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

        Text(text = "정류장 검색", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))


        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue -> searchQuery = newValue },
            label = { Text("정류장 이름 입력") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    coroutineScope.launch {
                        searchResults = searchBusStopsFromApi(searchQuery.text, apiKey)
                    }
                }
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )


        Spacer(modifier = Modifier.height(16.dp))

        if (searchResults.isNotEmpty()) {
            Text(
                text = "검색 결과:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            searchResults.forEach { result ->
                BusStopSearchResultItem(
                    busStopName = result.nodeName ?: "알 수 없음",
                    onClick = {
                        selectedBusStopName = result.nodeName ?: "알 수 없음"
                        selectedBusStopId = result.nodeId ?: ""
                        showDialog = true
                        coroutineScope.launch {
                            busArrivalInfo = fetchBusArrivalInfo(selectedBusStopId, apiKey)
                        }
                        onBusStopClick(result.nodeName ?: "알 수 없음")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(text = "검색 결과가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    // 15초마다 버스 도착 정보 업데이트
    LaunchedEffect(showDialog, selectedBusStopId) {
        while (showDialog) {
            busArrivalInfo = fetchBusArrivalInfo(selectedBusStopId, apiKey)
            delay(15000) // 15초마다 새로고침
        }
    }

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
                Text(busArrivalInfo, color = MaterialTheme.colorScheme.onBackground)
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("확인")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun BusStopSearchResultItem(busStopName: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = busStopName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 정류장 검색 API 호출 함수
suspend fun searchBusStopsFromApi(query: String, apiKey: String): List<BusStopItem> {
    return try {
        val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
        val response = BusApiClient.apiService.searchBusStops(
            apiKey = decodedKey,
            cityCode = 38030,
            nodeNm = query
        )

        if (response.isSuccessful) {
            val bodyResponse: List<BusStopItem>? = response.body()?.body?.items?.itemList
            bodyResponse ?: emptyList()
        } else {
            Log.e("API Error", "API 호출 실패 - 코드: ${response.code()}, 메시지: ${response.message()}")
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("API Exception", "API 호출 오류: ${e.message}")
        emptyList()
    }
}

// 버스 도착 정보 조회 API 호출 함수
suspend fun fetchBusArrivalInfo(busStopId: String, apiKey: String): String {
    return try {
        val decodedKey = URLDecoder.decode(apiKey, "UTF-8")
        val response = BusApiClient.apiService.getBusArrivalInfo(
            apiKey = decodedKey,
            cityCode = 38030,
            nodeId = busStopId
        )

        if (response.isSuccessful) {
            response.body()?.body?.items?.itemList?.joinToString("\n") { item ->
                val routeNo = item.routeNo ?: "알 수 없음"
                val arrTime = (item.arrTime ?: 0) / 60  // 초 단위 -> 분 단위 변환
                val arrPrevStationCnt = item.arrPrevStationCnt ?: "알 수 없음"
                "$routeNo 번 버스 - 약 ${arrTime}분 후 도착 (남은 정류장: ${arrPrevStationCnt}개)"
            } ?: "도착 정보 없음"
        } else {
            "도착 정보를 가져오는 데 실패했습니다. 코드: ${response.code()}, 메시지: ${response.message()}"
        }
    } catch (e: Exception) {
        Log.e("API Error", "도착 정보 호출 오류: ${e.message}")
        "도착 정보를 가져오는 중 오류가 발생했습니다."
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