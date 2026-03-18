(function (window) {
    const DEFAULT_COORDS = { lat: 23.8103, lng: 90.4125 };

    function asNumber(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function formatCoord(value) {
        return Number(value).toFixed(6);
    }

    function setStatus(statusNode, text) {
        if (statusNode) {
            statusNode.textContent = text;
        }
    }

    async function reverseGeocode(lat, lng) {
        try {
            const url = `https://nominatim.openstreetmap.org/reverse?lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lng)}&format=json`;
            const response = await fetch(url, {
                headers: { Accept: 'application/json' }
            });

            if (!response.ok) {
                throw new Error('Geocoding failed');
            }

            const json = await response.json();
            return json.display_name || null;
        } catch (err) {
            return null;
        }
    }

    function init(options) {
        const mapNode = document.getElementById(options.mapId);
        const latitudeInput = document.getElementById(options.latitudeInputId);
        const longitudeInput = document.getElementById(options.longitudeInputId);
        const addressInput = options.addressInputId ? document.getElementById(options.addressInputId) : null;
        const latitudeDisplay = options.latitudeDisplayId ? document.getElementById(options.latitudeDisplayId) : null;
        const longitudeDisplay = options.longitudeDisplayId ? document.getElementById(options.longitudeDisplayId) : null;
        const addressDisplay = options.addressDisplayId ? document.getElementById(options.addressDisplayId) : null;
        const statusNode = options.statusId ? document.getElementById(options.statusId) : null;

        if (!mapNode || !latitudeInput || !longitudeInput || !window.L) {
            return;
        }

        const initialLat = asNumber(options.initialLatitude);
        const initialLng = asNumber(options.initialLongitude);
        const initialAddress = options.initialAddress || null;
        const initial = {
            lat: initialLat === null ? DEFAULT_COORDS.lat : initialLat,
            lng: initialLng === null ? DEFAULT_COORDS.lng : initialLng
        };

        const map = L.map(mapNode, { zoomControl: true }).setView([initial.lat, initial.lng], 13);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        const marker = L.marker([initial.lat, initial.lng], { draggable: true }).addTo(map);

        function updateLatLng(lat, lng) {
            latitudeInput.value = String(lat);
            longitudeInput.value = String(lng);

            if (latitudeDisplay) {
                latitudeDisplay.value = formatCoord(lat);
            }
            if (longitudeDisplay) {
                longitudeDisplay.value = formatCoord(lng);
            }
        }

        async function updateAddress(lat, lng) {
            if (!addressInput && !addressDisplay) {
                return;
            }

            const address = await reverseGeocode(lat, lng);
            if (address) {
                if (addressInput) {
                    addressInput.value = address;
                }
                if (addressDisplay) {
                    addressDisplay.textContent = `📍 ${address}`;
                }
                setStatus(statusNode, `Location set to: ${address}`);
            } else {
                setStatus(statusNode, 'Unable to resolve address. You can try another spot.');
            }
        }

        function setPoint(lat, lng, shouldPan) {
            marker.setLatLng([lat, lng]);
            if (shouldPan) {
                map.panTo([lat, lng]);
            }
            updateLatLng(lat, lng);
            updateAddress(lat, lng);
        }

        marker.on('dragend', function () {
            const position = marker.getLatLng();
            setPoint(position.lat, position.lng, false);
        });

        map.on('click', function (event) {
            setPoint(event.latlng.lat, event.latlng.lng, false);
        });

        const geocoderControl = L.Control.geocoder({
            defaultMarkGeocode: false,
            geocoder: L.Control.Geocoder.nominatim()
        }).on('markgeocode', function (e) {
            const latlng = e.geocode.center;
            setPoint(latlng.lat, latlng.lng, true);
        }).addTo(map);

        map.on('geocoder_show', function () {
            setStatus(statusNode, 'Search results shown. Click a result to select location.');
        });

        updateLatLng(initial.lat, initial.lng);
        if (initialAddress) {
            if (addressDisplay) {
                addressDisplay.textContent = `📍 ${initialAddress}`;
            }
            if (addressInput) {
                addressInput.value = initialAddress;
            }
            setStatus(statusNode, `Using saved address: ${initialAddress}`);
        }

        if (initialLat !== null && initialLng !== null) {
            setStatus(statusNode, 'Using your saved location. You can search, drag, or click to update.');
            return;
        }

        if (!navigator.geolocation) {
            setStatus(statusNode, 'Geolocation not supported. Use search or click to choose location.');
            setPoint(DEFAULT_COORDS.lat, DEFAULT_COORDS.lng, true);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            function (position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                setPoint(lat, lng, true);
                setStatus(statusNode, 'Detected current location. You can still adjust it.');
            },
            function () {
                setPoint(DEFAULT_COORDS.lat, DEFAULT_COORDS.lng, true);
                setStatus(statusNode, 'Location permission denied. Search or click to pick a location.');
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0
            }
        );
    }

    window.BiblioLocationPicker = {
        init: init
    };
})(window);
