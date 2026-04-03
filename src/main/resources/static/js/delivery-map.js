(function () {
    const mapNode = document.getElementById('deliveryMap');
    if (!mapNode || !window.deliveryRouteData || !window.BiblioRouteMap) {
        return;
    }

    const data = window.deliveryRouteData;

    function toNumber(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }

        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function buildConfig(routeData) {
        const resolvedDistanceKm = toNumber(routeData.distance_km ?? routeData.distanceKm ?? data.distanceKm);
        const resolvedCost = toNumber(routeData.cost ?? routeData.deliveryCost ?? routeData.delivery_cost ?? data.deliveryCost);
        const resolvedCostPerKm = toNumber(routeData.cost_per_km ?? routeData.costPerKm ?? data.costPerKm);

        return {
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
            distanceKm: resolvedDistanceKm,
            deliveryCost: resolvedCost,
            costPerKm: resolvedCostPerKm
        };
    }

    async function renderRoute(routeData) {
        await window.BiblioRouteMap.renderRouteMap(buildConfig(routeData));
    }

    async function loadRouteData() {
        if (!Number.isFinite(Number(data.offerId))) {
            await renderRoute(data);
            return;
        }

        try {
            const response = await fetch('/delivery/location-data/' + encodeURIComponent(data.offerId), {
                headers: { Accept: 'application/json' }
            });

            if (!response.ok) {
                throw new Error('Failed to load route data');
            }

            const routeData = await response.json();
            await renderRoute(routeData);
        } catch (error) {
            console.warn('routeData API unavailable, using initial payload', error);
            await renderRoute(data);
        }
    }

    loadRouteData();
})();
