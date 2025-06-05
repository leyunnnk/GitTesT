package com.example.geupjo_bus.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "response", strict = false)
data class BusRouteResponse(
    @field:Element(name = "body", required = false)
    var body: BusRouteBody? = null
)

@Root(name = "body", strict = false)
data class BusRouteBody(
    @field:Element(name = "items", required = false)
    var items: BusRouteItemList? = null // ⬅️ 여기 수정
)

@Root(name = "items", strict = false)
data class BusRouteItemList(
    @field:ElementList(entry = "item", inline = true, required = false)
    var itemList: List<BusRouteList>? = null
)

@Root(name = "item", strict = false)
data class BusRouteList(
    @field:Element(name = "gpslati", required = false) var latitude: Double? = null,
    @field:Element(name = "gpslong", required = false) var longitude: Double? = null,
    @field:Element(name = "nodenm", required = false) var nodeName: String? = null,
    @field:Element(name = "nodeord", required = false) var nodeOrder: String? = null
)