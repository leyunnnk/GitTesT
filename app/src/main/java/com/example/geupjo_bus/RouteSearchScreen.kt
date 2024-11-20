package com.example.geupjo_bus

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geupjo_bus.ui.theme.Geupjo_BusTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
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
    var isSearching by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 위치 가져오기 (LaunchedEffect로 위치 정보를 가져옴)
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            currentLocation = getCurrentLocation(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBackClick, modifier = Modifier.align(Alignment.Start)) {
            Text("뒤로 가기")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 현재 좌표 표시
        currentLocation?.let { location ->
            Text(
                text = "현재 위치: (${location.latitude}, ${location.longitude})",
                style = MaterialTheme.typography.bodyMedium
            )
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
                    routeResults = dummyRouteSearch(departure.text, destination.text)
                    isSearching = false
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isSearching = true
                routeResults = dummyRouteSearch(departure.text, destination.text)
                isSearching = false
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
        } else if (!isSearching) {
            Text(text = "검색 결과가 없습니다.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// 위치 정보를 가져오는 suspend 함수
@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    return suspendCoroutine { continuation ->
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}

@Composable
fun RouteSearchResultItem(route: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(text = route, style = MaterialTheme.typography.bodyMedium)
    }
}

// 더미 데이터 검색 로직 (API 적용 전)
fun dummyRouteSearch(departure: String, destination: String): List<String> {
    return if (departure.isNotEmpty() && destination.isNotEmpty()) {
        listOf(
            "출발지: $departure -> 도착지: $destination 경로 1",
            "출발지: $departure -> 도착지: $destination 경로 2",
            "출발지: $departure -> 도착지: $destination 경로 3"
        )
    } else {
        emptyList()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRouteSearchScreen() {
    Geupjo_BusTheme {
        RouteSearchScreen()
    }
}
