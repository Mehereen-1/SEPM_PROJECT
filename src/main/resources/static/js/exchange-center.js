document.addEventListener('DOMContentLoaded', () => {
  const myOfferSelect = document.getElementById('myOfferSelect');
  const targetOfferSelect = document.getElementById('targetOfferSelect');
  const targetSearch = document.getElementById('targetSearch');
  const sendRequestBtn = document.getElementById('sendRequestBtn');
  const refreshBtn = document.getElementById('refreshBtn');
  const statusBanner = document.getElementById('statusBanner');

  const availableOffersList = document.getElementById('availableOffersList');
  const sentRequestsList = document.getElementById('sentRequestsList');
  const receivedRequestsList = document.getElementById('receivedRequestsList');

  const statActiveOffers = document.getElementById('statActiveOffers');
  const statSent = document.getElementById('statSent');
  const statReceived = document.getElementById('statReceived');

  let myOffers = [];
  let allOffers = [];
  let requests = { sentRequests: [], receivedRequests: [] };

  const escapeHtml = (value) => {
    if (value == null) {
      return '';
    }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const authHeaders = (json) => {
    const headers = { Accept: 'application/json' };
    if (json) {
      headers['Content-Type'] = 'application/json';
    }

    const token = localStorage.getItem('token');
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    return headers;
  };

  const responseMessage = async (response) => {
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
    statusBanner.className = `status-banner ${type}`;
    statusBanner.textContent = message;
  };

  const clearBanner = () => {
    statusBanner.className = 'status-banner';
    statusBanner.textContent = '';
  };

  const dateText = (value) => {
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

  const loadMyOffers = async () => {
    const response = await fetch('/offers/my-active', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (response.status === 401) {
      myOffers = [];
      return;
    }

    if (!response.ok) {
      throw new Error(await responseMessage(response));
    }

    const data = await response.json();
    myOffers = Array.isArray(data) ? data : [];
  };

  const loadAllOffers = async () => {
    const response = await fetch('/offers', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (!response.ok) {
      throw new Error(await responseMessage(response));
    }

    const data = await response.json();
    allOffers = Array.isArray(data) ? data : [];
  };

  const loadRequests = async () => {
    const response = await fetch('/exchange-requests/my-requests', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (response.status === 401) {
      requests = { sentRequests: [], receivedRequests: [] };
      return;
    }

    if (!response.ok) {
      throw new Error(await responseMessage(response));
    }

    const data = await response.json();
    requests = {
      sentRequests: Array.isArray(data.sentRequests) ? data.sentRequests : [],
      receivedRequests: Array.isArray(data.receivedRequests) ? data.receivedRequests : []
    };
  };

  const renderSelects = () => {
    if (myOffers.length === 0) {
      myOfferSelect.innerHTML = '<option value="">No active offers available</option>';
      sendRequestBtn.disabled = true;
    } else {
      myOfferSelect.innerHTML = [
        '<option value="">Choose your offer</option>',
        ...myOffers.map((offer) => {
          const label = `${offer.bookTitle || 'Untitled'} (${offer.condition || 'Unknown'})`;
          return `<option value="${escapeHtml(offer.offerId)}">${escapeHtml(label)}</option>`;
        })
      ].join('');
      sendRequestBtn.disabled = false;
    }

    const query = targetSearch.value.trim().toLowerCase();
    const filteredOffers = allOffers.filter((offer) => {
      if (!query) {
        return true;
      }
      const text = [offer.bookTitle, offer.author, offer.ownerName, offer.note]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return text.includes(query);
    });

    targetOfferSelect.innerHTML = [
      '<option value="">Choose target offer</option>',
      ...filteredOffers.map((offer) => {
        const label = `${offer.bookTitle || 'Untitled'} by ${offer.ownerName || 'Unknown'}`;
        return `<option value="${escapeHtml(offer.offerId)}">${escapeHtml(label)}</option>`;
      })
    ].join('');
  };

  const renderAvailableOffers = () => {
    const query = targetSearch.value.trim().toLowerCase();
    const filteredOffers = allOffers.filter((offer) => {
      if (!query) {
        return true;
      }
      const text = [offer.bookTitle, offer.author, offer.ownerName, offer.note]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return text.includes(query);
    });

    statActiveOffers.textContent = `Active offers: ${filteredOffers.length}`;

    if (filteredOffers.length === 0) {
      availableOffersList.innerHTML = '<div class="empty">No offers match your search.</div>';
      return;
    }

    availableOffersList.innerHTML = filteredOffers
      .map((offer) => {
        const note = offer.note && offer.note.trim() ? offer.note : 'No note provided.';
        return `
          <article class="item-card">
            <p class="item-title">${escapeHtml(offer.bookTitle || 'Untitled')}</p>
            <p class="item-meta">
              Author: ${escapeHtml(offer.author || 'Unknown')}<br>
              Owner: ${escapeHtml(offer.ownerName || 'Unknown')}<br>
              Condition: ${escapeHtml(offer.condition || 'Unknown')}<br>
              Note: ${escapeHtml(note)}
            </p>
            <button class="ghost-btn" data-use-target="${escapeHtml(offer.offerId)}">Use As Target</button>
          </article>
        `;
      })
      .join('');
  };

  const renderRequests = () => {
    statSent.textContent = `Sent requests: ${requests.sentRequests.length}`;
    statReceived.textContent = `Received requests: ${requests.receivedRequests.length}`;

    const renderList = (container, items, withActions) => {
      if (!Array.isArray(items) || items.length === 0) {
        container.innerHTML = '<div class="empty">No requests yet.</div>';
        return;
      }

      container.innerHTML = items
        .map((request) => {
          const pending = (request.status || '').toUpperCase() === 'PENDING';
          const actions = withActions && pending
            ? `
              <div class="request-actions">
                <button class="request-btn accept-btn" data-action="accept" data-id="${escapeHtml(request.exchangeRequestId)}">Accept</button>
                <button class="request-btn reject-btn" data-action="reject" data-id="${escapeHtml(request.exchangeRequestId)}">Reject</button>
              </div>
            `
            : '';

          return `
            <article class="item-card">
              <p class="item-title">${escapeHtml(request.requesterBookTitle || 'Unknown')} -> ${escapeHtml(request.targetBookTitle || 'Unknown')}</p>
              <p class="item-meta">
                Requester: ${escapeHtml(request.requesterUser || 'Unknown')}<br>
                Target Owner: ${escapeHtml(request.targetUser || 'Unknown')}<br>
                Created: ${escapeHtml(dateText(request.createdAt))}
              </p>
              <span class="request-status ${statusClass(request.status)}">${escapeHtml(request.status || 'PENDING')}</span>
              ${actions}
            </article>
          `;
        })
        .join('');
    };

    renderList(sentRequestsList, requests.sentRequests, false);
    renderList(receivedRequestsList, requests.receivedRequests, true);
  };

  const createRequest = async (requesterOfferId, targetOfferId) => {
    const response = await fetch('/exchange-requests', {
      method: 'POST',
      headers: authHeaders(true),
      body: JSON.stringify({ requesterOfferId, targetOfferId })
    });

    if (!response.ok) {
      throw new Error(await responseMessage(response));
    }
  };

  const respondRequest = async (id, action) => {
    const endpoint = action === 'accept'
      ? `/exchange-requests/${id}/accept`
      : `/exchange-requests/${id}/reject`;

    const response = await fetch(endpoint, {
      method: 'PUT',
      headers: authHeaders(false)
    });

    if (!response.ok) {
      throw new Error(await responseMessage(response));
    }
  };

  const refreshAll = async () => {
    clearBanner();
    try {
      await loadMyOffers();
      await loadAllOffers();
      await loadRequests();
      renderSelects();
      renderAvailableOffers();
      renderRequests();

      if (myOffers.length === 0) {
        showBanner('info', 'Create at least one active offer to send exchange requests.');
      }
    } catch (error) {
      showBanner('error', error.message || 'Failed to load exchange center data.');
    }
  };

  sendRequestBtn.addEventListener('click', async () => {
    const requesterOfferId = Number(myOfferSelect.value);
    const targetOfferId = Number(targetOfferSelect.value);

    if (!requesterOfferId || !targetOfferId) {
      showBanner('error', 'Select both your offer and a target offer.');
      return;
    }

    if (requesterOfferId === targetOfferId) {
      showBanner('error', 'Requester and target offers must be different.');
      return;
    }

    sendRequestBtn.disabled = true;
    clearBanner();
    try {
      await createRequest(requesterOfferId, targetOfferId);
      showBanner('success', 'Exchange request sent successfully.');
      await loadRequests();
      renderRequests();
    } catch (error) {
      showBanner('error', error.message || 'Failed to send exchange request.');
    } finally {
      sendRequestBtn.disabled = false;
    }
  });

  targetSearch.addEventListener('input', () => {
    renderSelects();
    renderAvailableOffers();
  });

  refreshBtn.addEventListener('click', refreshAll);

  availableOffersList.addEventListener('click', (event) => {
    const button = event.target.closest('[data-use-target]');
    if (!button) {
      return;
    }

    const offerId = button.getAttribute('data-use-target');
    targetOfferSelect.value = offerId;
    showBanner('info', 'Target offer selected. Choose your offer and send request.');
  });

  receivedRequestsList.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-action][data-id]');
    if (!button) {
      return;
    }

    const action = button.getAttribute('data-action');
    const id = Number(button.getAttribute('data-id'));
    if (!id || !action) {
      return;
    }

    button.disabled = true;
    clearBanner();

    try {
      await respondRequest(id, action);
      showBanner('success', `Request ${action}ed successfully.`);
      await loadMyOffers();
      await loadAllOffers();
      await loadRequests();
      renderSelects();
      renderAvailableOffers();
      renderRequests();
    } catch (error) {
      showBanner('error', error.message || `Failed to ${action} request.`);
    } finally {
      button.disabled = false;
    }
  });

  refreshAll();
});
