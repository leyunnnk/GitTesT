package com.example.geupjo_bus.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

// XML의 <item> 태그에 해당하는 버스 정류장 정보
@Root(name = "item", strict = false)
data class BusStop(
    @field:Element(name = "citycode", required = false) var cityCode: Int? = null,
    @field:Element(name = "gpslati", required = false) var latitude: Double? = null,
    @field:Element(name = "gpslong", required = false) var longitude: Double? = null,
    @field:Element(name = "nodeid", required = false) var nodeId: String? = null,
    @field:Element(name = "nodenm", required = false) var nodeName: String? = null,  // 올바른 필드 이름
    @field:Element(name = "nodeno", required = false) var nodeNumber: String? = null // String 타입으로 수정
)

// 전체 응답을 담는 클래스 (items를 포함)
@Root(name = "response", strict = false)
data class BusStopResponse(
    @field:Element(name = "header", required = false) var header: ResponseHeader? = null,
    @field:Element(name = "body", required = false) var body: ResponseBody? = null
)

// 응답의 헤더 정보
@Root(name = "header", strict = false)
data class ResponseHeader(
    @field:Element(name = "resultCode", required = false) var resultCode: String? = null,
    @field:Element(name = "resultMsg", required = false) var resultMsg: String? = null
)

// 응답의 본문 (items를 포함)
@Root(name = "body", strict = false)
data class ResponseBody(
    @field:Element(name = "items", required = false) var items: Items? = null,
    @field:Element(name = "numOfRows", required = false) var numOfRows: Int? = null,
    @field:Element(name = "pageNo", required = false) var pageNo: Int? = null,
    @field:Element(name = "totalCount", required = false) var totalCount: Int? = null
)

// 실제 아이템 목록
@Root(name = "items", strict = false)
data class Items(
    @field:ElementList(entry = "item", inline = true, required = false) var itemList: List<BusStop>? = null
)
