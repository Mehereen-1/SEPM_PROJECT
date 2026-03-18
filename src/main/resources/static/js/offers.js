document.addEventListener('DOMContentLoaded', () => {
  const offersContainer = document.getElementById('offersContainer');
  const offerStats = document.getElementById('offerStats');
  const searchInput = document.getElementById('searchInput');
  const conditionFilter = document.getElementById('conditionFilter');
  const exchangeStatusBanner = document.getElementById('exchangeStatusBanner');
  const sentRequestsList = document.getElementById('sentRequestsList');
  const receivedRequestsList = document.getElementById('receivedRequestsList');
  const refreshRequestsBtn = document.getElementById('refreshRequestsBtn');

  let offers = [];
  let myActiveOffers = [];

  const escapeHtml = (value) => {
    if (value == null) {
      return '';
    }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const authHeaders = (includeJson = false) => {
    const headers = { Accept: 'application/json' };
    if (includeJson) {
      headers['Content-Type'] = 'application/json';
    }

    const token = localStorage.getItem('token');
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    return headers;
  };

  const readErrorMessage = async (response) => {
    try {
      const payload = await response.json();
      if (payload && payload.message) {
        return payload.message;
      }
    } catch (_) {
      // ignored on purpose
    }
    return `Request failed with status ${response.status}`;
  };

  const showBanner = (type, message) => {
    exchangeStatusBanner.className = `status-banner ${type}`;
    exchangeStatusBanner.textContent = message;
  };

  const clearBanner = () => {
    exchangeStatusBanner.className = 'status-banner';
    exchangeStatusBanner.textContent = '';
  };

  const showMessage = (cssClass, message) => {
    offersContainer.className = cssClass;
    offersContainer.innerHTML = `<div>${escapeHtml(message)}</div>`;
  };

  const setConditionOptions = (items) => {
    const staticOption = '<option value="">All conditions</option>';
    conditionFilter.innerHTML = staticOption;

    const uniqueConditions = [...new Set(items.map((offer) => (offer.condition || '').trim()).filter(Boolean))];
    uniqueConditions.sort((a, b) => a.localeCompare(b));

    uniqueConditions.forEach((condition) => {
      const option = document.createElement('option');
      option.value = condition;
      option.textContent = condition;
      conditionFilter.appendChild(option);
    });
  };

  const matchesFilters = (offer) => {
    const query = searchInput.value.trim().toLowerCase();
    const selectedCondition = conditionFilter.value;

    if (selectedCondition && offer.condition !== selectedCondition) {
      return false;
    }

    if (!query) {
      return true;
    }

    const haystack = [offer.bookTitle, offer.author, offer.ownerName, offer.note, offer.condition]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    return haystack.includes(query);
  };

  const toDateText = (value) => {
    if (!value) {
      return 'Unknown time';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString();
  };

  const statusClass = (status) => {
    const normalized = (status || '').toUpperCase();
    if (normalized === 'ACCEPTED') {
      return 'status-accepted';
    }
    if (normalized === 'REJECTED') {
      return 'status-rejected';
    }
    return 'status-pending';
  };

  const myOfferOptionsHtml = () => {
    if (myActiveOffers.length === 0) {
      return '<option value="">No active offers available</option>';
    }

    return [
      '<option value="">Choose your offer to exchange</option>',
      ...myActiveOffers.map((offer) => {
        const label = `${offer.bookTitle || 'Untitled'} (${offer.condition || 'Unknown'})`;
        return `<option value="${escapeHtml(offer.offerId)}">${escapeHtml(label)}</option>`;
      })
    ].join('');
  };

  const renderOffers = () => {
    const filtered = offers.filter(matchesFilters);

    offerStats.textContent = `${filtered.length} active offer${filtered.length === 1 ? '' : 's'} shown`;

    if (filtered.length === 0) {
      showMessage('empty', 'No active offers match your current filters.');
      return;
    }

    const optionMarkup = myOfferOptionsHtml();
    const disableActions = myActiveOffers.length === 0;

    offersContainer.className = 'offers-grid';
    offersContainer.innerHTML = filtered
      .map((offer) => {
        const firstImage = Array.isArray(offer.imageUrls) && offer.imageUrls.length > 0 ? offer.imageUrls[0] : null;
        const galleryHtml = firstImage
          ? `<img src="${escapeHtml(firstImage)}" alt="${escapeHtml(offer.bookTitle || 'Book image')}" loading="lazy">`
          : '<div class="gallery-empty">No Photo</div>';

        const note = offer.note && offer.note.trim() ? offer.note : 'No note provided.';
        const hasCurrentLocation = Number.isFinite(Number(offer.currentUserLatitude)) && Number.isFinite(Number(offer.currentUserLongitude));
        const hasOwnerLocation = Number.isFinite(Number(offer.ownerLatitude)) && Number.isFinite(Number(offer.ownerLongitude));
        const canOpenMap = hasCurrentLocation && hasOwnerLocation;
        const mapHint = canOpenMap
          ? 'See route, distance, and delivery cost.'
          : 'Location not available for this user';

        return `
          <article class="offer-card">
            <div class="offer-gallery">${galleryHtml}</div>
            <div class="offer-body">
              <h3 class="offer-title">${escapeHtml(offer.bookTitle || 'Untitled')}</h3>
              <p class="offer-author">by ${escapeHtml(offer.author || 'Unknown author')}</p>
              <span class="badge-condition">${escapeHtml(offer.condition || 'Unknown')}</span>
              <p class="offer-note">${escapeHtml(note)}</p>
              <div class="offer-meta">
                <span>Owner: ${escapeHtml(offer.ownerName || 'Unknown')}</span>
                <span>${Array.isArray(offer.imageUrls) ? offer.imageUrls.length : 0} image(s)</span>
              </div>
              <select class="exchange-select" data-target-offer-id="${escapeHtml(offer.offerId)}" ${disableActions ? 'disabled' : ''}>
                ${optionMarkup}
              </select>
              <button class="exchange-btn" data-target-offer-id="${escapeHtml(offer.offerId)}" ${disableActions ? 'disabled' : ''}>
                Send Exchange Request
              </button>
              <button
                class="map-route-btn"
                data-action="show-map"
                data-owner-name="${escapeHtml(offer.ownerName || 'Offer Owner')}"
                data-owner-address="${escapeHtml(offer.ownerAddress || '')}"
                data-owner-lat="${escapeHtml(offer.ownerLatitude)}"
                data-owner-lng="${escapeHtml(offer.ownerLongitude)}"
                data-current-name="${escapeHtml(offer.currentUserName || 'You')}"
                data-current-address="${escapeHtml(offer.currentUserAddress || '')}"
                data-current-lat="${escapeHtml(offer.currentUserLatitude)}"
                data-current-lng="${escapeHtml(offer.currentUserLongitude)}"
                ${canOpenMap ? '' : 'disabled'}>
                Show Map & Cost
              </button>
              <div class="exchange-note">
                ${disableActions ? 'Create an active offer first to request exchanges.' : 'Choose one of your active offers and request this book.'}
              </div>
              <div class="exchange-note">${escapeHtml(mapHint)}</div>
            </div>
          </article>
        `;
      })
      .join('');
  };

  const renderRequests = (sentRequests, receivedRequests) => {
    const renderList = (container, items, includeActions) => {
      if (!Array.isArray(items) || items.length === 0) {
        container.innerHTML = '<div class="subtle-empty">No requests yet.</div>';
        return;
      }

      container.innerHTML = items
        .map((request) => {
          const pending = (request.status || '').toUpperCase() === 'PENDING';
          const actions = includeActions && pending
            ? `
              <div class="request-actions">
                <button class="request-action-btn accept-btn" data-action="accept" data-id="${escapeHtml(request.exchangeRequestId)}">Accept</button>
                <button class="request-action-btn reject-btn" data-action="reject" data-id="${escapeHtml(request.exchangeRequestId)}">Reject</button>
              </div>
            `
            : '';

          return `
            <article class="request-item">
              <p class="request-item-title">${escapeHtml(request.requesterBookTitle || 'Unknown')} -> ${escapeHtml(request.targetBookTitle || 'Unknown')}</p>
              <p class="request-item-meta">
                Requester: ${escapeHtml(request.requesterUser || 'Unknown')}<br>
                Target Owner: ${escapeHtml(request.targetUser || 'Unknown')}<br>
                Created: ${escapeHtml(toDateText(request.createdAt))}
              </p>
              <span class="request-status ${statusClass(request.status)}">${escapeHtml(request.status || 'PENDING')}</span>
              ${actions}
            </article>
          `;
        })
        .join('');
    };

    renderList(sentRequestsList, sentRequests, false);
    renderList(receivedRequestsList, receivedRequests, true);
  };

  const loadMyActiveOffers = async () => {
    const response = await fetch('/offers/my-active', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (!response.ok) {
      myActiveOffers = [];
      return;
    }

    const payload = await response.json();
    myActiveOffers = Array.isArray(payload) ? payload : [];
  };

  const loadOffers = async () => {
    showMessage('loading', 'Loading active exchange offers...');
    offerStats.textContent = 'Fetching offers...';

    const response = await fetch('/offers', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }

    const data = await response.json();
    const allActiveOffers = Array.isArray(data) ? data : [];
    const myOfferIds = new Set(myActiveOffers.map((offer) => Number(offer.offerId)));
    offers = allActiveOffers.filter((offer) => !myOfferIds.has(Number(offer.offerId)));
    setConditionOptions(offers);
    renderOffers();
  };

  const loadRequests = async () => {
    const response = await fetch('/exchange-requests/my-requests', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (response.status === 401) {
      renderRequests([], []);
      showBanner('info', 'Log in to view and manage your exchange requests.');
      return;
    }

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }

    const payload = await response.json();
    renderRequests(payload.sentRequests || [], payload.receivedRequests || []);
  };

  const createExchangeRequest = async (requesterOfferId, targetOfferId) => {
    const response = await fetch('/exchange-requests', {
      method: 'POST',
      headers: authHeaders(true),
      body: JSON.stringify({ requesterOfferId, targetOfferId })
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }
  };

  const respondToRequest = async (id, action) => {
    const endpoint = action === 'accept'
      ? `/exchange-requests/${id}/accept`
      : `/exchange-requests/${id}/reject`;

    const response = await fetch(endpoint, {
      method: 'PUT',
      headers: authHeaders(false)
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }
  };

  const refreshAll = async () => {
    clearBanner();
    try {
      await loadMyActiveOffers();
      await loadOffers();
      await loadRequests();
    } catch (error) {
      offerStats.textContent = 'Unable to load offers';
      showMessage('error', 'Could not load exchange data right now. Please try again in a moment.');
      showBanner('error', error.message || 'Something went wrong while loading exchange data.');
    }
  };

  searchInput.addEventListener('input', renderOffers);
  conditionFilter.addEventListener('change', renderOffers);

  refreshRequestsBtn.addEventListener('click', async () => {
    clearBanner();
    try {
      await loadRequests();
      showBanner('success', 'Requests refreshed.');
    } catch (error) {
      showBanner('error', error.message || 'Failed to refresh requests.');
    }
  });

  offersContainer.addEventListener('click', async (event) => {
    const mapButton = event.target.closest('.map-route-btn');
    if (mapButton) {
      const currentLat = Number(mapButton.getAttribute('data-current-lat'));
      const currentLng = Number(mapButton.getAttribute('data-current-lng'));
      const ownerLat = Number(mapButton.getAttribute('data-owner-lat'));
      const ownerLng = Number(mapButton.getAttribute('data-owner-lng'));

      if (!Number.isFinite(currentLat) || !Number.isFinite(currentLng) || !Number.isFinite(ownerLat) || !Number.isFinite(ownerLng)) {
        showBanner('error', 'Location not available for this user');
        return;
      }

      if (!window.openRouteMap) {
        showBanner('error', 'Map service is unavailable right now.');
        return;
      }

      try {
        await window.openRouteMap(
          currentLat,
          currentLng,
          ownerLat,
          ownerLng,
          mapButton.getAttribute('data-owner-address') || null,
          {
            modalId: 'routeMapModal',
            mapId: 'routeMapCanvas',
            titleId: 'routeMapTitle',
            routeStatusId: 'routeStatus',
            distanceValueId: 'distanceValue',
            costValueId: 'costValue',
            senderAddressId: 'senderAddress',
            receiverAddressId: 'receiverAddress',
            currentUserName: mapButton.getAttribute('data-current-name') || 'You',
            currentUserAddress: mapButton.getAttribute('data-current-address') || null,
            otherUserName: mapButton.getAttribute('data-owner-name') || 'Offer Owner',
            title: 'Exchange Route Map & Cost'
          }
        );
      } catch (error) {
        showBanner('error', error.message || 'Failed to open map.');
      }
      return;
    }

    const button = event.target.closest('.exchange-btn');
    if (!button) {
      return;
    }

    const targetOfferId = Number(button.getAttribute('data-target-offer-id'));
    const select = offersContainer.querySelector(`.exchange-select[data-target-offer-id="${targetOfferId}"]`);
    const requesterOfferId = select ? Number(select.value) : NaN;

    if (!Number.isFinite(requesterOfferId) || requesterOfferId <= 0) {
      showBanner('error', 'Select one of your active offers first.');
      return;
    }

    if (requesterOfferId === targetOfferId) {
      showBanner('error', 'You cannot request exchange with the same offer.');
      return;
    }

    button.disabled = true;
    clearBanner();
    try {
      await createExchangeRequest(requesterOfferId, targetOfferId);
      showBanner('success', 'Exchange request sent successfully.');
      await loadRequests();
    } catch (error) {
      showBanner('error', error.message || 'Failed to send exchange request.');
    } finally {
      button.disabled = false;
    }
  });

  receivedRequestsList.addEventListener('click', async (event) => {
    const actionButton = event.target.closest('.request-action-btn');
    if (!actionButton) {
      return;
    }

    const action = actionButton.getAttribute('data-action');
    const id = Number(actionButton.getAttribute('data-id'));
    if (!id || !action) {
      return;
    }

    actionButton.disabled = true;
    clearBanner();

    try {
      await respondToRequest(id, action);
      showBanner('success', `Request ${action}ed successfully.`);
      await loadMyActiveOffers();
      await loadOffers();
      await loadRequests();
    } catch (error) {
      showBanner('error', error.message || `Failed to ${action} request.`);
    } finally {
      actionButton.disabled = false;
    }
  });

  refreshAll();
});
