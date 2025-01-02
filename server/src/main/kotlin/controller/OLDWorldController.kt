package controller

// ---------------------------------
// This is all that's left of the old controller. This was partially working, didn't fully implement it

//     @Get("reorder/{clientOrderCS}", produces = [APPLICATION_OCTET_STREAM])
//     fun reorderClients(clientOrderCS: String): HttpResponse<String> {
//         println("reorder, ids: $clientOrderCS")
//         val newClientOrder = clientOrderCS.split(",").map { it.toInt() }
//         val newIds = newClientOrder.toSortedSet()

//         val oldClients = world.clients()
//         val oldIds = oldClients.map { it.id }.toSortedSet()

//         if (oldIds != newIds) {
//             println("Error: old and new ids do not match, oldIds: $oldIds, newIds: $newIds")
//             return HttpResponse.badRequest("New IDs do not match current client IDs, oldIds: $oldIds, newIds: $newIds")
//         }

//         world.rebuild(newClientOrder)
//         return HttpResponse.ok("")
//     }
