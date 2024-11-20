package com.example.geupjo_bus.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

object BusApiClient {
    private const val BASE_URL = "http://apis.data.go.kr/" // 공공 데이터 API의 실제 URL로 교체

    val apiService: BusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create()) // XML 변환기 추가
            .build()
            .create(BusApiService::class.java) // BusApiService를 생성
    }
}
