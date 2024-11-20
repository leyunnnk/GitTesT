package com.example.geupjo_bus.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.geupjo_bus.R

// 버스 정류장 데이터 클래스
data class BusStop(
    val name: String,
    val location: LatLng,
    val info: String
)

@Composable
fun MapScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle(context)
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionGranted = remember { mutableStateOf(false) }
    var selectedBusStop by remember { mutableStateOf<BusStop?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        locationPermissionGranted.value = isGranted
        if (isGranted) {
            getCurrentLocationAndSetMap(fusedLocationClient, googleMap, context)
        } else {
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted.value = true
            getCurrentLocationAndSetMap(fusedLocationClient, googleMap, context)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { map ->
            map.getMapAsync { gMap ->
                googleMap = gMap
                googleMap?.let { map ->
                    if (locationPermissionGranted.value) {
                        getCurrentLocationAndSetMap(fusedLocationClient, map, context)
                    }
                }
            }
        }

        // 선택된 정류장이 있을 때 Dialog로 정보를 표시
        selectedBusStop?.let { busStop ->
            BusStopInfoDialog(busStop = busStop, onDismiss = { selectedBusStop = null })
        }
    }
}

@Composable
fun BusStopInfoDialog(busStop: BusStop, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = busStop.name) },
        text = { Text(text = busStop.info) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

// 현재 위치를 가져와 지도 초기 위치로 설정하고, 주변 버스 정류장 마커 추가
private fun getCurrentLocationAndSetMap(
    fusedLocationClient: FusedLocationProviderClient,
    googleMap: GoogleMap?,
    context: Context
) {
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val currentLocation = LatLng(location.latitude, location.longitude)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17f))
            googleMap?.addMarker(
                MarkerOptions().position(currentLocation).title("현재 위치")
            )
            setupNearbyBusStops(currentLocation, googleMap, context)
        } else {
            Toast.makeText(context, "현재 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

// 주변 버스 정류장 마커 추가 및 마커 클릭 리스너 설정
private fun setupNearbyBusStops(
    currentLocation: LatLng,
    googleMap: GoogleMap?,
    context: Context
) {
    val busStops = listOf(
        BusStop("경상국립대학교 가좌캠퍼스 정문", LatLng(35.151994, 128.104567), "첫 번째 정류장입니다."),
        BusStop("정류장 B", LatLng(35.152771, 128.105747), "두 번째 정류장입니다."),
        BusStop("경상국립대학교 가좌캠퍼스 후문", LatLng(35.155421, 128.106986), "정보")
    )

    busStops.forEach { stop ->
        googleMap?.addMarker(
            MarkerOptions()
                .position(stop.location)
                .title(stop.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // 색상 변경
                .anchor(0.5f, 0.5f) // 아이콘 중앙 하단을 마커 위치로 설정
        )?.tag = stop
    }

    googleMap?.setOnMarkerClickListener { marker ->
        val busStop = marker.tag as? BusStop
        if (busStop != null) {
            marker.showInfoWindow()
            // 마커 클릭 시 선택된 정류장 정보를 MapScreen에서 상태로 관리
            var selectedBusStop = busStop
        }
        true
    }
}
// 벡터 이미지를 Bitmap으로 변환하여 마커 아이콘으로 사용
private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) as? VectorDrawable
    vectorDrawable?.let {
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    return null
}

// MapView의 생명주기를 관리하는 함수
@Composable
fun rememberMapViewWithLifecycle(context: Context): MapView {
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    return mapView
}
