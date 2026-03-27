(function () {
    const mapRegistry = new Map();

    function escapeHtml(text) {
        return String(text || '').replace(/[&<>"']/g, function (char) {
            const map = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            };
            return map[char];
        });
    }

    function getNode(id) {
        return id ? document.getElementById(id) : null;
    }

    function isValidCoord(lat, lng) {
        return Number.isFinite(lat) && Number.isFinite(lng);
    }

    function toNumber(value) {
        if (value === null || value === undefined || value === '') {
            return Number.NaN;
        }
        const number = Number(value);
        return Number.isFinite(number) ? number : Number.NaN;
    }

    function setDistanceAndCost(distanceNode, costNode, distanceKm) {
        if (!distanceNode || !costNode) {
            return;
        }

        if (!Number.isFinite(distanceKm) || distanceKm < 0) {
            distanceNode.textContent = 'N/A';
            costNode.textContent = 'N/A';
            return;
        }

        const roundedDistance = distanceKm.toFixed(2);
        const cost = (distanceKm * 2).toFixed(2);
        distanceNode.textContent = roundedDistance + ' km';
        costNode.textContent = cost + ' taka';
    }

    function setRouteStatus(routeNode, text, isError) {
        if (!routeNode) {
            return;
        }

        routeNode.textContent = text;
        routeNode.className = isError
            ? 'mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700 route-map-status route-map-status-error'
            : 'mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700 route-map-status route-map-status-success';
    }

    async function reverseGeocode(lat, lng) {
        const url = 'https://nominatim.openstreetmap.org/reverse?lat='
            + encodeURIComponent(lat)
            + '&lon=' + encodeURIComponent(lng)
            + '&format=json';

        const response = await fetch(url, {
            headers: {
                Accept: 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Reverse geocoding failed');
        }

        const payload = await response.json();
        return payload.display_name || 'Address unavailable';
    }

    async function setAddress(addressNode, address, lat, lng) {
        if (!addressNode) {
            return;
        }

        if (address) {
            addressNode.textContent = '📍 ' + address;
            return;
        }

        try {
            const resolved = await reverseGeocode(lat, lng);
            addressNode.textContent = '📍 ' + resolved;
        } catch (_) {
            addressNode.textContent = '📍 Address unavailable';
        }
    }

    function buildRouteUrl(from, to) {
        return 'https://router.project-osrm.org/route/v1/driving/'
            + encodeURIComponent(from.lng) + ',' + encodeURIComponent(from.lat)
            + ';'
            + encodeURIComponent(to.lng) + ',' + encodeURIComponent(to.lat)
            + '?overview=full&geometries=geojson';
    }

    async function loadRouteFromOsrm(map, from, to) {
        const response = await fetch(buildRouteUrl(from, to));
        if (!response.ok) {
            throw new Error('OSRM request failed');
        }

        const payload = await response.json();
        if (!payload.routes || payload.routes.length === 0) {
            throw new Error('No route available from OSRM');
        }

        const route = payload.routes[0];
        const routeLayer = L.geoJSON(route.geometry, {
            style: {
                color: '#f43f6a',
                weight: 5,
                opacity: 0.85
            }
        }).addTo(map);

        map.fitBounds(routeLayer.getBounds(), { padding: [35, 35] });
        return route.distance / 1000;
    }

    function destroyMap(mapId) {
        const existingMap = mapRegistry.get(mapId);
        if (existingMap) {
            existingMap.remove();
            mapRegistry.delete(mapId);
        }
    }

    async function renderRouteMap(config) {
        if (!window.L) {
            throw new Error('Leaflet is required to render route map.');
        }

        const mapNode = getNode(config.mapId);
        if (!mapNode) {
            throw new Error('Map container not found.');
        }

        const routeNode = getNode(config.routeStatusId);
        const distanceNode = getNode(config.distanceValueId);
        const costNode = getNode(config.costValueId);
        const senderAddressNode = getNode(config.senderAddressId);
        const receiverAddressNode = getNode(config.receiverAddressId);

        const sender = {
            lat: toNumber(config.senderLat),
            lng: toNumber(config.senderLng),
            name: config.senderName || 'Sender',
            address: config.senderAddress || null
        };

        const receiver = {
            lat: toNumber(config.receiverLat),
            lng: toNumber(config.receiverLng),
            name: config.receiverName || 'Receiver',
            address: config.receiverAddress || null
        };

        if (!isValidCoord(sender.lat, sender.lng) || !isValidCoord(receiver.lat, receiver.lng)) {
            setDistanceAndCost(distanceNode, costNode, Number.NaN);
            setRouteStatus(routeNode, 'Location not available for this user', true);
            if (senderAddressNode) {
                senderAddressNode.textContent = '📍 Location not available for this user';
            }
            if (receiverAddressNode) {
                receiverAddressNode.textContent = '📍 Location not available for this user';
            }
            return;
        }

        destroyMap(config.mapId);

        const map = L.map(mapNode).setView([sender.lat, sender.lng], 12);
        mapRegistry.set(config.mapId, map);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        L.marker([sender.lat, sender.lng]).addTo(map).bindPopup(
            '<strong>' + escapeHtml(sender.name) + '</strong>'
            + (sender.address ? '<br/><small>' + escapeHtml(sender.address) + '</small>' : '')
        );

        L.marker([receiver.lat, receiver.lng]).addTo(map).bindPopup(
            '<strong>' + escapeHtml(receiver.name) + '</strong>'
            + (receiver.address ? '<br/><small>' + escapeHtml(receiver.address) + '</small>' : '')
        );

        const bounds = L.latLngBounds([
            [sender.lat, sender.lng],
            [receiver.lat, receiver.lng]
        ]);
        map.fitBounds(bounds, { padding: [35, 35] });

        setDistanceAndCost(distanceNode, costNode, toNumber(config.distanceKm));
        setRouteStatus(routeNode, 'Loading route from OSRM...', false);

        await Promise.all([
            setAddress(senderAddressNode, sender.address, sender.lat, sender.lng),
            setAddress(receiverAddressNode, receiver.address, receiver.lat, receiver.lng)
        ]);

        try {
            const distanceKm = await loadRouteFromOsrm(map, sender, receiver);
            setDistanceAndCost(distanceNode, costNode, distanceKm);
            setRouteStatus(routeNode, 'Shortest driving route loaded from OSRM.', false);
        } catch (_) {
            setRouteStatus(routeNode, 'Could not load live OSRM route. Showing estimated values.', true);
        }

        setTimeout(function () {
            map.invalidateSize();
        }, 120);
    }

    function bindModalClose(modalId) {
        const modal = getNode(modalId);
        if (!modal || modal.dataset.routeMapBound === '1') {
            return;
        }

        modal.dataset.routeMapBound = '1';

        modal.addEventListener('click', function (event) {
            if (event.target.hasAttribute('data-route-map-close') || event.target === modal) {
                modal.classList.add('hidden');
            }
        });
    }

    async function openRouteMap(userLat, userLng, otherLat, otherLng, otherAddress, options) {
        const settings = options || {};
        const modalId = settings.modalId || 'routeMapModal';
        const modal = getNode(modalId);
        if (!modal) {
            throw new Error('Route map modal not found.');
        }

        bindModalClose(modalId);
        modal.classList.remove('hidden');

        const titleNode = getNode(settings.titleId || 'routeMapTitle');
        if (titleNode) {
            titleNode.textContent = settings.title || 'Exchange Route Map & Cost';
        }

        return renderRouteMap({
            mapId: settings.mapId || 'routeMapCanvas',
            senderLat: userLat,
            senderLng: userLng,
            senderName: settings.currentUserName || 'You',
            senderAddress: settings.currentUserAddress || null,
            senderAddressId: settings.senderAddressId || 'senderAddress',
            receiverLat: otherLat,
            receiverLng: otherLng,
            receiverName: settings.otherUserName || 'Offer Owner',
            receiverAddress: otherAddress || null,
            receiverAddressId: settings.receiverAddressId || 'receiverAddress',
            routeStatusId: settings.routeStatusId || 'routeStatus',
            distanceValueId: settings.distanceValueId || 'distanceValue',
            costValueId: settings.costValueId || 'costValue',
            distanceKm: settings.distanceKm || null
        });
    }

    window.BiblioRouteMap = {
        renderRouteMap: renderRouteMap,
        openRouteMap: openRouteMap
    };

    window.openRouteMap = openRouteMap;
})();