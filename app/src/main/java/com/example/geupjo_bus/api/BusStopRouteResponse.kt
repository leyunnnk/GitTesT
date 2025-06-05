package com.example.geupjo_bus.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "response", strict = false)
data class BusStopRouteResponse(
    @field:Element(name = "body", required = false)
    var body: BusRouteResponseBody? = null
)

@Root(name = "body", strict = false)
data class BusRouteResponseBody(
    @field:Element(name = "items", required = false)
    var items: BusRouteItems? = null,

    @field:Element(name = "numOfRows", required = false)
    var numOfRows: Int? = null,

    @field:Element(name = "pageNo", required = false)
    var pageNo: Int? = null,

    @field:Element(name = "totalCount", required = false)
    var totalCount: Int? = null
)

@Root(name = "items", strict = false)
data class BusRouteItems(
    @field:ElementList(name = "item", inline = true, required = false)
    var itemList: List<BusRouteItem>? = null
)

@Root(name = "item", strict = false)
data class BusRouteItem(
    @field:Element(name = "routeid", required = false)
    var routeId: String? = null,

    @field:Element(name = "routeno", required = false)
    var routeNo: String? = null,

    @field:Element(name = "routetp", required = false)
    var routeType: String? = null,

    @field:Element(name = "endnodenm", required = false)
    var endNodeName: String? = null,

    @field:Element(name = "startnodenm", required = false)
    var startNodeName: String? = null
)
