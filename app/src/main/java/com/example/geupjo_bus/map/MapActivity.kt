package com.example.geupjo_bus.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.geupjo_bus.R
import androidx.appcompat.app.AppCompatActivity

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var busStopName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map) // activity_map.xml 파일 필요

        // Intent에서 정류장 이름 가져오기
        busStopName = intent.getStringExtra("BUS_STOP_NAME")

        // 지도 프래그먼트 초기화
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // 예시 좌표 (서울 시청 위치) - 실제 위치는 API를 통해 가져오도록 구현 필요
        val exampleLocation = LatLng(37.5665, 126.9780)

        // 마커 추가
        map.addMarker(
            MarkerOptions()
                .position(exampleLocation)
                .title(busStopName)
        )

        // 카메라를 선택한 정류장 위치로 이동
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(exampleLocation, 15f))
    }
}