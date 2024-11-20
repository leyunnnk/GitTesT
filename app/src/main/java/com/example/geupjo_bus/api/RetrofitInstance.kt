package com.example.geupjo_bus.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitInstance {

    private const val BASE_URL = "http://apis.data.go.kr/1613000/BusSttnInfoInqireService/" // 공공데이터 API의 기본 URL

    // Retrofit 인스턴스 생성
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 응답을 GSON으로 변환
            .build()
    }

    val apiService: BusApiService by lazy {
        retrofit.create(BusApiService::class.java)
    }
}

fun createRetrofitInstance(): Retrofit {
    // Logging Interceptor 설정
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // BODY를 사용하면 요청 및 응답 본문 전체를 기록함
    }

    // OkHttpClient에 Interceptor 추가
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit 인스턴스 생성
    return Retrofit.Builder()
        .baseUrl("http://apis.data.go.kr/1613000/BusSttnInfoInqireService/")  // API의 기본 URL
        .client(httpClient)  // loggingInterceptor를 포함한 OkHttpClient 사용
        .addConverterFactory(GsonConverterFactory.create())  // JSON 변환기 추가
        .build()
}
