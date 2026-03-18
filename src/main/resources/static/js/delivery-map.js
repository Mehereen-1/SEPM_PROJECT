(function () {
    const mapNode = document.getElementById('deliveryMap');
    if (!mapNode || !window.deliveryRouteData || !window.BiblioRouteMap) {
        return;
    }

    const data = window.deliveryRouteData;

    window.BiblioRouteMap.renderRouteMap({
        mapId: 'deliveryMap',
        senderLat: data.senderLat,
        senderLng: data.senderLng,
        senderName: data.senderName || 'Sender',
        senderAddress: data.senderAddress,
        senderAddressId: 'senderAddress',
        receiverLat: data.receiverLat,
        receiverLng: data.receiverLng,
        receiverName: data.receiverName || 'Receiver',
        receiverAddress: data.receiverAddress,
        receiverAddressId: 'receiverAddress',
        routeStatusId: 'routeStatus',
        distanceValueId: 'distanceValue',
        costValueId: 'costValue',
        distanceKm: data.distanceKm
    });
})();
