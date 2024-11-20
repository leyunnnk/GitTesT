package com.example.geupjo_bus.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "response", strict = false)
data class BusArrivalResponse(
    @field:Element(name = "header", required = false)
    var header: Header? = null,

    @field:Element(name = "body", required = false)
    var body: Body? = null
)

@Root(name = "header", strict = false)
data class Header(
    @field:Element(name = "resultCode", required = false)
    var resultCode: String? = null,

    @field:Element(name = "resultMsg", required = false)
    var resultMsg: String? = null
)

@Root(name = "body", strict = false)
data class Body(
    @field:Element(name = "items", required = false)
    var items: ArrivalItems? = null,

    @field:Element(name = "numOfRows", required = false)
    var numOfRows: Int? = null,

    @field:Element(name = "pageNo", required = false)
    var pageNo: Int? = null,

    @field:Element(name = "totalCount", required = false)
    var totalCount: Int? = null
)

@Root(name = "items", strict = false)
data class ArrivalItems(
    @field:ElementList(name = "item", inline = true, required = false)
    var itemList: List<BusArrivalItem>? = null
)

@Root(name = "item", strict = false)
data class BusArrivalItem(
    @field:Element(name = "arrprevstationcnt", required = false)
    var arrPrevStationCnt: Int? = null,

    @field:Element(name = "arrtime", required = false)
    var arrTime: Int? = null,

    @field:Element(name = "nodeid", required = false)
    var nodeId: String? = null,

    @field:Element(name = "nodenm", required = false)
    var nodeName: String? = null,

    @field:Element(name = "routeid", required = false)
    var routeId: String? = null,

    @field:Element(name = "routeno", required = false)
    var routeNo: String? = null,

    @field:Element(name = "routetp", required = false)
    var routeType: String? = null,

    @field:Element(name = "vehicletp", required = false)
    var vehicleType: String? = null
)